// IPluginService.aidl
package segfault.abak.sdk;

import segfault.abak.sdk.BackupRequest;
import segfault.abak.sdk.RestoreRequest;
import android.os.ResultReceiver;
import segfault.abak.sdk.BackupResponse;

interface IPluginService {
    BackupResponse runBackup(in BackupRequest request, in ResultReceiver callback);
    void runRestore(in RestoreRequest request, in ResultReceiver callback);
}
