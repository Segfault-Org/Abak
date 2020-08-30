package segfault.abak.restore;

import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.auto.value.AutoValue;
import segfault.abak.common.backupformat.entries.ApplicationEntryV1;
import segfault.abak.common.backupformat.entries.Entry;

import java.io.File;
import java.io.IOException;

@AutoValue
public abstract class ApplicationEntryWithInput extends Entry implements Parcelable {
    @NonNull
    public static ApplicationEntryWithInput create(@NonNull ApplicationEntryV1 entry, @NonNull File data) {
        return new AutoValue_ApplicationEntryWithInput(entry, data);
    }

    @NonNull
    public abstract ApplicationEntryV1 entry();

    @NonNull
    public abstract File data();

    @Override
    public void write(@NonNull File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public String toPersistableName() {
        throw new UnsupportedOperationException();
    }
}
