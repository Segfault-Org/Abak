package segfault.abak.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java9.util.function.Supplier;

public class BindServiceSupplier implements Supplier<BindServiceSupplier.Result> {
    private static final String TAG = "BindServiceSupplier";

    private final int flags;
    private final Intent intent;
    private final Context mContext;
    private Handler mHandler;
    private final CountDownLatch mLatch;

    public BindServiceSupplier(@NonNull Context context, @NonNull Intent intent2, int flags2) {
        this.mContext = context;
        this.intent = intent2;
        this.flags = flags2;

        mLatch = new CountDownLatch(1);
    }

    public Result get() {
        Log.d(TAG, "GET " + Thread.currentThread().getName() + ", " + mHandler);
        boolean success;
        BinderConn conn = new BinderConn() {
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                super.onServiceConnected(componentName, iBinder);
                mLatch.countDown();
            }
        };
        success = this.mContext.bindService(this.intent, conn, this.flags);
        try {
            mLatch.await();
        } catch (InterruptedException ignored) {
        }
        return new Result(success, conn.mBinder, conn.componentName, conn);
    }

    public class Result {
        public final IBinder binder;
        public final ComponentName componentName;
        public final ServiceConnection connection;
        public final boolean success;

        public Result(boolean success2, IBinder binder2, ComponentName componentName2, ServiceConnection connection2) {
            this.success = success2;
            this.binder = binder2;
            this.componentName = componentName2;
            this.connection = connection2;
        }
    }

    private static class BinderConn implements ServiceConnection {
        private ComponentName componentName;
        private IBinder mBinder;

        private BinderConn() {
        }

        public void onServiceConnected(ComponentName componentName2, IBinder iBinder) {
            this.mBinder = iBinder;
            this.componentName = componentName2;
            Log.d("BindServiceSupplier", "Connected in " + Thread.currentThread());
        }

        public void onServiceDisconnected(ComponentName componentName2) {
        }
    }
}