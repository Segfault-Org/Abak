package segfault.abak.restore;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import java9.util.function.Supplier;
import segfault.abak.common.AppPluginPair;
import segfault.abak.sdk.IPluginService;
import segfault.abak.sdk.RestoreRequest;
import segfault.abak.sdk.SdkConstants;
import segfault.abak.sdkclient.Plugin;

class ServiceCallable implements Supplier<Uri> {
    private static final String TAG = "ServiceCallable";

    private final IRestoreThread.Callback mUIHandler;

    private final RestoreRequest mRequest;
    private final Plugin mPlugin;
    private final IPluginService mService;

    private volatile boolean mDone; // Used to prevent additional (or delayed) messages from being received once the call is completed.

    ServiceCallable(final @NonNull IRestoreThread.Callback uiHandler,
                    final @NonNull RestoreRequest request,
                    final @NonNull Plugin plugin,
                    final @NonNull IPluginService service) {
        this.mUIHandler = uiHandler;
        this.mRequest = request;
        this.mPlugin = plugin;
        this.mService = service;
    }

    @Override
    @WorkerThread
    public Uri get() {
        final AppPluginPair pair = AppPluginPair.create(mRequest.application, mPlugin);
        Log.d(TAG, "START " + pair.taskName() + " THREAD " + Thread.currentThread());
        final PluginCallback callback = new PluginCallback(null /* Use null to receive calls directly on the binder thread, which reduces latency. */,
                progress -> {
            if (mDone) {
                // This is caused by latencies. One of the major barrier to real time updating is the UI processing.
                // That's the reason why we chose RecyclerView, which is fast.
                Log.w(TAG, "Ignoring message since the call is done: " + progress);
                return;
            }
            mUIHandler.sendProgress(pair, progress);
        });

        try {
            mService.runRestore(mRequest, callback);
            mDone = true;
            Log.i(TAG, "Call success");
        } catch (Exception e) {
            // We have to do clean-ups, like disconnecting from the service.
            Log.e(TAG, "Remote service exception", e);
            mDone = true;
            throw new RuntimeException(e);
        }
        return mRequest.data;
    }

    private static class PluginCallback extends ResultReceiver {
        private final Callback mCallback;

        public PluginCallback(@Nullable Handler handler, @NonNull Callback callback) {
            super(handler);
            this.mCallback = callback;
        }

        @Override
        @UiThread
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Log.d(TAG, "TRACE REC: " + resultCode + " " + Thread.currentThread());
            switch (resultCode) {
                case SdkConstants.CALLBACK_CODE_PROGRESS:
                    if (resultData == null) {
                        Log.e(TAG, "CODE_PROGRESS with null bundle");
                        return;
                    }
                    int progress = resultData.getInt(SdkConstants.CALLBACK_EXTRA_PROGRESS, -2);
                    if (progress == -2) {
                        Log.e(TAG, "CODE_PROGRESS without EXTRA_PROGRESS");
                        return;
                    }
                    if (progress > 100 || progress < -1) {
                        Log.e(TAG, "CODE_PROGRESS with unexpected progress");
                        return;
                    }
                    // Log.d(TAG, "Received progress " + progress);
                    mCallback.setProgress(progress);
                    break;
            }
        }

        public interface Callback {
            void setProgress(int progress);
        }
    }
}
