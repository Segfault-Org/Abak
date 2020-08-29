package segfault.abak.backup;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarOutputStream;
import segfault.abak.common.backupformat.BackupLayout;
import segfault.abak.common.backupformat.SerializableComponent;
import segfault.abak.common.backupformat.entries.Entry;
import segfault.abak.common.widgets.FileUtils;

import java.io.*;
import java.util.List;

import static segfault.abak.common.widgets.FileUtils.deleteDirectory;

class BackupDriver implements SerializableComponent<OutputStream> {
    private static final String TAG = "BackupDriver";

    private final BackupLayout mLayout;
    private final Context mContext;

    public BackupDriver(@NonNull List<Entry> entries, @NonNull Context context) {
        mLayout = BackupLayout.create(entries);
        mContext = context;
    }

    @Override
    public void write(@NonNull OutputStream outputStream) throws IOException {
        final File rootDir = new File(mContext.getCacheDir(),
                "backup-" + System.currentTimeMillis());
        Log.i(TAG, "Writing to " + rootDir.getAbsolutePath());
        rootDir.mkdir();
        mLayout.write(rootDir);

        // Package
        final TarOutputStream out = new TarOutputStream(outputStream);

        for (final File file : FileUtils.walk(rootDir)) {
            out.putNextEntry(new TarEntry(file, file.getName()));
            BufferedInputStream origin = new BufferedInputStream(new FileInputStream(file));
            int count;
            byte[] data = new byte[2048];

            while((count = origin.read(data)) != -1) {
                out.write(data, 0, count);
            }

            out.flush();
            origin.close();
        }
        out.close();

        if (!deleteDirectory(rootDir))
            Log.e(TAG, "Cannot delete " + rootDir.getAbsolutePath());
    }
}
