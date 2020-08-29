package segfault.abak.restore.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import segfault.abak.R;
import segfault.abak.common.AppUtils;
import segfault.abak.common.widgets.progress.ProgressFragment;
import segfault.abak.common.widgets.progress.Task;
import segfault.abak.restore.IRestoreThread;
import segfault.abak.restore.RestoreOptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RestoreProgressFragment extends ProgressFragment implements Handler.Callback, IRestoreThread.Callback {
    private static final String TAG = "RestoreProgressFragment";

    static final String EXTRA_RESTORE_OPTIONS = RestoreProgressFragment.class.getName() +
            ".EXTRA_RESTORE_OPTIONS";

    private static final int MSG_PROGRESS = 0;
    private static final int MSG_OK = 1;
    private static final int MSG_KO = 2;

    private IRestoreThread mRestoreThread;
    private Handler mHandler;

    private RestoreOptions mOptions;

    private final Queue<Message> mSuppressedMessages = new LinkedList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mOptions = getArguments().getParcelable(EXTRA_RESTORE_OPTIONS);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCancelable(false);
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        // Suppress if we are not in the front.
        if (!isResumed()) {
            if (message.what == MSG_PROGRESS &&
            message.arg1 >= -1) {
                // Do not cache progress messages. Only cache the final ones.
                return false;
            }
            Log.d(TAG, "Suppressing message");
            mSuppressedMessages.add(Message.obtain(message));
            return false;
        }
        final boolean result = doHandleMsg(message);
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume the suppression
        while (true) {
            final Message msg = mSuppressedMessages.poll();
            if (msg == null) {
                Log.d(TAG, "We had gone through all messages.");
                break;
            }
            doHandleMsg(msg);
            msg.recycle();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.restore_prof_title);
        return dialog;
    }

    private boolean doHandleMsg(@NonNull Message message) {
        if (message.what == MSG_PROGRESS) {
            final String id = (String) message.obj;
            setProgress(id, message.arg1);
            return true;
        } else if (message.what == MSG_OK) {
            setCancelable(true);
            Toast.makeText(requireContext(), getString(R.string.restore_prof_ok), Toast.LENGTH_LONG).show();
            getDialog().setTitle(R.string.restore_prof_ok_title);
            return true;
        } else if (message.what == MSG_KO) {
            setCancelable(true);
            Toast.makeText(requireContext(), getString(R.string.restore_prof_ko), Toast.LENGTH_LONG).show();
            getDialog().setTitle(R.string.restore_prof_ko_title);
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mRestoreThread.stopThread();
        super.onDestroy();
    }

    @Override
    public void run() {
        final List<Task> tasks = new ArrayList<>(mOptions.apps().size());
        for (final RestoreOptions.DataAppPluginPair pair : mOptions.apps()) {
            Log.d(TAG, pair.pair().taskName());
            tasks.add(Task.create(pair.pair().taskName(), getString(R.string.backup_prog_task_plugin,
                    pair.pair().plugin().loadTitle(requireContext()),
                    AppUtils.appName(pair.pair().application(), requireContext()))));
        }
        addTasks(tasks);

        // Setup UI Handler
        mHandler = new Handler(Looper.getMainLooper(), this);
        mRestoreThread = IRestoreThread.create(this, mOptions, requireContext());
        mRestoreThread.startThread();
    }

    @Override
    @WorkerThread
    public void sendProgress(@NonNull String task, int progress) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS,
                progress,
                0,
                task));
    }

    @Override
    @WorkerThread
    public void sendResult(boolean success) {
        mHandler.sendEmptyMessage(success ? MSG_OK : MSG_KO);
    }
}
