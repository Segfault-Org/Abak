package segfault.abak.common.backupformat;

import android.util.Log;
import androidx.annotation.NonNull;
import segfault.abak.common.backupformat.entries.Entry;
import segfault.abak.common.backupformat.manifest.ManifestFile;
import segfault.abak.common.packaging.PackagingSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Represents the folder of backup.
 *
 * The folder contains multiple entries. Each entry represents an application - plugin relationship.
 * How are the entries arranged, serialized, or deserialized depend on the version.
 *
 * When reading, the manifest is read first, then the whole ball is extracted.
 * The manifest contains basic information (i.e. version), so it must be read first.
 *
 * When writing, the manifest will be written before final packaging, which happens inside BackupLayout.
 *
 * Considering the format of manifest may change in the future, its file name does not
 * contain an extension.
 */
public interface BackupLayout extends SerializableComponent<Void> {
    String TAG = "BackupLayout";

    static BackupLayout create(@NonNull List<Entry> entries, @NonNull PackagingSession packagingSession) {
        return new BackupLayoutV1(entries, 1, packagingSession);
    }

    static BackupLayout parse(@NonNull File extracted, @NonNull ManifestFile manifest) throws InvalidFormatException {
        if (manifest.version() == 1) {
            return new BackupLayoutV1(extracted, manifest);
        }
        Log.e(TAG, "No matching version: " + manifest.version());
        throw new InvalidFormatException();
    }

    @NonNull
    List<Entry> entries();

    void writeNewEntry(@NonNull Entry entry, @NonNull InputStream data) throws IOException;
}
