package segfault.abak.common.backupformat.entries;

import android.os.Parcelable;
import androidx.annotation.NonNull;
import segfault.abak.common.backupformat.SerializableComponent;

import java.io.File;

public abstract class Entry implements SerializableComponent<File>, Parcelable {
    @NonNull
    public abstract String toPersistableName();
}
