package segfault.abak.backup;

import android.net.Uri;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.auto.value.AutoValue;
import segfault.abak.common.AppPluginPair;
import segfault.abak.common.packaging.Packager;
import segfault.abak.common.packaging.PrebuiltPackagers;

import java.util.List;

/**
 * This class should not be used outside of segfault.abak.backup
 */
@AutoValue
public abstract class BackupThreadOptions implements Parcelable {
    @NonNull
    public static BackupThreadOptions create(@NonNull List<AppPluginPair> tasks, @NonNull Uri location, int packager) {
        return new AutoValue_BackupThreadOptions(tasks, location, packager);
    }

    @NonNull
    public abstract List<AppPluginPair> tasks();

    @NonNull
    public abstract Uri location();

    public abstract int packager();

    @NonNull
    public final Packager resolvePackager() {
        // The packager() method should always return a valid result
        // since we had already validated before converting to thread options.
        return PrebuiltPackagers.PREBUILT_PACKAGERS[packager()];
    }
}
