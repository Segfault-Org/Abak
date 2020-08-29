package segfault.abak.backup;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import segfault.abak.BuildConfig;
import segfault.abak.common.backupformat.BackupDriver;
import segfault.abak.common.backupformat.entries.ApplicationEntryV1;
import segfault.abak.common.BindServiceSupplier;
import segfault.abak.common.widgets.FileUtils;
import segfault.abak.common.widgets.progress.Task;
import segfault.abak.sdk.BackupRequest;
import segfault.abak.sdk.BackupResponse;
import segfault.abak.sdk.IPluginService;
import segfault.abak.sdkclient.Plugin;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java9.util.concurrent.*;

import static segfault.abak.backup.BackupProgressFragment.*;

public class BackupThread extends HandlerThread implements Handler.Callback {
    private static final String TAG = "BackupThread";

    static final int MSG_OK = 1;
    static final int MSG_KO = 2;

    private final Handler mCallback;
    private final BackupOptions mOptions;
    private final Context mContext;

    private final ExecutorService mPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private Set<ServiceConnection> mToBeDisconnected = new CopyOnWriteArraySet<>();

    private volatile boolean mAtLeastOneError = false;

    private Handler mThis;

    private HashMap<Pair<String, Plugin>, BackupResponse> mResults;

    public BackupThread(@NonNull Handler callback, @NonNull BackupOptions options, @NonNull Context context) {
        super(TAG);
        mCallback = callback;
        mOptions = options;
        mContext = context;
        mResults = new HashMap<>(mOptions.plugins().size());
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
        final List<Future<Void>> tasks = StreamSupport.stream(mOptions.plugins())
                .map(plugin -> {
                    // 1: Bind to service
                    final CompletableFuture<IPluginService> future = CompletableFuture.supplyAsync(new BindServiceSupplier(mContext, new Intent().setComponent(plugin.component()), Context.BIND_AUTO_CREATE),
                            mPool)
                            .thenApply(result -> {
                                if (!result.success) {
                                    throw new IllegalStateException("Cannot bind to service " + plugin.component().flattenToShortString());
                                }
                                return result;
                            })
                            .thenApplyAsync(result -> {
                                Log.d(TAG, "Conn: " + result.success);
                                if (result.success && result.connection != null) {
                                    // Add to the list ASAP.
                                    // TODO: It still has service not registered exceptions.
                                    mToBeDisconnected.add(result.connection);
                                } else {
                                    throw new IllegalStateException("Connection failed");
                                }
                                return IPluginService.Stub.asInterface(result.binder);
                            }, mPool);

                    // 2: Flat map the applications
                    return future.thenComposeAsync(service ->
                            CompletableFuture.allOf(
                                StreamSupport.stream(mOptions.application())
                                    .map(application -> {
                                        Log.d(TAG, "Composing " + BackupProgressFragment.pluginToTask(plugin, application));
                                        // TODO: Disable application
                                        return CompletableFuture
                                                // Create the folder, then share it via FileProvider
                                                .supplyAsync(
                                                    // Call backup AIDL function
                                                    new ServiceCallable(mCallback,
                                                            new BackupRequest(
                                                                    application,
                                                                    plugin.options()
                                                            ),
                                                            plugin, service),
                                                            mPool)
                                                .thenApplyAsync(result -> {
                                                    try {
                                                        Log.d(TAG, "Copying file");
                                                        // Copy file
                                                        final InputStream in = mContext.getContentResolver().openInputStream(result.response.data);
                                                        final File cache = getReceivedCacheFile(application, plugin);
                                                        final OutputStream out = new FileOutputStream(cache);
                                                        FileUtils.copy(in, out);
                                                        out.close();
                                                        in.close();
                                                        mContext.revokeUriPermission(result.response.data, 0);
                                                        return result;
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }, mPool)
                                                // 3: Handle and compose results
                                                .handle((res, ex) -> {
                                                    Log.d(TAG, "Handle");
                                                    if (ex == null) {
                                                        BackupProgressFragment.sendProgress(mCallback,
                                                                plugin, application,
                                                                Task.PROG_COMPLETED,
                                                                MSG_PROGRESS_SRC_HANDLE);
                                                        Log.i(TAG, BackupProgressFragment.pluginToTask(plugin, application) + " succeed.");

                                                        // Not sure if it is safe.
                                                        mResults.put(new Pair<>(application, plugin), res.response);
                                                    } else {
                                                        mAtLeastOneError = true;
                                                        BackupProgressFragment.sendProgress(mCallback,
                                                                plugin, application,
                                                                Task.PROG_FAIL,
                                                                MSG_PROGRESS_SRC_HANDLE);
                                                        Log.e(TAG, BackupProgressFragment.pluginToTask(plugin, application) + " failed.", ex);
                                                    }
                                                    return res;
                                                });
                                    })
                                    .collect(Collectors.toList())
                                    .toArray(new CompletableFuture[]{})),
                            mPool);
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

        Log.d(TAG, "Disconnecting from services");
        StreamSupport.stream(mToBeDisconnected).forEach(mContext::unbindService);

        // Package
        if (!mAtLeastOneError) {
            BackupProgressFragment.sendProgress(mCallback,
                    "package",
                    Task.PROG_INDETERMINATE,
                    MSG_PROGRESS_SRC_PACKAGE);
            final BackupDriver backupDriver = new BackupDriver(StreamSupport.stream(mResults.entrySet())
                    .map(pair -> {
                        final Plugin plugin = pair.getKey().second;
                        final String application = pair.getKey().first;
                        // final BackupResponse response = pair.getValue().first.response;
                        return ApplicationEntryV1.create(
                                application,
                                plugin.component(),
                                plugin.version(),
                                getReceivedCacheFile(application, plugin)
                        );
                    })
                    .collect(Collectors.toList()),
                    mContext);
            try {
                final OutputStream stream = mContext.getContentResolver().openOutputStream(mOptions.location());
                backupDriver.write(stream);
                stream.close();
                BackupProgressFragment.sendProgress(mCallback,
                        "package",
                        Task.PROG_COMPLETED,
                        MSG_PROGRESS_SRC_PACKAGE);
            } catch (IOException e) {
                Log.e(TAG, "Unable to package", e);
                BackupProgressFragment.sendProgress(mCallback,
                        "package",
                        Task.PROG_FAIL,
                        MSG_PROGRESS_SRC_PACKAGE);
            }
        } else {
            Log.w(TAG, "At least one error occurred, skip packaging.");
            // Mark as skipped
            BackupProgressFragment.sendProgress(mCallback,
                    "package",
                    Task.PROG_SKIPPED,
                    MSG_PROGRESS_SRC_PACKAGE);
        }
        mCallback.sendMessage(mCallback.obtainMessage(mAtLeastOneError ? MSG_KO : MSG_OK));
    }

    private @NonNull File getReceivedCacheFile(@NonNull String application, @NonNull Plugin plugin) {
        return new File(mContext.getCacheDir(), BackupProgressFragment.pluginToTask(plugin, application));
    }
}
