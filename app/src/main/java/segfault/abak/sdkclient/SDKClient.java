package segfault.abak.sdkclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import segfault.abak.sdk.SdkConstants;

import java.util.List;

public class SDKClient {
    private static final String TAG = "SDKClient";

    @Nullable
    private static Plugin resolvePlugin(@NonNull ResolveInfo resolveInfo, @NonNull Context context) {
        // Process ComponentName
        final ComponentName cn = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
        Log.d(TAG, "resolve: " + cn);

        final ApplicationInfo applicationInfo = assertGetApplicationInfo(context.getPackageManager(), cn.getPackageName(),
                PackageManager.GET_META_DATA);

        // Process Meta
        final Bundle appMeta = applicationInfo.metaData;
        if (appMeta == null) {
            Log.e(TAG, "No meta for " + cn.getPackageName());
            return null;
        }
        final int ver = appMeta.getInt(SdkConstants.META_SDK_VERSION, -1);
        if (ver == -1) {
            Log.e(TAG, "Invalid version: " + ver + " for " + cn);
            return null;
        }

        final PluginClient client = PluginClient.create(ver);
        if (client == null) {
            return null;
        }
        return client.parse(resolveInfo, cn, applicationInfo, context);
    }

    @Nullable
    public static Plugin resolvePlugin(@NonNull ComponentName cn, @NonNull Context context) {
        final Intent targetIntent = new Intent().setComponent(cn);
        final ResolveInfo resolveInfo =
                context.getPackageManager().resolveService(targetIntent, PackageManager.GET_META_DATA);
        if (resolveInfo == null) return null;
        return resolvePlugin(resolveInfo, context);
    }

    public static List<Plugin> listPlugins(@NonNull Context context) {
        final Intent targetIntent = new Intent(SdkConstants.ACTION_PLUGIN_SERVICE);
        final List<ResolveInfo> resolvedServices =
                context.getPackageManager().queryIntentServices(targetIntent, PackageManager.GET_META_DATA);
        return StreamSupport.stream(resolvedServices)
                .map(resolveInfo -> resolvePlugin(resolveInfo, context))
                .filter(plugin -> plugin != null)
                .collect(Collectors.toList());
    }

    private static @NonNull ApplicationInfo assertGetApplicationInfo(@NonNull PackageManager packageManager,
                                                                     @NonNull String packageName,
                                                                     int flags) {
        try {
            return packageManager.getApplicationInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static Integer tryParseInt(@NonNull String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
