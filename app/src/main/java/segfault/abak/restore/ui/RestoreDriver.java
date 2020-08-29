package segfault.abak.restore.ui;

import androidx.annotation.NonNull;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;
import segfault.abak.common.backupformat.BackupLayout;
import segfault.abak.common.backupformat.InvalidFormatException;
import segfault.abak.common.backupformat.manifest.ManifestFile;
import segfault.abak.common.widgets.FileUtils;

import java.io.*;

class RestoreDriver {
    public static void extract(@NonNull InputStream tarball, @NonNull File destFolder) throws IOException {
        final TarInputStream tis = new TarInputStream(new BufferedInputStream(tarball));
        TarEntry entry;

        while((entry = tis.getNextEntry()) != null) {
            int count;
            byte[] data = new byte[2048];
            FileOutputStream fos = new FileOutputStream(new File(destFolder, entry.getName()));
            BufferedOutputStream dest = new BufferedOutputStream(fos);

            while((count = tis.read(data)) != -1) {
                dest.write(data, 0, count);
            }

            dest.flush();
            dest.close();
        }

        tis.close();
    }

    @NonNull
    public static File findManifest(@NonNull File extracted) throws InvalidFormatException {
        final File[] list = extracted.listFiles((file, s) -> s.startsWith("manifest"));
        if (list == null || list.length <= 0) throw new InvalidFormatException();
        return list[0];
    }

    public static ManifestFile parseManifest(@NonNull File manifestFile) throws InvalidFormatException, IOException {
        final String[] segments = manifestFile.getName().split("\\.");
        if (segments.length != 2) {
            throw new InvalidFormatException();
        }
        if (!segments[0].equals("manifest")) throw new InvalidFormatException();
        final int version;
        try {
            version = Integer.parseInt(segments[1]);
        } catch (NumberFormatException ignored) {
            throw new InvalidFormatException();
        }

        final InputStream inputStream = new FileInputStream(manifestFile);
        final Reader reader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(reader);
        final String content = FileUtils.allLines(bufferedReader);
        bufferedReader.close();
        reader.close();
        inputStream.close();
        return ManifestFile.parse(content, version);
    }

    public static BackupLayout parseLayout(@NonNull File extracted, @NonNull ManifestFile manifest) throws InvalidFormatException {
        return BackupLayout.parse(extracted, manifest);
    }
}
