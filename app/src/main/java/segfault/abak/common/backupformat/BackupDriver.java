package segfault.abak.common.backupformat;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import androidx.annotation.NonNull;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarOutputStream;
import segfault.abak.common.backupformat.entries.Entry;
import segfault.abak.sdkclient.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BackupDriver implements SerializableComponent<OutputStream> {
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

        for (final File file : Files.walk(rootDir.toPath())
                .filter(Files::isRegularFile)
                .map(path -> path.toFile())
                .collect(Collectors.toList())) {
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

    private static boolean deleteDirectory(@NonNull File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
