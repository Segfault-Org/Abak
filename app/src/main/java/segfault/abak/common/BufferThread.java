package segfault.abak.common;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import segfault.abak.common.OnHandlerReadyListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Buffers the data received and pumps out at an fixed interval.
 * @param <T> Item type.
 */
public class BufferThread<T> extends HandlerThread implements Handler.Callback {
    private static final String TAG = "BufferThread";

    private static final long ROUTINE_INTERVAL = 200;

    private final List<T> mItems = new ArrayList<>(50);
    private final Callback<T> mCallback;
    private final OnHandlerReadyListener readyListener;

    private Handler mHandler;

    private static final int MSG_ROUTINE = 1;
    private static final int MSG_INSERT = 2;

    public BufferThread(@NonNull Callback<T> callback, @NonNull OnHandlerReadyListener readyListener) {
        super(TAG);
        this.mCallback = callback;
        this.readyListener = readyListener;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new Handler(Looper.myLooper(), this);
        readyListener.onReady();
        scheduleRoutine();
    }

    public boolean insert(@NonNull T item) {
        if (mHandler == null) return false;
        return mHandler.sendMessage(mHandler.obtainMessage(MSG_INSERT, item));
    }

    public boolean handlerReady() {
        return mHandler != null;
    }

    private void scheduleRoutine() {
        mHandler.sendEmptyMessageDelayed(MSG_ROUTINE, ROUTINE_INTERVAL);
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        switch (message.what) {
            case MSG_ROUTINE:
                if (!mItems.isEmpty()) {
                    final List<T> copyList = new ArrayList<>(mItems.size()); // To avoid ConcurrentModificationException
                    copyList.addAll(mItems);
                    mCallback.onBatchUpdate(copyList);
                    mItems.clear();
                }
                scheduleRoutine();
                return true;
            case MSG_INSERT:
                mItems.add((T) message.obj);
                return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface Callback<T> {
        @WorkerThread
        void onBatchUpdate(@NonNull List<T> items);
    }
}
