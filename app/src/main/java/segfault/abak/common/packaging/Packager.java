package segfault.abak.common.packaging;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Packager {
    public final String mime;
    public final String defaultExtension;

    public Packager(@NonNull String mime,
                    @NonNull String defaultExtension) {
        this.mime = mime;
        this.defaultExtension = defaultExtension;
    }

    public abstract boolean detect(@NonNull InputStream in) throws IOException;

    @NonNull
    public abstract CharSequence loadName(@NonNull Context context);

    @NonNull
    public abstract PackagingSession startSession(@NonNull OutputStream out);

    public abstract void unpack(@NonNull InputStream in, @NonNull File extracted) throws IOException;
}
