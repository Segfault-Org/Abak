package segfault.abak.sdkclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.auto.value.AutoValue;

/**
 * Entity that describes a plugin with its settings.
 */
@AutoValue
public abstract class Plugin implements Parcelable {
    @NonNull
    public static Plugin create(@NonNull ComponentName component,
                                int version,
                                @Nullable Bundle options,
                                @Nullable ComponentName settingsActivity,
                                boolean requireSettings) {
        return new AutoValue_Plugin(component, version, options, settingsActivity, requireSettings);
    }

    @NonNull
    public abstract ComponentName component();

    public abstract int version();

    @Nullable
    public abstract Bundle options();

    @Nullable
    public abstract ComponentName settingsActivity();

    public abstract boolean requireSettings();

    @Override
    public final boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Plugin)) return false;
        final Plugin that = (Plugin) obj;
        return this.component().equals(that.component());
    }

    @NonNull
    @Override
    public String toString() {
        return component().flattenToShortString();
    }

    public final CharSequence loadTitle(@NonNull Context context) {
        try {
            return context.getPackageManager().getServiceInfo(component(), 0).loadLabel(context.getPackageManager());
        } catch (PackageManager.NameNotFoundException ignored) {
            return component().flattenToShortString();
        }
    }
}
