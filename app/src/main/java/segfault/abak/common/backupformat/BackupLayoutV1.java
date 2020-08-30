package segfault.abak.common.backupformat;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import segfault.abak.common.backupformat.entries.ApplicationEntryV1;
import segfault.abak.common.backupformat.entries.Entry;
import segfault.abak.common.backupformat.manifest.ManifestFile;
import segfault.abak.common.packaging.PackagingSession;
import segfault.abak.restore.ApplicationEntryWithInput;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * /                                            {@link BackupLayout}
 * /manifest.(version)                          {@link ManifestFile}
 * /app:(application):(plugin):(plugin version) {@link ApplicationEntryV1}
 */
class BackupLayoutV1 implements BackupLayout {
    private static final String TAG = "BackupLayoutV1";

    private final List<Entry> mEntries;
    private final ManifestFile mManifest;

    // If not null, the layout is created manually and running under backup mode.
    @Nullable
    private PackagingSession mPackaging;

    BackupLayoutV1(@NonNull List<Entry> entries, int version, @NonNull PackagingSession packagingSession) {
        mEntries = entries;
        mManifest = ManifestFile.create(version);
        mPackaging = packagingSession;
    }

    BackupLayoutV1(@NonNull File extracted, @NonNull ManifestFile manifest) throws InvalidFormatException {
        if (!extracted.isDirectory()) {
            throw new IllegalArgumentException("Extracted is not a directory");
        }
        final File[] sub = extracted.listFiles();
        if (sub == null) {
            throw new InvalidFormatException();
        }
        mEntries = StreamSupport.stream(Arrays.asList(sub))
                .map(file -> {
                    final String[] segments = file.getName().split(":");
                    try {
                        switch (segments[0]) {
                            case "app":
                                return ApplicationEntryWithInput.create(ApplicationEntryV1.parse(file),
                                        file);
                            default:
                                Log.w(TAG, "Ignoring " + segments[0]);
                                return null;
                        }
                    } catch (InvalidFormatException e) {
                        return null;
                    }
                })
                .collect(Collectors.toList());
        if (mEntries == null) {
            throw new InvalidFormatException();
        }

        mManifest = manifest;
    }

    @NonNull
    @Override
    public List<Entry> entries() {
        return mEntries;
    }

    @Override
    public void write(@NonNull Void na) throws IOException {
        synchronized (this) {
            if (mPackaging == null)
                throw new IllegalStateException("The layout is not running under backup mode");
            // Move files. Final packaging is done by driver.
            final ByteArrayOutputStream manifestOut = new ByteArrayOutputStream();
            mManifest.write(manifestOut);
            final InputStream manifestIn = new ByteArrayInputStream(manifestOut.toByteArray());
            manifestOut.close();
            mPackaging.addFile(manifestIn, "manifest.1");

            mPackaging.close();
        }
    }

    @Override
    public void writeNewEntry(@NonNull Entry entry, @NonNull InputStream data) throws IOException {
        synchronized (this) {
            if (mPackaging == null) {
                throw new IllegalStateException("The layout is not running under backup mode");
            }
            mPackaging.addFile(data, entry.toPersistableName());
        }
    }
}
