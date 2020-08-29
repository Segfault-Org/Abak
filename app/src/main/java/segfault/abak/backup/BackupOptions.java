package segfault.abak.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;

import java9.util.stream.StreamSupport;
import segfault.abak.sdkclient.Plugin;

import java.util.ArrayList;

/**
 * Immutable options entity for backing up.
 */
@AutoValue
public abstract class BackupOptions implements Parcelable {
    @NonNull
    public static BackupOptions create(@NonNull ArrayList<String> application,
                                       @NonNull ArrayList<Plugin> plugins,
                                       @Nullable Uri location) {
        return new AutoValue_BackupOptions(application,
                plugins,
                location);
    }

    /**
     * Application ID to backup.
     */
    @NonNull
    abstract ArrayList<String> application();

    /**
     * All enabled plugins
     */
    @NonNull
    abstract ArrayList<Plugin> plugins();

    /**
     * URI to be saved.
     */
    @Nullable
    abstract Uri location();


    /**
     * Validates the options.
     * @return Whether the options is valid for backup up.
     */
    public final boolean validate(@NonNull Context context) {
        if (StreamSupport.stream(plugins())
                .filter(plugin -> plugin.requireSettings() && plugin.options() == null)
                .count() > 0) return false;
        return plugins().size() != 0 &&
                location() != null &&
                application().size() != 0;
    }
}
