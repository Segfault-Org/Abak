package segfault.abak.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import java9.util.concurrent.CompletableFuture;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import segfault.abak.common.AppPluginPair;
import segfault.abak.common.BindServiceSupplier;
import segfault.abak.common.backupformat.BackupLayout;
import segfault.abak.common.backupformat.entries.ApplicationEntryV1;
import segfault.abak.common.packaging.PackagingSession;
import segfault.abak.common.widgets.progress.Task;
import segfault.abak.sdk.BackupRequest;
import segfault.abak.sdk.BackupResponse;
import segfault.abak.sdk.IPluginService;
import segfault.abak.sdkclient.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class BackupThread extends HandlerThread implements Handler.Callback, IBackupThread {
    private static final String TAG = "BackupThread";

    private final IBackupThread.Callback mCallback;
    private final BackupThreadOptions mOptions;
    private final Context mContext;

    private final ExecutorService mPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    private volatile boolean mAtLeastOneError = false;

    private Handler mThis;

    private HashMap<AppPluginPair, BackupResponse> mResults;
    private HashMap<Plugin, BindServiceSupplier.Result> mConns;

    private final OutputStream mOut;
    private final BackupLayout mLayout;

    public BackupThread(@NonNull IBackupThread.Callback callback, @NonNull BackupThreadOptions options, @NonNull Context context) {
        super(TAG);
        mCallback = callback;
        mOptions = options;
        mContext = context;
        mResults = new HashMap<>(mOptions.tasks().size());
        mConns = new HashMap<>(mOptions.tasks().size());

        try {
            mOut = mContext.getContentResolver().openOutputStream(mOptions.location());
            final PackagingSession session = mOptions.resolvePackager().startSession(mOut);
            mLayout = BackupLayout.create(Collections.emptyList(), session);
        } catch (IOException e) {
            Log.e(TAG, "Unable to initiate the session", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        Log.d(TAG, "Handle message: " + message.what + " in " + Thread.currentThread());
        return false;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mThis = new Handler(Looper.myLooper(), this);
        Log.d(TAG, "Starting in " + Thread.currentThread());
        try {
            // Wait for addTasks() to draw. This is a really bad design.
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        // TODO: Connecting may also be done in parallel.
        for (final AppPluginPair pair : mOptions.tasks()) {
            final Plugin plugin = pair.plugin();
            if (mConns.containsKey(plugin)) continue;
            final BindServiceSupplier.Result result = new BindServiceSupplier(mContext,
                    new Intent().setComponent(plugin.component()),
                    Context.BIND_AUTO_CREATE).get();
            if (!result.success) {
                mCallback.sendProgress(pair, Task.PROG_FAIL);
                mAtLeastOneError = true;
            }
            mConns.put(plugin, result);
        }

        if (!mAtLeastOneError) {
            final List<CompletableFuture<ServiceCallable.Result>> tasks = StreamSupport.stream(mOptions.tasks())
                    .map(task -> {
                        final IPluginService binder = IPluginService.Stub.asInterface(mConns.get(task.plugin()).binder);
                        // TODO: Disable application
                        return CompletableFuture
                                .supplyAsync(
                                        // Call backup AIDL function
                                        new ServiceCallable(mCallback,
                                                new BackupRequest(
                                                        task.application(),
                                                        task.plugin().options()
                                                ),
                                                task.plugin(),
                                                binder),
                                        mPool)
                                .thenApplyAsync(result -> {
                                    try {
                                        Log.d(TAG, "Packaging file");
                                        // Copy file
                                        // TODO: Copying can be skipped and the file may be directly added to the tarball.
                                        final InputStream in = mContext.getContentResolver().openInputStream(result.response.data);
                                        mLayout.writeNewEntry(ApplicationEntryV1.create(
                                                task.application(),
                                                task.plugin().component(),
                                                task.plugin().version()
                                        ), in);
                                        mContext.revokeUriPermission(result.response.data, 0);
                                        return result;
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }, mPool)
                                // 3: Handle and compose results
                                .handle((res, ex) -> {
                                    if (ex == null) {
                                        mCallback.sendProgress(task, Task.PROG_COMPLETED);
                                        Log.i(TAG, task.taskName() + " succeed.");

                                        // Not sure if it is safe.
                                        mResults.put(task, res.response);
                                    } else {
                                        mAtLeastOneError = true;
                                        mCallback.sendProgress(task, Task.PROG_FAIL);
                                        Log.e(TAG, task.taskName() + " failed.", ex);
                                    }
                                    return res;
                                });
                    })
                    .collect(Collectors.toList());
            final CompletableFuture<Void> bigFuture =
                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[]{}));
            try {
                bigFuture.get();
            } catch (Exception e) {
                Log.e(TAG, "General Failure", e);
                mAtLeastOneError = true;
            }
        }

        Log.d(TAG, "Disconnecting from services");
        StreamSupport.stream(mConns.values()).map(result -> result.connection).forEach(mContext::unbindService);

        // Package
        if (!mAtLeastOneError) {
            mCallback.sendProgress("package", Task.PROG_INDETERMINATE);
            final BackupDriver backupDriver = new BackupDriver(mLayout, mContext);
            try {
                backupDriver.write(null);
                mOut.close();
                mCallback.sendProgress("package", Task.PROG_COMPLETED);
            } catch (IOException e) {
                Log.e(TAG, "Unable to package", e);
                mCallback.sendProgress("package", Task.PROG_FAIL);
            }
        } else {
            Log.w(TAG, "At least one error occurred, skip packaging.");
            // Mark as skipped
            mCallback.sendProgress("package", Task.PROG_SKIPPED);
        }
        mCallback.sendResult(!mAtLeastOneError);
    }

    @Override
    public void startThread() {
        start();
    }

    @Override
    public void stopThread() {
        quitSafely();
    }
}
