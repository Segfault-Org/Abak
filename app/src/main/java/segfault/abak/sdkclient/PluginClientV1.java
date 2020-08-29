package segfault.abak.sdkclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SharedMemory;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import segfault.abak.sdk.BackupRequest;
import segfault.abak.sdk.BackupResponse;
import segfault.abak.sdk.IPluginService;
import segfault.abak.sdk.SdkConstants;

class PluginClientV1 implements PluginClient {
    private static final String TAG = "PluginClientV1";

    @Nullable
    @Override
    public Plugin parse(@NonNull ResolveInfo info, @NonNull ComponentName cn, @NonNull ApplicationInfo applicationInfo, @NonNull Context context) {
        final Bundle serviceMeta = info.serviceInfo.metaData;
        ComponentName settingsActivity = null;
        boolean requireSettings = false;

        if (serviceMeta != null) {
            final String rawSettingsActivity = serviceMeta.getString(SdkConstants.META_SETTINGS_ACTIVITY);
            if (rawSettingsActivity != null) {
                final String rawSettingsActivityPatched;
                // Allowing <package name>/ to be ignored.
                int sep = rawSettingsActivity.indexOf('/');
                if (sep < 0 || (sep+1) >= rawSettingsActivity.length()) {
                    rawSettingsActivityPatched = cn.getPackageName() + "/" + rawSettingsActivity;
                } else {
                    rawSettingsActivityPatched = rawSettingsActivity;
                }
                settingsActivity = ComponentName.unflattenFromString(rawSettingsActivityPatched);
                Log.d(TAG, "Settings Activity: " + settingsActivity);
                try {
                    if (!context.getPackageManager().getActivityInfo(settingsActivity, 0).exported) {
                        Log.e(TAG, rawSettingsActivityPatched + " is not exported, abort.");
                        return null;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, rawSettingsActivityPatched + " is an invalid settings activity, abort.");
                    return null;
                }
            }
            requireSettings = serviceMeta.getBoolean(SdkConstants.META_REQUIRE_SETTINGS);

            if (requireSettings && settingsActivity == null) {
                Log.e(TAG, "Settings required but settings activity is null, abort.");
                return null;
            }
        }
        return Plugin.create(cn,
                1,
                null,
                settingsActivity,
                requireSettings);
    }

    @NonNull
    @Override
    public BackupResponse backup(@NonNull IPluginService service, @NonNull BackupRequest request, @NonNull ResultReceiver callback) throws RemoteException {
        return service.runBackup(request, callback);
    }
}
