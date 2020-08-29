package segfault.abak.backup;

import android.app.Dialog;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import segfault.abak.R;
import segfault.abak.common.AppUtils;
import segfault.abak.common.widgets.FileUtils;
import segfault.abak.common.widgets.progress.ProgressFragment;
import segfault.abak.common.widgets.progress.Task;
import segfault.abak.sdkclient.Plugin;

import java.util.*;

public class BackupProgressFragment extends ProgressFragment implements Handler.Callback {
    private static final String TAG = "BackupProgressFragment";

    static final String EXTRA_OPTIONS = BackupProgressFragment.class.getName() +
            ".EXTRA_BACKUP_OPTIONS";

    static final int MSG_PROGRESS = 0;

    static final int MSG_PROGRESS_SRC_HANDLE = 2;
    static final int MSG_PROGRESS_SRC_PLUGIN = 1;
    static final int MSG_PROGRESS_SRC_PACKAGE = 3;

    private HandlerThread mBackupThread;
    private Handler mHandler;

    private BackupOptions mOptions;

    private final Queue<Message> mSuppressedMessages = new LinkedList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mOptions = getArguments().getParcelable(EXTRA_OPTIONS);
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
        dialog.setTitle(R.string.backup_prof_title);
        return dialog;
    }

    private boolean doHandleMsg(@NonNull Message message) {
        if (message.what == MSG_PROGRESS) {
            final String id = (String) message.obj;
            setProgress(id, message.arg1);
            return true;
        } else if (message.what == BackupThread.MSG_OK) {
            setCancelable(true);
            Toast.makeText(requireContext(), getString(R.string.backup_prof_ok,
                    FileUtils.sizeOf(mOptions.location(), requireContext())), Toast.LENGTH_LONG).show();
            getDialog().setTitle(R.string.backup_prof_ok_title);
            return true;
        } else if (message.what == BackupThread.MSG_KO) {
            setCancelable(true);
            Toast.makeText(requireContext(), getString(R.string.backup_prof_ko), Toast.LENGTH_LONG).show();
            getDialog().setTitle(R.string.backup_prof_ko_title);
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mBackupThread.quitSafely();
        super.onDestroy();
    }

    static String pluginToTask(@NonNull Plugin plugin, @NonNull String application) {
        return "plug_" + plugin.component().getPackageName() + ":" + plugin.component().getClassName() + "_" + application;
    }

    static void sendProgress(@NonNull Handler uiHandler, @NonNull String task, int progress, int from) {
        // Log.d(TAG, "sendProgress " + task + ":" + progress + ":" + from);
        uiHandler.sendMessage(uiHandler.obtainMessage(MSG_PROGRESS,
                progress,
                0,
                task));
    }

    static void sendProgress(@NonNull Handler uiHandler, @NonNull Plugin plugin, @NonNull String application, int progress, int from) {
        sendProgress(uiHandler, pluginToTask(plugin, application), progress, from);
    }

    @Override
    public void run() {
        // addTask(Task.create("backup_info", getString(R.string.backup_prog_backup_info)));
        // addTask(Task.create("disable", getString(R.string.backup_prog_disable)));
        final List<Task> tasks = new ArrayList<>(mOptions.plugins().size() * mOptions.application().size() + 1);
        for (final Plugin plugin : mOptions.plugins()) {
            for (final String application : mOptions.application()) {
                tasks.add(Task.create(pluginToTask(plugin, application), getString(R.string.backup_prog_task_plugin,
                        plugin.loadTitle(requireContext()),
                        AppUtils.appName(application, requireContext()))));
            }
        }
        // addTask(Task.create("enable", getString(R.string.backup_prog_enable)));
        tasks.add(Task.create("package", getString(R.string.backup_prog_package)));
        addTasks(tasks);

        // Setup UI Handler
        mHandler = new Handler(Looper.getMainLooper(), this);
        mBackupThread = new BackupThread(mHandler, mOptions, requireContext());
        mBackupThread.start();
    }
}
