package segfault.abak.common;

import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import segfault.abak.sdkclient.Plugin;

@AutoValue
public abstract class AppPluginPair implements Parcelable {
    public static AppPluginPair create(@NonNull String application,
                                                      @NonNull Plugin plugin) {
        return new AutoValue_AppPluginPair(application, plugin);
    }

    @NonNull
    public abstract String application();

    @NonNull
    public abstract Plugin plugin();

    public final boolean equals(@Nullable Object that) {
        if (!(that instanceof AppPluginPair)) return false;
        final AppPluginPair pair = (AppPluginPair) that;
        return this.application().equals(pair.application()) &&
                this.plugin().equals(pair.plugin());
    }

    @NonNull
    public final String taskName() {
        return "plug_" +
                plugin().component().getPackageName() + ":" +
                plugin().component().getClassName() + "_" +
                application();
    }
}