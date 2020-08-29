package segfault.abak.common.backupformat;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import segfault.abak.common.backupformat.entries.ApplicationEntryV1;
import segfault.abak.common.backupformat.entries.Entry;
import segfault.abak.common.backupformat.manifest.ManifestFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    BackupLayoutV1(@NonNull List<Entry> entries, int version) {
        mEntries = entries;
        mManifest = ManifestFile.create(version);
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
                                return ApplicationEntryV1.parse(file);
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
    public void write(@NonNull File file) throws IOException {
        // Move files. Final packaging is done by driver.
        final File manifestFile = new File(file, "manifest.1");
        final OutputStream manifestOut = new FileOutputStream(manifestFile);
        mManifest.write(manifestOut);
        manifestOut.close();

        final long errors = StreamSupport.stream(mEntries)
                .filter(item -> item instanceof ApplicationEntryV1)
                .map(entry -> {
                    final ApplicationEntryV1 appEntry = (ApplicationEntryV1) entry;
                    // To, From
                    return new Pair<>(new File(file, String.format("app:%1$s:%2$s:%3$s",
                            appEntry.application(),
                            appEntry.persistablePluginComponent(),
                            appEntry.pluginVersion())),
                            appEntry.data());
                })
                .map(pair -> {
                    final File target = pair.first;
                    final File source = pair.second;
                    try {
                        Os.rename(source.getAbsolutePath(), target.getAbsolutePath());
                        return true;
                    } catch (ErrnoException e) {
                        Log.e(TAG, "Cannot rename() from " + source.getName() + " to " + target.getName(), e);
                        Log.e(TAG, "Dump1 " + source.getAbsolutePath());
                        Log.e(TAG, "Dump2 " + target.getAbsolutePath());
                        return false;
                    }
                })
                .filter(result -> !result) /* Check if there are errors */
                .count();
        if (errors > 0) {
            Log.e(TAG, "Moving entries caused one or more errors: " + errors);
            throw new IOException("Moving entries caused one or more errors: " + errors);
        }
    }
}
