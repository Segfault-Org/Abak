package segfault.abak.common;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

public class AppUtils {
    public static @NonNull CharSequence appName(@NonNull String application, @NonNull Context context) {
        try {
            return context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(application, 0));
        } catch (PackageManager.NameNotFoundException ignored) {
            return application;
        }
    }
}
