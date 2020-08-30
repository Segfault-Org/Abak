package segfault.abak.common.backupformat.entries;

import android.content.ComponentName;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.auto.value.AutoValue;
import segfault.abak.common.backupformat.InvalidFormatException;

import java.io.File;
import java.io.IOException;

@AutoValue
public abstract class ApplicationEntryV1 extends Entry {
    private static final String TAG = "ApplicationEntryV1";

    public static ApplicationEntryV1 parse(@NonNull File data)
            throws InvalidFormatException {
        if (!data.isFile()) {
            throw new InvalidFormatException();
        }

        final String[] segments = data.getName().split(":");
        final String application = segments[1];
        final String pluginComponent = segments[2];
        final String pluginVersionRaw = segments[3];
        final int pluginVersion;
        try {
            pluginVersion = Integer.parseInt(pluginVersionRaw);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Cannot parse plugin version", e);
            throw new InvalidFormatException();
        }
        return ApplicationEntryV1.create(application,
                ComponentName.unflattenFromString(
                        pluginComponent.replace('-', '/')),
                pluginVersion);
    }

    public static ApplicationEntryV1 create(@NonNull String application,
                                            @NonNull ComponentName pluginComponent,
                                            int pluginVersion) {
        return new AutoValue_ApplicationEntryV1(application,
                pluginComponent,
                pluginVersion);
    }

    @Override
    public final String toPersistableName() {
        return String.format("app:%1$s:%2$s:%3$s",
                application(),
                persistablePluginComponent(),
                pluginVersion());
    }

    @NonNull
    public abstract String application();

    @NonNull
    public abstract ComponentName pluginComponent();

    public final String persistablePluginComponent() {
        return pluginComponent().flattenToString().replace('/', '-');
    }

    public abstract int pluginVersion();

    @Override
    public void write(@NonNull File file) throws IOException {

    }
}
