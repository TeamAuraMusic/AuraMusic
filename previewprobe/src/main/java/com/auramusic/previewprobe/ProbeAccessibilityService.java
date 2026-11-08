package com.auramusic.previewprobe;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.View;
import android.util.Log;
import java.io.InputStream;

/** Temporary proof that an accessibility overlay can animate within Projectivy's focused card. */
public final class ProbeAccessibilityService extends AccessibilityService {
    private static final String PROJECTIVY = "com.spocky.projengmenu";
    private static final String TAG = "ProbeA11yOverlay";
    private WindowManager windowManager;
    private CardOverlay overlay;
    private WindowManager.LayoutParams overlayParams;
    private String visibleCard;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = getSystemService(WindowManager.class);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) hideOverlay("no active window");
            return;
        }
        try {
            CharSequence rootPackage = root.getPackageName();
            if (rootPackage == null || !PROJECTIVY.contentEquals(rootPackage)) {
                if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    hideOverlay("Projectivy left foreground");
                }
                return;
            }
            CardTarget target = findFocusedPreviewCard(root);
            if (target == null) hideOverlay("no target card focused");
            else showOverlay(target);
        } finally {
            root.recycle();
        }
    }

    private CardTarget findFocusedPreviewCard(AccessibilityNodeInfo node) {
        if (node.isFocused() && node.getPackageName() != null
                && PROJECTIVY.contentEquals(node.getPackageName())) {
            CharSequence label = node.getContentDescription();
            if (label != null && label.toString().startsWith("Preview A")) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                if (bounds.width() > 0 && bounds.height() > 0) return new CardTarget(label.toString(), bounds);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                CardTarget target = findFocusedPreviewCard(child);
                if (target != null) return target;
            } finally {
                child.recycle();
            }
        }
        return null;
    }

    private void showOverlay(CardTarget target) {
        if (windowManager == null) return;
        if (overlay == null) {
            overlay = new CardOverlay();
            overlayParams = new WindowManager.LayoutParams(
                    target.bounds.width(), target.bounds.height(),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            overlayParams.gravity = Gravity.TOP | Gravity.LEFT;
            overlayParams.x = target.bounds.left;
            overlayParams.y = target.bounds.top;
            try {
                windowManager.addView(overlay, overlayParams);
                Log.i(TAG, "overlay added title=" + target.label + " bounds=" + target.bounds);
            } catch (WindowManager.BadTokenException | IllegalStateException error) {
                Log.e(TAG, "overlay add failed", error);
                overlay = null;
                return;
            }
        } else {
            overlayParams.width = target.bounds.width();
            overlayParams.height = target.bounds.height();
            overlayParams.x = target.bounds.left;
            overlayParams.y = target.bounds.top;
            try {
                windowManager.updateViewLayout(overlay, overlayParams);
            } catch (IllegalArgumentException ignored) {
                hideOverlay("card window update failed");
                return;
            }
        }
        if (!target.label.equals(visibleCard)) {
            visibleCard = target.label;
            overlay.resetAnimation();
        }
        overlay.invalidate();
    }

    private void hideOverlay(String reason) {
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); }
            catch (IllegalArgumentException ignored) { }
            Log.i(TAG, "overlay removed reason=" + reason);
            overlay = null;
            overlayParams = null;
        }
        visibleCard = null;
    }

    @Override public void onInterrupt() { hideOverlay("service interrupted"); }

    @Override public void onDestroy() {
        hideOverlay("service destroyed");
        super.onDestroy();
    }

    private final class CardOverlay extends View {
        private final Movie movie;
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clipPath = new Path();
        private final RectF cardRect = new RectF();
        private long animationStartedMs = SystemClock.uptimeMillis();

        CardOverlay() {
            super(ProbeAccessibilityService.this);
            Movie loaded;
            try (InputStream stream = getResources().openRawResource(R.raw.fixture_a_wave)) {
                loaded = Movie.decodeStream(stream);
            } catch (Exception error) {
                loaded = null;
            }
            movie = loaded;
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        void resetAnimation() { animationStartedMs = SystemClock.uptimeMillis(); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;
            float radius = Math.min(24f, height * 0.12f);
            cardRect.set(0, 0, width, height);
            clipPath.reset();
            clipPath.addRoundRect(cardRect, radius, radius, Path.Direction.CW);
            int save = canvas.save();
            canvas.clipPath(clipPath);
            if (movie != null) {
                int duration = Math.max(1, movie.duration());
                movie.setTime((int) ((SystemClock.uptimeMillis() - animationStartedMs) % duration));
                float scale = Math.max(width / (float) movie.width(), height / (float) movie.height());
                float drawWidth = movie.width() * scale;
                float drawHeight = movie.height() * scale;
                movie.draw(canvas, (width - drawWidth) / 2f, (height - drawHeight) / 2f, imagePaint);
            }
            drawWave(canvas, width, height);
            canvas.restoreToCount(save);
            postInvalidateDelayed(80);
        }

        private void drawWave(Canvas canvas, int width, int height) {
            int bars = 5;
            float barWidth = Math.max(5f, width * 0.025f);
            float gap = barWidth * 0.65f;
            float total = bars * barWidth + (bars - 1) * gap;
            float left = (width - total) / 2f;
            float baseline = height * 0.77f;
            float maxHeight = height * 0.34f;
            float phase = (SystemClock.uptimeMillis() - animationStartedMs) / 220f;
            barPaint.setColor(Color.rgb(255, 48, 171));
            barPaint.setShadowLayer(10f, 0, 0, Color.rgb(255, 40, 160));
            for (int i = 0; i < bars; i++) {
                float wave = (float) (0.25 + 0.75 * Math.abs(Math.sin(phase + i * 0.82)));
                float barHeight = Math.max(height * 0.12f, maxHeight * wave);
                float x = left + i * (barWidth + gap);
                canvas.drawRoundRect(x, baseline - barHeight, x + barWidth, baseline,
                        barWidth / 2f, barWidth / 2f, barPaint);
            }
            barPaint.clearShadowLayer();
        }
    }

    private static final class CardTarget {
        final String label;
        final Rect bounds;
        CardTarget(String label, Rect bounds) { this.label = label; this.bounds = bounds; }
    }
}
