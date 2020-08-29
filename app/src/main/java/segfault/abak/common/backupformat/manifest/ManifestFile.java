package segfault.abak.common.backupformat.manifest;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import segfault.abak.common.backupformat.BackupLayout;
import segfault.abak.common.backupformat.InvalidFormatException;
import segfault.abak.common.backupformat.SerializableComponent;

import java.io.OutputStream;

public interface ManifestFile extends SerializableComponent<OutputStream> {
    String TAG = "ManifestFile";

    static ManifestFile create(int layoutVersion) {
        return new ManifestFileV1(layoutVersion);
    }

    static ManifestFile parse(@NonNull String raw, int manifestVersion) throws InvalidFormatException {
        if (manifestVersion == 1) {
            return new ManifestFileV1(raw);
        }
        Log.e(TAG, "No matching version " + manifestVersion);
        throw new InvalidFormatException();
    }

    int version();
}
