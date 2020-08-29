package segfault.abak.common.backupformat;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SerializableComponent<OUT> {
    void write(@NonNull OUT out) throws IOException;
}
