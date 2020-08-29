package segfault.abak.sdkclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SharedMemory;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import segfault.abak.sdk.BackupRequest;
import segfault.abak.sdk.BackupResponse;
import segfault.abak.sdk.IPluginService;

public interface PluginClient {
    String TAG = "PluginClient";

    @Nullable
    static PluginClient create(int version) {
        if (version == 1) {
            return new PluginClientV1();
        }
        // Host needs to be upgraded.
        Log.e(TAG, "Unknown version: " + version);
        return null;
    }

    @Nullable
    Plugin parse(@NonNull ResolveInfo info, @NonNull ComponentName cn, @NonNull ApplicationInfo applicationInfo, @NonNull Context context);

    @NonNull
    BackupResponse backup(@NonNull IPluginService service, @NonNull BackupRequest request, @NonNull ResultReceiver callback) throws RemoteException;
}
