package segfault.abak.common.packaging;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class PackagingSession implements Closeable {
    @NonNull
    protected final OutputStream mOut;

    public PackagingSession(@NonNull OutputStream out) {
        this.mOut = out;
    }

    public abstract void addFile(@NonNull InputStream in, @NonNull String relativePath) throws IOException;
}
