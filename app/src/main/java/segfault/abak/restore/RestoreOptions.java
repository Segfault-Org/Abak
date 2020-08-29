package segfault.abak.restore;

import android.content.Context;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import java.util.stream.StreamSupport;
import segfault.abak.common.AppUtils;
import segfault.abak.sdkclient.Plugin;

import java.io.File;
import java.util.ArrayList;

@AutoValue
public abstract class RestoreOptions implements Parcelable {
    @NonNull
    public static RestoreOptions create(@NonNull ArrayList<AppPluginPair> apps, @NonNull File extracted) {
        return new AutoValue_RestoreOptions(apps, extracted);
    }

    @NonNull
    public abstract ArrayList<AppPluginPair> apps();

    @NonNull
    public abstract File extracted();

    public final boolean validate(@NonNull Context context) {
        return !apps().isEmpty();
    }

    @AutoValue
    public static abstract class AppPluginPair implements Parcelable {
        public static AppPluginPair create(@NonNull String application,
                                           @NonNull File data,
                                           @NonNull Plugin plugin) {
            return new AutoValue_RestoreOptions_AppPluginPair(application, data, plugin);
        }

        @NonNull
        public abstract String application();

        @NonNull
        public abstract File data();

        @NonNull
        public abstract Plugin plugin();

        public final boolean equals(@Nullable Object that) {
            if (!(that instanceof AppPluginPair)) return false;
            final AppPluginPair pair = (AppPluginPair) that;
            return this.application().equals(pair.application()) &&
                    this.plugin().equals(pair.plugin());
        }
    }
}
