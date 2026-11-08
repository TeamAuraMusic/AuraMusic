package com.auramusic.previewprobe;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Binder;
import android.util.Log;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Serves the deterministic A/B files using seekable descriptors to third-party launchers. */
public final class ProbeMediaProvider extends ContentProvider {
    private static final String TAG = "PreviewProbeMedia";
    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) {
        String path = uri.getLastPathSegment();
        if (path != null && path.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (path != null && path.endsWith(".ts")) return "video/mp2t";
        if (path != null && path.endsWith(".mp3")) return "audio/mpeg";
        if (path != null && path.endsWith(".gif")) return "image/gif";
        if ("fixture_e_audio.mp4".equals(path)) return "audio/mp4";
        return "e".equals(path) ? "audio/mp4" : "video/mp4";
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        String[] columns = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        File file;
        try {
            file = fixture(uri);
        } catch (FileNotFoundException missing) {
            return null;
        }
        MatrixCursor result = new MatrixCursor(columns);
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i] = file.getName();
            else if (OpenableColumns.SIZE.equals(columns[i])) row[i] = file.length();
        }
        result.addRow(row);
        return result;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read-only fixture provider");
        File file = fixture(uri);
        Log.i(TAG, "open uri=" + uri + " file=" + file.getName() + " bytes=" + file.length()
                + " callerUid=" + Binder.getCallingUid());
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private File fixture(Uri uri) throws FileNotFoundException {
        if (uri.getPathSegments().size() == 2 && "hls".equals(uri.getPathSegments().get(0))) {
            String name = uri.getPathSegments().get(1);
            if (!"master.m3u8".equals(name) && !"video.m3u8".equals(name)
                    && !"audio.m3u8".equals(name) && !"fixture_f_video.ts".equals(name)
                    && !"fixture_f_audio.ts".equals(name) && !"gmaster.m3u8".equals(name)
                    && !"gvideo.m3u8".equals(name) && !"gaudio.m3u8".equals(name)
                    && !"hmaster.m3u8".equals(name) && !"haudio.m3u8".equals(name)
                    && !"fixture_h_audio.mp3".equals(name)
                    && !"fixture_e_audio.mp4".equals(name)) {
                throw new FileNotFoundException("Unknown HLS fixture " + name);
            }
            File file = "fixture_e_audio.mp4".equals(name)
                    ? new File(getContext().getFilesDir(), "previewprobe/fixture_e_audio_only.mp4")
                    : new File(getContext().getFilesDir(), "previewprobe/hls/" + name);
            if (!file.isFile() || file.length() == 0) {
                throw new FileNotFoundException("HLS fixture is not prepared: " + name);
            }
            return file;
        }
        if (uri.getPathSegments().size() != 1) throw new FileNotFoundException("Invalid fixture URI");
        String id = uri.getPathSegments().get(0);
        if ("wave.gif".equals(id)) {
            File gif = new File(getContext().getFilesDir(), "previewprobe/fixture_wave.gif");
            if (!gif.isFile()) throw new FileNotFoundException("Animated art fixture is not prepared");
            return gif;
        }
        if (!"a".equals(id) && !"b".equals(id) && !"c".equals(id)
                && !"e".equals(id)) {
            throw new FileNotFoundException("Unknown fixture " + id);
        }
        String name = "c".equals(id) ? "fixture_c_wave.mp4"
                : "e".equals(id) ? "fixture_e_audio_only.mp4" : "fixture_" + id + ".mp4";
        File file = new File(getContext().getFilesDir(), "previewprobe/" + name);
        if (!file.isFile()) throw new FileNotFoundException("Fixture is not prepared: " + id);
        return file;
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
