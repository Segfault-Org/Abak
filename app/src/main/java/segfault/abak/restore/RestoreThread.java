package segfault.abak.restore;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import java9.util.concurrent.CompletableFuture;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import segfault.abak.common.AppPluginPair;
import segfault.abak.common.BindServiceSupplier;
import segfault.abak.common.widgets.progress.Task;
import segfault.abak.sdk.IPluginService;
import segfault.abak.sdk.RestoreRequest;
import segfault.abak.sdkclient.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static segfault.abak.common.widgets.FileUtils.deleteDirectory;

class RestoreThread extends HandlerThread implements IRestoreThread {
    private static final String TAG = "RestoreThread";

    private final IRestoreThread.Callback mCallback;
    private final RestoreOptions mOptions;
    private final Context mContext;

    private final ExecutorService mPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    private volatile boolean mAtLeastOneError = false;

    private HashMap<Plugin, BindServiceSupplier.Result> mConns;

    public RestoreThread(@NonNull IRestoreThread.Callback callback, @NonNull RestoreOptions options, @NonNull Context context) {
        super(TAG);
        mCallback = callback;
        mOptions = options;
        mContext = context;
        mConns = new HashMap<>(mOptions.apps().size());
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        Log.d(TAG, "Starting in " + Thread.currentThread());
        try {
            // Wait for addTasks() to draw. This is a really bad design.
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        // TODO: Connecting may also be done in parallel.
        for (final RestoreOptions.DataAppPluginPair p : mOptions.apps()) {
            final AppPluginPair pair = p.pair();
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
            final List<Future<Void>> tasks = StreamSupport.stream(mOptions.apps())
                    .map(pair -> {
                        // 1: Bind to service
                        return CompletableFuture.supplyAsync(() -> {
                            final Uri uri = FileProvider.getUriForFile(mContext,
                                    "segfault.abak.fileprovider",
                                    pair.data());
                            mContext.grantUriPermission(pair.pair().plugin().component().getPackageName(),
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            return uri;
                        })
                                .thenComposeAsync(uri -> {
                                            final IPluginService binder = IPluginService.Stub.asInterface(mConns.get(pair.pair().plugin()).binder);
                                            return CompletableFuture.supplyAsync(
                                                    // Call backup AIDL function
                                                    new ServiceCallable(mCallback,
                                                            new RestoreRequest(
                                                                    pair.pair().application(),
                                                                    uri
                                                            ),
                                                            pair.pair().plugin(), binder),
                                                    mPool);
                                        },
                                        mPool)
                                .thenAcceptAsync(uri -> {
                                    mContext.revokeUriPermission(uri, 0);
                                })
                                .handle((res, ex) -> {
                                    Log.d(TAG, "Handle");
                                    if (ex == null) {
                                        mCallback.sendProgress(pair.pair(), Task.PROG_COMPLETED);
                                        Log.i(TAG, pair.pair().taskName() + " succeed.");
                                    } else {
                                        mAtLeastOneError = true;
                                        mCallback.sendProgress(pair.pair(), Task.PROG_FAIL);
                                        Log.e(TAG, pair.pair().taskName() + " failed.", ex);
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

        deleteDirectory(mOptions.extracted());

        Log.d(TAG, "Disconnecting from services");
        StreamSupport.stream(mConns.values()).map(result -> result.connection).forEach(mContext::unbindService);
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
