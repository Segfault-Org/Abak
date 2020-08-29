package segfault.abak.backup;

import android.net.Uri;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.auto.value.AutoValue;
import segfault.abak.common.AppPluginPair;

import java.util.List;

/**
 * This class should not be used outside of segfault.abak.backup
 */
@AutoValue
public abstract class BackupThreadOptions implements Parcelable {
    @NonNull
    public static BackupThreadOptions create(@NonNull List<AppPluginPair> tasks, @NonNull Uri location) {
        return new AutoValue_BackupThreadOptions(tasks, location);
    }

    @NonNull
    public abstract List<AppPluginPair> tasks();

    @NonNull
    public abstract Uri location();
}
