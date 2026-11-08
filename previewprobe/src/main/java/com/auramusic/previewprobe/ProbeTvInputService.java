package com.auramusic.previewprobe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

/** Instrumented, no-network TvInputService diagnostic for Projectivy compatibility. */
public final class ProbeTvInputService extends TvInputService {
    static final String TAG = "PreviewProbeTvInput";

    @Override
    public Session onCreateSession(String inputId) {
        Log.i(TAG, "session_created inputId=" + inputId + " elapsedNs=" + SystemClock.elapsedRealtimeNanos());
        return new ProbeSession(this);
    }

    private static final class ProbeSession extends Session {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Surface surface;
        private boolean tuned;
        private long frameNumber;
        private AudioTrack audioTrack;
        private Thread toneThread;
        private volatile boolean toneRunning;

        ProbeSession(Context context) {
            super(context);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }

        @Override
        public boolean onTune(Uri channelUri) {
            tuned = true;
            Log.i(TAG, "onTune uri=" + channelUri + " elapsedNs=" + SystemClock.elapsedRealtimeNanos());
            notifyVideoAvailable();
            startTone();
            drawFrame();
            return true;
        }

        @Override
        public boolean onSetSurface(Surface nextSurface) {
            Log.i(TAG, "onSetSurface valid=" + (nextSurface != null && nextSurface.isValid()) +
                    " elapsedNs=" + SystemClock.elapsedRealtimeNanos());
            surface = nextSurface;
            drawFrame();
            return true;
        }

        @Override
        public void onSurfaceChanged(int format, int width, int height) {
            Log.i(TAG, "onSurfaceChanged width=" + width + " height=" + height +
                    " format=" + format + " elapsedNs=" + SystemClock.elapsedRealtimeNanos());
            drawFrame();
        }

        @Override
        public void onSetStreamVolume(float volume) {
            Log.i(TAG, "onSetStreamVolume volume=" + volume +
                    " elapsedNs=" + SystemClock.elapsedRealtimeNanos());
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            Log.i(TAG, "onSetCaptionEnabled enabled=" + enabled +
                    " elapsedNs=" + SystemClock.elapsedRealtimeNanos());
        }

        @Override
        public void onRelease() {
            Log.i(TAG, "onRelease frames=" + frameNumber + " elapsedNs=" + SystemClock.elapsedRealtimeNanos());
            tuned = false;
            surface = null;
            handler.removeCallbacks(frameLoop);
            stopTone();
        }

        private final Runnable frameLoop = new Runnable() {
            @Override public void run() {
                if (!tuned || surface == null || !surface.isValid()) return;
                drawFrame();
                handler.postDelayed(this, 100);
            }
        };

        private void drawFrame() {
            Surface target = surface;
            if (!tuned || target == null || !target.isValid()) return;
            Canvas canvas = null;
            try {
                canvas = target.lockCanvas(null);
                if (canvas == null) return;
                int width = canvas.getWidth();
                int height = canvas.getHeight();
                long frame = frameNumber++;
                canvas.drawColor(Color.rgb(12, 28, 62));
                paint.setColor(Color.WHITE);
                paint.setTextSize(Math.max(28, height / 8f));
                canvas.drawText("TV INPUT TEST", 24, Math.max(48, height / 3f), paint);
                paint.setColor(Color.rgb(255, 205, 20));
                float markerWidth = Math.max(24, width / 12f);
                float x = (float) ((frame * Math.max(4, width / 50f)) % Math.max(1, width - markerWidth));
                float y = height * 0.68f + (float) Math.sin(frame * 0.12) * height * 0.08f;
                canvas.drawRect(x, y, x + markerWidth, y + Math.max(12, height / 24f), paint);
                if (frame % 10 == 0) {
                    Log.i(TAG, "frame timestampNs=" + SystemClock.elapsedRealtimeNanos() + " frame=" + frame);
                }
            } catch (Exception error) {
                Log.w(TAG, "surface draw failed", error);
            } finally {
                if (canvas != null) {
                    try { target.unlockCanvasAndPost(canvas); }
                    catch (Exception error) { Log.w(TAG, "surface post failed", error); }
                }
            }
            if (!handler.hasCallbacks(frameLoop)) handler.postDelayed(frameLoop, 100);
        }

        private void startTone() {
            if (toneRunning) return;
            int sampleRate = 44100;
            int bufferBytes = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferBytes <= 0) {
                Log.w(TAG, "AudioTrack unavailable bufferBytes=" + bufferBytes);
                return;
            }
            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setBufferSizeInBytes(Math.max(bufferBytes, 8192))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            audioTrack.play();
            toneRunning = true;
            toneThread = new Thread(() -> {
                short[] samples = new short[1024];
                long sample = 0;
                while (toneRunning) {
                    for (int i = 0; i < samples.length; i++, sample++) {
                        samples[i] = (short) (Math.sin(2.0 * Math.PI * 440.0 * sample / sampleRate) * 4000.0);
                    }
                    AudioTrack track = audioTrack;
                    if (track == null) break;
                    int written = track.write(samples, 0, samples.length);
                    if (written < 0) {
                        Log.w(TAG, "AudioTrack write failed code=" + written);
                        break;
                    }
                }
            }, "PreviewProbe-Tone");
            toneThread.start();
            Log.i(TAG, "deterministic_tone_started hz=440 sampleRate=" + sampleRate);
        }

        private void stopTone() {
            toneRunning = false;
            Thread thread = toneThread;
            toneThread = null;
            if (thread != null) {
                thread.interrupt();
                try { thread.join(500); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            }
            AudioTrack track = audioTrack;
            audioTrack = null;
            if (track != null) {
                try { track.pause(); track.flush(); } catch (Exception ignored) { }
                track.release();
            }
        }
    }
}
