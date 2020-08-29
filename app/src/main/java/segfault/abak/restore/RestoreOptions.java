package segfault.abak.restore;

import android.content.Context;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import segfault.abak.common.AppPluginPair;

import java.io.File;
import java.util.ArrayList;

/**
 * You should not use this class outside of the package.
 */
@AutoValue
public abstract class RestoreOptions implements Parcelable {
    @NonNull
    public static RestoreOptions create(@NonNull ArrayList<DataAppPluginPair> apps, @NonNull File extracted) {
        return new AutoValue_RestoreOptions(apps, extracted);
    }

    @NonNull
    public abstract ArrayList<DataAppPluginPair> apps();

    @NonNull
    public abstract File extracted();

    public final boolean validate(@NonNull Context context) {
        return !apps().isEmpty();
    }

    @AutoValue
    public static abstract class DataAppPluginPair implements Parcelable {
        public static DataAppPluginPair create(@NonNull AppPluginPair pair,
                                               @NonNull File data) {
            return new AutoValue_RestoreOptions_DataAppPluginPair(data, pair);
        }

        @NonNull
        public abstract File data();

        @NonNull
        public abstract AppPluginPair pair();

        @Override
        public final boolean equals(@Nullable Object that) {
            if (!(that instanceof DataAppPluginPair)) return false;
            return pair().equals(((DataAppPluginPair) that).pair());
        }
    }
}
