package segfault.abak.restore;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import segfault.abak.common.BindServiceSupplier;
import segfault.abak.common.backupformat.BackupDriver;
import segfault.abak.common.backupformat.entries.ApplicationEntryV1;
import segfault.abak.common.widgets.progress.Task;
import segfault.abak.sdk.BackupRequest;
import segfault.abak.sdk.BackupResponse;
import segfault.abak.sdk.IPluginService;
import segfault.abak.sdk.RestoreRequest;
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

public class RestoreThread extends HandlerThread {
    private static final String TAG = "RestoreThread";

    static final int MSG_OK = 1;
    static final int MSG_KO = 2;

    private final Handler mCallback;
    private final RestoreOptions mOptions;
    private final Context mContext;

    private final ExecutorService mPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private Set<ServiceConnection> mToBeDisconnected = new CopyOnWriteArraySet<>();

    private volatile boolean mAtLeastOneError = false;

    public RestoreThread(@NonNull Handler callback, @NonNull RestoreOptions options, @NonNull Context context) {
        super(TAG);
        mCallback = callback;
        mOptions = options;
        mContext = context;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        Log.d(TAG, "Starting in " + Thread.currentThread());
        try {
            // Wait for addTasks() to draw. This is a really bad design.
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        final List<Future<Void>> tasks = StreamSupport.stream(mOptions.apps())
                .map(pair -> {
                    // 1: Bind to service
                    return CompletableFuture.supplyAsync(new BindServiceSupplier(mContext,
                                    new Intent().setComponent(pair.plugin().component()), Context.BIND_AUTO_CREATE),
                            mPool)
                            .thenApply(result -> {
                                if (!result.success) {
                                    throw new IllegalStateException("Cannot bind to service " + pair.plugin().component().flattenToShortString());
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
                            }, mPool)
                            .thenApplyAsync(binder -> {
                                final Uri uri = FileProvider.getUriForFile(mContext,
                                        "segfault.abak.fileprovider",
                                        pair.data());
                                mContext.grantUriPermission(pair.plugin().component().getPackageName(),
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                return new Pair<>(binder, uri);
                            })
                            .thenComposeAsync(pair1 -> {
                                        return CompletableFuture.supplyAsync(
                                                // Call backup AIDL function
                                                new ServiceCallable(mCallback,
                                                        new RestoreRequest(
                                                                pair.application(),
                                                                pair1.second
                                                        ),
                                                        pair.plugin(), pair1.first),
                                                mPool);
                                    },
                                    mPool)
                            .thenAcceptAsync(uri -> {
                                mContext.revokeUriPermission(uri, 0);
                            })
                            .handle((res, ex) -> {
                                                    Log.d(TAG, "Handle");
                                                    if (ex == null) {
                                                        RestoreProgressFragment.sendProgress(mCallback,
                                                                pair.plugin(), pair.application(),
                                                                Task.PROG_COMPLETED);
                                                        Log.i(TAG, RestoreProgressFragment.pluginToTask(pair.plugin(), pair.application()) + " succeed.");
                                                    } else {
                                                        mAtLeastOneError = true;
                                                        RestoreProgressFragment.sendProgress(mCallback,
                                                                pair.plugin(), pair.application(),
                                                                Task.PROG_FAIL);
                                                        Log.e(TAG, RestoreProgressFragment.pluginToTask(pair.plugin(), pair.application()) + " failed.", ex);
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

        deleteDirectory(mOptions.extracted());

        Log.d(TAG, "Disconnecting from services");
        StreamSupport.stream(mToBeDisconnected).forEach(mContext::unbindService);
        mCallback.sendMessage(mCallback.obtainMessage(mAtLeastOneError ? MSG_KO : MSG_OK));
    }

    private static boolean deleteDirectory(@NonNull File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
