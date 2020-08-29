package segfault.abak.restore;

import android.content.Context;
import androidx.annotation.NonNull;
import segfault.abak.common.AppPluginPair;

/**
 * This should not be used outside segfaut.abak.restore package.
 */
public interface IRestoreThread {
    static IRestoreThread create(@NonNull Callback callback,
                                 @NonNull RestoreOptions options,
                                 @NonNull Context context) {
        return new RestoreThread(callback, options, context);
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
