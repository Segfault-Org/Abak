package segfault.abak.backup;

import android.content.Context;
import androidx.annotation.NonNull;
import segfault.abak.common.AppPluginPair;

/**
 * This should not be used outside segfaut.abak.backup package.
 */
public interface IBackupThread {
    static IBackupThread create(@NonNull Callback callback,
                                @NonNull BackupThreadOptions options,
                                @NonNull Context context) {
        return new BackupThread(callback, options, context);
    }

    void startThread();
    void stopThread();
    interface Callback {
        default void sendProgress(@NonNull AppPluginPair task, int progress) {
            sendProgress(task.taskName(), progress);
        }
        void sendProgress(@NonNull String task, int progress);
        void sendResult(boolean success);
    }
}
