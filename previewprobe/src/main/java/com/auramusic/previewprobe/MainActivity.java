package com.auramusic.previewprobe;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import android.media.tv.TvView;
import androidx.tvprovider.media.tv.PreviewChannel;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String TAG = "PreviewProbe";
    private static final String CHANNEL = "previewprobe:wave-video-v1";
    private TvView tvInputDiagnosticView;

    private Uri previewUri(String id) {
        String override = getIntent().getStringExtra("preview_uri_" + id.toLowerCase());
        if (override != null && !override.trim().isEmpty()) return Uri.parse(override.trim());
        if ("f".equalsIgnoreCase(id)) {
            return Uri.parse("content://" + getPackageName() + ".previewmedia/hls/master.m3u8");
        }
        if ("g".equalsIgnoreCase(id)) {
            return Uri.parse("content://" + getPackageName() + ".previewmedia/hls/gmaster.m3u8");
        }
        if ("h".equalsIgnoreCase(id)) {
            return Uri.parse("content://" + getPackageName() + ".previewmedia/hls/hmaster.m3u8");
        }
        String base = getIntent().getStringExtra("preview_https_base");
        if (!"c".equalsIgnoreCase(id) && base != null && !base.trim().isEmpty()) {
            if (!base.endsWith("/")) base += "/";
            return Uri.parse(base + "fixture_" + id.toLowerCase() + ".mp4");
        }
        return Uri.parse("content://" + getPackageName() + ".previewmedia/" + id.toLowerCase());
    }

    private Uri publishMediaStoreFixture(String id, File source) {
        String displayName = "previewprobe-" + id.toLowerCase() + ".mp4";
        Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri mediaUri = null;
        try (android.database.Cursor cursor = getContentResolver().query(
                collection,
                new String[]{MediaStore.Video.Media._ID},
                MediaStore.Video.Media.DISPLAY_NAME + "=? AND " + MediaStore.Video.Media.RELATIVE_PATH + "=?",
                new String[]{displayName, "Movies/PreviewProbe/"},
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                mediaUri = Uri.withAppendedPath(collection,
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)) + "");
            }
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PreviewProbe/");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        if (mediaUri == null) mediaUri = getContentResolver().insert(collection, values);
        else getContentResolver().update(mediaUri, values, null, null);
        if (mediaUri == null) throw new IllegalStateException("MediaStore rejected fixture " + id);
        try (java.io.InputStream input = new java.io.FileInputStream(source);
             OutputStream output = getContentResolver().openOutputStream(mediaUri, "wt")) {
            if (output == null) throw new IllegalStateException("MediaStore fixture stream is unavailable");
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Could not publish MediaStore fixture " + id, error);
        }
        ContentValues ready = new ContentValues();
        ready.put(MediaStore.Video.Media.IS_PENDING, 0);
        getContentResolver().update(mediaUri, ready, null, null);
        Log.i(TAG, "MediaStore fixture id=" + id + " uri=" + mediaUri + " bytes=" + source.length());
        return mediaUri;
    }

    private String programIdPrefix() {
        String base = getIntent().getStringExtra("preview_https_base");
        return base != null && !base.trim().isEmpty()
                ? "previewprobe:https:wave-video:" : "previewprobe:wave-video:";
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 36, 48, 36);
        TextView label = new TextView(this);
        label.setText("Projectivy PreviewProbe — publishes deterministic A/B/C programs");
        root.addView(label);
        Button publish = new Button(this);
        publish.setText("Publish / refresh test channel");
        publish.setOnClickListener(v -> {
            try {
                long channelId = ensureChannel();
                List<String> order = requestedProgramOrder();
                for (int position = 0; position < order.size(); position++) {
                    publishFixture(channelId, order.get(position), order.size() - position);
                }
                prunePrograms(channelId, new HashSet<>(order));
                TvContractCompat.requestChannelBrowsable(this, channelId);
                logProviderState(channelId);
                label.setText("Published channel " + channelId + " programs " + order
                        + ". Check Projectivy home.");
            } catch (Throwable error) {
                Log.e(TAG, "publish failed", error);
                label.setText("Publish failed: " + error);
            }
        });
        root.addView(publish);
        Button tuneInput = new Button(this);
        tuneInput.setText("Tune TvInputService surface diagnostic");
        tuneInput.setOnClickListener(v -> tuneTvInputService(root, label));
        root.addView(tuneInput);
        setContentView(root);
        if (getIntent().getBooleanExtra("previewprobe_publish", false)) {
            publish.post(publish::performClick);
        }
    }

    /** Intent extras make channel mutations reproducible through adb shell am start.
     * Omit previewprobe_programs for the default A-H set; pass an empty value to clear it.
     */
    private List<String> requestedProgramOrder() {
        return PreviewProbeProgramSet.parse(getIntent().getStringExtra("previewprobe_programs"));
    }

    private void publishFixture(long channelId, String id, int weight) {
        switch (id) {
            case "A": publishProgram(channelId, id, 440, 0xffff2020, R.raw.fixture_a, weight); break;
            case "B": publishProgram(channelId, id, 880, 0xff2040ff, R.raw.fixture_b, weight); break;
            case "C": publishProgram(channelId, id, 0, 0xff202020, R.raw.fixture_c, weight); break;
            case "D": publishProgram(channelId, id, 880, 0xffff2020, R.raw.fixture_a, weight); break;
            case "E": publishProgram(channelId, id, 880, 0xff2040ff, R.raw.fixture_e_audio, weight); break;
            case "F": case "G": case "H":
                publishProgram(channelId, id, 880, 0xff2040ff, R.raw.fixture_a, weight); break;
            default: throw new IllegalArgumentException("Unknown PreviewProbe fixture " + id);
        }
    }

    private void tuneTvInputService(LinearLayout root, TextView label) {
        TvInputManager manager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        TvInputInfo input = null;
        if (manager != null) {
            for (TvInputInfo candidate : manager.getTvInputList()) {
                if (ProbeTvInputService.class.getName().equals(candidate.getServiceInfo().name)) {
                    input = candidate;
                    break;
                }
            }
        }
        if (input == null) {
            label.setText("TvInputService is not registered by the system.");
            Log.e(TAG, "TvInput diagnostic input not found");
            return;
        }
        if (tvInputDiagnosticView == null) {
            tvInputDiagnosticView = new TvView(this);
            tvInputDiagnosticView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 360));
            tvInputDiagnosticView.setCallback(new TvView.TvInputCallback() {
                @Override public void onVideoAvailable(String inputId) {
                    Log.i(TAG, "TvView video available inputId=" + inputId);
                }
                @Override public void onConnectionFailed(String inputId) {
                    Log.e(TAG, "TvView connection failed inputId=" + inputId);
                }
            });
            root.addView(tvInputDiagnosticView);
        }
        long channelId = ensureChannel();
        Uri channel = TvContractCompat.buildChannelUri(channelId);
        Log.i(TAG, "TvView tune inputId=" + input.getId() + " channel=" + channel);
        tvInputDiagnosticView.tune(input.getId(), channel);
        label.setText("TvInputService tuned. Observe service callbacks and animated surface.");
    }

    @Override protected void onDestroy() {
        if (tvInputDiagnosticView != null) tvInputDiagnosticView.reset();
        super.onDestroy();
    }

    private void logProviderState(long channelId) {
        try (android.database.Cursor c = getContentResolver().query(
                TvContractCompat.buildChannelUri(channelId), null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int browsable = c.getColumnIndex(TvContractCompat.Channels.COLUMN_BROWSABLE);
                int name = c.getColumnIndex(TvContractCompat.Channels.COLUMN_DISPLAY_NAME);
                Log.i(TAG, "channel id=" + channelId + " name=" + c.getString(name) +
                        " browsable=" + c.getInt(browsable));
            }
        }
        try (android.database.Cursor c = getContentResolver().query(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId), null, null, null, null)) {
            Log.i(TAG, "provider programs channel=" + channelId + " count=" + (c == null ? -1 : c.getCount()));
            if (c != null) while (c.moveToNext()) {
                int title = c.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_TITLE);
                int uri = c.getColumnIndex(TvContractCompat.PreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI);
                int audio = c.getColumnIndex(TvContractCompat.PreviewProgramColumns.COLUMN_PREVIEW_AUDIO_URI);
                Log.i(TAG, "provider program title=" + c.getString(title) + " uri=" + c.getString(uri)
                        + " audioUri=" + c.getString(audio));
            }
        }
    }

    private long ensureChannel() {
        try (android.database.Cursor c = getContentResolver().query(
                TvContractCompat.Channels.CONTENT_URI,
                new String[]{TvContractCompat.Channels._ID, TvContractCompat.Channels.COLUMN_PACKAGE_NAME,
                        TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID}, null, null, null)) {
            if (c != null) while (c.moveToNext()) {
                if (getPackageName().equals(c.getString(1)) && CHANNEL.equals(c.getString(2))) return c.getLong(0);
            }
        }
        PreviewChannel channel = new PreviewChannel.Builder().setDisplayName("PreviewProbe A/B")
                .setDescription("Deterministic Projectivy card preview test")
                .setInternalProviderId(CHANNEL).setAppLinkIntent(new Intent(this, MainActivity.class)).build();
        Uri inserted = getContentResolver().insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues());
        if (inserted == null) throw new IllegalStateException("TvProvider rejected channel insert");
        return Long.parseLong(inserted.getLastPathSegment());
    }

    private void publishProgram(long channelId, String id, int hz, int color, int videoResource, int weight) {
        if ("F".equals(id) || "G".equals(id) || "H".equals(id)) prepareHlsFixture();
        String mediaName = "C".equals(id) ? "fixture_c_wave.mp4"
                : "D".equals(id) ? "fixture_a.mp4"
                : "E".equals(id) ? "fixture_e_audio_only.mp4"
                : "F".equals(id) || "G".equals(id) || "H".equals(id) ? "fixture_a.mp4" : "fixture_" + id.toLowerCase() + ".mp4";
        File media = new File(getFilesDir(), "previewprobe/" + mediaName);
        if (!media.isFile()) {
            File directory = media.getParentFile();
            if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
                throw new IllegalStateException("Could not create fixture directory");
            }
            File temporary = new File(directory, media.getName() + ".partial");
            try (java.io.InputStream input = getResources().openRawResource(videoResource);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[32 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Could not prepare fixture " + id, error);
            }
            if (!temporary.renameTo(media)) throw new IllegalStateException("Could not publish fixture " + id);
        }
        if ("C".equals(id)) {
            File gif = new File(media.getParentFile(), "fixture_wave.gif");
            if (!gif.isFile()) {
                try (java.io.InputStream input = getResources().openRawResource(R.raw.fixture_wave);
                     FileOutputStream output = new FileOutputStream(gif)) {
                    byte[] buffer = new byte[16 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                } catch (java.io.IOException error) {
                    throw new IllegalStateException("Could not prepare animated artwork fixture", error);
                }
            }
        }
        Uri video = getIntent().getBooleanExtra("preview_media_store", false)
                && !"C".equals(id) && !"E".equals(id)
                ? publishMediaStoreFixture(id, media) : previewUri(id);
        Intent click = new Intent(Intent.ACTION_VIEW).setClass(this, MainActivity.class)
                .putExtra("program", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String title = "C".equals(id) ? "Preview C — Animated video waveform test"
                : "F".equals(id) ? "Preview F — HLS TS audio + video"
                : "G".equals(id) ? "Preview G — HLS MP4 audio + TS video" : "Preview " + id;
        if ("H".equals(id)) title = "Preview H — HLS raw MP3 + TS video";
        Uri artwork = "C".equals(id) || "E".equals(id)
                ? Uri.parse("content://" + getPackageName() + ".previewmedia/wave.gif") : video;
        if ("F".equals(id) || "G".equals(id) || "H".equals(id)) artwork = previewUri("A");
        PreviewProgram.Builder builder = new PreviewProgram.Builder().setChannelId(channelId)
                .setType("C".equals(id) || "E".equals(id)
                        ? TvContractCompat.PreviewProgramColumns.TYPE_TRACK
                        : TvContractCompat.PreviewProgramColumns.TYPE_MOVIE).setTitle(title)
                .setDescription("C".equals(id) ? "Full artwork with a moving waveform video" :
                        "E".equals(id) ? "Audio-only video URI with animated GIF artwork" :
                        hz + " Hz / color " + Integer.toHexString(color))
                .setInternalProviderId(programIdPrefix() + id).setContentId(programIdPrefix() + id)
                .setIntent(click).setPosterArtUri(artwork).setThumbnailUri(artwork)
                .setDurationMillis("E".equals(id) || "F".equals(id) || "G".equals(id) || "H".equals(id) ? 30000 : 120000);
        builder.setPreviewVideoUri("D".equals(id) ? previewUri("A") : video);
        if ("D".equals(id)) builder.setPreviewAudioUri(previewUri("B"));
        ContentValues values = builder.build().toContentValues();
        // COLUMN_WEIGHT is present in the platform TvContract and provider schema;
        // tvprovider:1.0.0 does not expose it as a compat constant.
        values.put("weight", weight);
        Uri existing = null;
        try (android.database.Cursor c = getContentResolver().query(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                new String[]{TvContractCompat.PreviewPrograms._ID, TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID},
                null, null, null)) {
            if (c != null) while (c.moveToNext()) if (values.getAsString(TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID).equals(c.getString(1))) {
                existing = TvContractCompat.buildPreviewProgramUri(c.getLong(0)); break;
            }
        }
        if (existing == null) getContentResolver().insert(TvContractCompat.PreviewPrograms.CONTENT_URI, values);
        else getContentResolver().update(existing, values, null, null);
        Log.i(TAG, "program id=" + id + " hz=" + hz + " uri=" + video);
    }

    private void prunePrograms(long channelId, Set<String> expectedIds) {
        String prefix = programIdPrefix();
        Set<String> expected = new HashSet<>();
        for (String id : expectedIds) expected.add(prefix + id);
        try (android.database.Cursor c = getContentResolver().query(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                new String[]{TvContractCompat.PreviewPrograms._ID,
                        TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID},
                null, null, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                String id = c.getString(1);
                if (!expected.contains(id)) {
                    int removed = getContentResolver().delete(
                            TvContractCompat.buildPreviewProgramUri(c.getLong(0)), null, null);
                    Log.i(TAG, "removed stale program id=" + id + " deleted=" + removed);
                }
            }
        }
    }

    private void prepareHlsFixture() {
        File directory = new File(getFilesDir(), "previewprobe/hls");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create HLS fixture directory");
        }
        copyRawIfMissing(R.raw.fixture_f_video, new File(directory, "fixture_f_video.ts"));
        copyRawIfMissing(R.raw.fixture_f_audio, new File(directory, "fixture_f_audio.ts"));
        copyRawIfMissing(R.raw.fixture_h_audio, new File(directory, "fixture_h_audio.mp3"));
        writeText(new File(directory, "master.m3u8"),
                "#EXTM3U\n#EXT-X-VERSION:3\n" +
                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"program-audio\",NAME=\"880 Hz fixture\",DEFAULT=YES,AUTOSELECT=YES,URI=\"audio.m3u8\"\n" +
                "#EXT-X-STREAM-INF:BANDWIDTH=600000,CODECS=\"avc1.42c01e,mp4a.40.2\",AUDIO=\"program-audio\"\n" +
                "video.m3u8\n");
        writeText(new File(directory, "video.m3u8"),
                "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:31\n#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXTINF:30.000,\nfixture_f_video.ts\n#EXT-X-ENDLIST\n");
        writeText(new File(directory, "audio.m3u8"),
                "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:31\n#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXTINF:30.000,\nfixture_f_audio.ts\n#EXT-X-ENDLIST\n");
        writeText(new File(directory, "gmaster.m3u8"),
                "#EXTM3U\n#EXT-X-VERSION:7\n" +
                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"program-audio\",NAME=\"Direct MP4 880 Hz\",DEFAULT=YES,AUTOSELECT=YES,URI=\"gaudio.m3u8\"\n" +
                "#EXT-X-STREAM-INF:BANDWIDTH=600000,CODECS=\"avc1.42c01e,mp4a.40.2\",AUDIO=\"program-audio\"\n" +
                "gvideo.m3u8\n");
        writeText(new File(directory, "gvideo.m3u8"),
                "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:31\n#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXTINF:30.000,\nfixture_f_video.ts\n#EXT-X-ENDLIST\n");
        writeText(new File(directory, "gaudio.m3u8"),
                "#EXTM3U\n#EXT-X-VERSION:7\n#EXT-X-TARGETDURATION:31\n#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXT-X-MAP:URI=\"fixture_e_audio.mp4\"\n#EXTINF:30.000,\nfixture_e_audio.mp4\n#EXT-X-ENDLIST\n");
        writeText(new File(directory, "hmaster.m3u8"),
                "#EXTM3U\n#EXT-X-VERSION:3\n" +
                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"program-audio\",NAME=\"880 Hz MP3\",DEFAULT=YES,AUTOSELECT=YES,URI=\"haudio.m3u8\"\n" +
                "#EXT-X-STREAM-INF:BANDWIDTH=600000,CODECS=\"avc1.42c01e,mp4a.40.34\",AUDIO=\"program-audio\"\n" +
                "gvideo.m3u8\n");
        writeText(new File(directory, "haudio.m3u8"),
                "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:31\n#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXTINF:30.000,\nfixture_h_audio.mp3\n#EXT-X-ENDLIST\n");
        Log.i(TAG, "HLS fixture prepared TS and regular MP4 audio rendition alternatives");
    }

    private void copyRawIfMissing(int resource, File target) {
        if (target.isFile() && target.length() > 0) return;
        File temporary = new File(target.getParentFile(), target.getName() + ".partial");
        try (java.io.InputStream input = getResources().openRawResource(resource);
             FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Could not prepare HLS fixture " + target.getName(), error);
        }
        if (!temporary.renameTo(target)) throw new IllegalStateException("Could not publish HLS fixture " + target.getName());
    }

    private void writeText(File target, String text) {
        try (FileOutputStream output = new FileOutputStream(target)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Could not write HLS playlist " + target.getName(), error);
        }
    }
}
