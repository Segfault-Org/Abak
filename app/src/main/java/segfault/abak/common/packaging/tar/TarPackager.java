package segfault.abak.common.packaging.tar;

import android.content.Context;
import androidx.annotation.NonNull;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarInputStream;
import org.kamranzafar.jtar.TarOutputStream;
import segfault.abak.R;
import segfault.abak.common.packaging.Packager;
import segfault.abak.common.packaging.PackagingSession;

import java.io.*;
import java.util.Arrays;

public class TarPackager extends Packager {
    // Tar magic: 75 73 74 61
    private static final byte[] MAGIC = new byte[]{ 117, 115, 116, 97 };

    public TarPackager() {
        super("application/x-tar");
    }

    @Override
    public boolean detect(@NonNull InputStream in) throws IOException {
        in.mark(4);
        byte[] magic = new byte[4];
        in.skip(0x101);
        in.read(magic, 0, 4);
        in.reset();
        return Arrays.equals(magic, MAGIC);
    }

    @NonNull
    @Override
    public CharSequence loadName(@NonNull Context context) {
        return context.getString(R.string.packager_tar);
    }

    @NonNull
    @Override
    public PackagingSession startSession(@NonNull OutputStream out) {
        return new TarPackagingSession(out);
    }

    @Override
    public void unpack(@NonNull InputStream in, @NonNull File extracted) throws IOException {
        final TarInputStream tis = new TarInputStream(in);
        TarEntry entry;

        while((entry = tis.getNextEntry()) != null) {
            int count;
            byte[] data = new byte[2048];
            FileOutputStream fos = new FileOutputStream(new File(extracted, entry.getName()));
            BufferedOutputStream dest = new BufferedOutputStream(fos);

            while((count = tis.read(data)) != -1) {
                dest.write(data, 0, count);
            }

            dest.flush();
            dest.close();
        }

        tis.close();
    }

    private static class TarPackagingSession extends PackagingSession {
        private final TarOutputStream mTarOut;

        public TarPackagingSession(@NonNull OutputStream out) {
            super(out);
            mTarOut = new TarOutputStream(out);
        }

        @Override
        public void addFile(@NonNull InputStream in, @NonNull String relativePath) throws IOException {
            long size = 0;
            int count;
            byte[] data = new byte[2048];
            final ByteArrayOutputStream tmpfs = new ByteArrayOutputStream();
            while((count = in.read(data)) != -1) {
                tmpfs.write(data, 0, count);
                size += count;
            }
            tmpfs.flush();

            final TarHeader header =
                    TarHeader.createHeader(relativePath,
                            size,
                            System.currentTimeMillis() / 1000,
                            false,
                            420 /* 0644 */);

            mTarOut.putNextEntry(new TarEntry(header));

            tmpfs.writeTo(mTarOut);
            mTarOut.flush();
        }

        @Override
        public void close() throws IOException {
            mTarOut.close();
            mOut.close();
        }
    }
}
