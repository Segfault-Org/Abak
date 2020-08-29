package segfault.abak.common.backupformat.manifest;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import segfault.abak.common.backupformat.BackupLayout;
import segfault.abak.common.backupformat.InvalidFormatException;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * V1 Manifest plain text schema:
 * <code>
 *     (version)
 * </code>
 */
class ManifestFileV1 implements ManifestFile {
    private static final String TAG = "ManifestFileV1";

    private final int mVersion;

    ManifestFileV1(int version) {
        mVersion = version;
    }

    ManifestFileV1(@NonNull String raw) throws InvalidFormatException {
        try {
            mVersion = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Cannot parse version", e);
            throw new InvalidFormatException();
        }
    }

    @Override
    public int version() {
        return mVersion;
    }

    @Override
    public void write(@NonNull OutputStream stream) throws IOException {
        Log.d(TAG, "Writing version " + mVersion);
        final Writer writer = new OutputStreamWriter(stream);
        final BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write(String.valueOf(mVersion));
        bufferedWriter.close();
        writer.close();
    }
}
