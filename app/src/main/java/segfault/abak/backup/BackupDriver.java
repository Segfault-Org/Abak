package segfault.abak.backup;

import android.content.Context;
import androidx.annotation.NonNull;
import segfault.abak.common.backupformat.BackupLayout;
import segfault.abak.common.backupformat.SerializableComponent;

import java.io.IOException;
import java.io.OutputStream;

class BackupDriver implements SerializableComponent<OutputStream> {
    private static final String TAG = "BackupDriver";

    private final BackupLayout mLayout;
    private final Context mContext;

    public BackupDriver(@NonNull BackupLayout layout, @NonNull Context context) {
        mLayout = layout;
        mContext = context;
    }

    @Override
    public void write(@NonNull OutputStream outputStream) throws IOException {
        mLayout.write(null);
    }
}
