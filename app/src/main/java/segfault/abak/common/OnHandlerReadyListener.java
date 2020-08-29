package segfault.abak.common;

import androidx.annotation.WorkerThread;

@FunctionalInterface
public interface OnHandlerReadyListener {
    @WorkerThread
    void onReady();
}
