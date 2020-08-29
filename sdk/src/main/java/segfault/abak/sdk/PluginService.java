package segfault.abak.sdk;

import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.system.OsConstants;
import android.util.Log;
import androidx.annotation.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static segfault.abak.sdk.SdkConstants.*;

public abstract class PluginService extends Service {
    private static final String TAG = "PluginService";

    private ThreadLocal<BackupRequest> mBackupRequest = new ThreadLocal<BackupRequest>() {
        @Nullable
        @Override
        protected BackupRequest initialValue() {
            return null;
        }
    };
    private ThreadLocal<RestoreRequest> mRestoreRequest = new ThreadLocal<RestoreRequest>() {
        @Nullable
        @Override
        protected RestoreRequest initialValue() {
            return null;
        }
    };
    private ThreadLocal<ResultReceiver> mCallback = new ThreadLocal<ResultReceiver>() {
        @Nullable
        @Override
        protected ResultReceiver initialValue() {
            return null;
        }
    };

    private final IPluginService.Stub mBinder = new IPluginService.Stub() {
        @Override
        public BackupResponse runBackup(BackupRequest request, ResultReceiver callback) throws RemoteException {
            if (request == null || callback == null) {
                Log.e(TAG, "Invalid call from " + Binder.getCallingUid() + ": args null");
                throw new IllegalArgumentException("request or callback is null");
            }
            Log.d(TAG, "runBackup: " + request.application);
            mBackupRequest.set(request);
            mCallback.set(callback);
            try {
                final BackupResponse response = backup(mBackupRequest.get());
                Log.i(TAG, "Backup success.");
                return response;
            } catch (Throwable e) {
                Log.d(TAG, "Encountered fetal exception", e);
                throw new IllegalStateException(e.getMessage());
            }
        }

        @Override
        public void runRestore(RestoreRequest request, ResultReceiver callback) throws RemoteException {
            if (request == null || callback == null) {
                Log.e(TAG, "Invalid call from " + Binder.getCallingUid() + ": args null");
                throw new IllegalArgumentException("request or callback is null");
            }
            Log.d(TAG, "runRestore: " + request.application);
            mRestoreRequest.set(request);
            mCallback.set(callback);
            try {
                restore(request);
                Log.i(TAG, "Restore success.");
            } catch (Throwable e) {
                Log.d(TAG, "Encountered fetal exception", e);
                throw new IllegalStateException(e.getMessage());
            }
        }
    };

    /**
     * It is not allowed to override this method.
     */
    @Override
    public final IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Report a new progress.
     * @param progress -1 for indeterminate.
     */
    public final void reportProgress(@IntRange(from = -1, to = 100) int progress) {
        // Log.d(TAG, "TRACE PROG " + progress + ": " + mBackupRequest.get().application + ": " + this.toString());
        final Bundle bundle = new Bundle();
        bundle.putInt(CALLBACK_EXTRA_PROGRESS, progress);
        mCallback.get().send(CALLBACK_CODE_PROGRESS, bundle);
    }

    /**
     * Get the backup request. It is same as 'request' param.
     */
    @NonNull
    protected final BackupRequest getBackupRequest() throws IllegalStateException {
        if (mBackupRequest == null) {
            // We are in restore mode.
            throw new IllegalStateException("We are in restore mode.");
        }
        return mBackupRequest.get();
    }

    /**
     * Performs backup. This is running under worker thread.
     * All required information is supplied in request param.
     *
     * During backup, the method should continuously report its progress by calling reportProgress()
     * @param request Request
     * @return Result
     * @throws Throwable If there is any exception.
     */
    @NonNull
    @WorkerThread
    public abstract BackupResponse backup(@NonNull BackupRequest request) throws Throwable;

    public abstract void restore(@NonNull RestoreRequest request) throws Throwable;
}
