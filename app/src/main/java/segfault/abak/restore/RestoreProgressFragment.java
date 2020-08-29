package segfault.abak.restore;

import android.app.Dialog;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import segfault.abak.R;
import segfault.abak.common.AppUtils;
import segfault.abak.common.widgets.progress.ProgressFragment;
import segfault.abak.common.widgets.progress.Task;
import segfault.abak.sdkclient.Plugin;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static segfault.abak.restore.RestoreThread.MSG_KO;
import static segfault.abak.restore.RestoreThread.MSG_OK;

public class RestoreProgressFragment extends ProgressFragment implements Handler.Callback {
    private static final String TAG = "RestoreProgressFragment";

    static final String EXTRA_RESTORE_OPTIONS = RestoreProgressFragment.class.getName() +
            ".EXTRA_RESTORE_OPTIONS";

    static final int MSG_PROGRESS = 0;

    private HandlerThread mRestoreThread;
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
        mRestoreThread.quitSafely();
        super.onDestroy();
    }

    static String pluginToTask(@NonNull Plugin plugin, @NonNull String application) {
        return "plug_" + plugin.component().getPackageName() + ":" + plugin.component().getClassName() + "_" + application;
    }

    static void sendProgress(@NonNull Handler uiHandler, @NonNull String task, int progress) {
        uiHandler.sendMessage(uiHandler.obtainMessage(MSG_PROGRESS,
                progress,
                0,
                task));
    }

    static void sendProgress(@NonNull Handler uiHandler, @NonNull Plugin plugin, @NonNull String application, int progress) {
        sendProgress(uiHandler, pluginToTask(plugin, application), progress);
    }

    @Override
    public void run() {
        final List<Task> tasks = new ArrayList<>(mOptions.apps().size());
        for (final RestoreOptions.AppPluginPair pair : mOptions.apps()) {
            tasks.add(Task.create(pluginToTask(pair.plugin(), pair.application()), getString(R.string.backup_prog_task_plugin,
                    pair.plugin().loadTitle(requireContext()),
                    AppUtils.appName(pair.application(), requireContext()))));
        }
        addTasks(tasks);

        // Setup UI Handler
        mHandler = new Handler(Looper.getMainLooper(), this);
        mRestoreThread = new RestoreThread(mHandler, mOptions, requireContext());
        mRestoreThread.start();
    }
}
