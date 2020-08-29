package segfault.abak.common.widgets.progress;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.*;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import segfault.abak.R;
import segfault.abak.common.OnHandlerReadyListener;
import segfault.abak.common.BufferThread;

import java.util.*;

/**
 * A dialog fragment maintaining a list of tasks.
 * This dialog allows rapid updates of tasks. The updates will be buffered and collected so the UI thread won't be blocked.
 */
public abstract class ProgressFragment extends DialogFragment implements BufferThread.Callback<Task>,
        OnHandlerReadyListener {
    private static final String TAG = "ProgressFragment";
    private static final String STATE_TASKS = ProgressFragment.class.getName() +
            ".STATE_TASKS";

    private boolean mAlreadyAttemptRun = false;

    /**
     * This list should only be accessed through UI thread.
     */
    private HashMap<String, Task> mTasks = new LinkedHashMap<>(5);

    private ProgressAdapter mAdapter;

    private BufferThread<Task> mBuffer;

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mBuffer = new BufferThread<>(this, this);
        mBuffer.start();
        if (savedInstanceState != null) {
            if (savedInstanceState.get(STATE_TASKS) != null)
                mTasks = (HashMap<String, Task>) savedInstanceState.getSerializable(STATE_TASKS);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_TASKS, mTasks);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = view.findViewById(R.id.dialog_fragment_recycler);
        mAdapter = new ProgressAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(mAdapter);
        super.onViewCreated(view, savedInstanceState);
        attemptRun();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Get a task according to id
     * @param id ID of the task. See {@link Task#id()}
     * @return Task
     * @throws NoSuchElementException If there is no such task.
     */
    @NonNull
    protected Task getTask(@NonNull String id) throws NoSuchElementException {
        final Task task = mTasks.get(id);
        if (task == null) throw new NoSuchElementException();
        return task;
    }

    protected void addTasks(@NonNull List<Task> tasks) throws IllegalArgumentException {
        for (final Task t : tasks) {
            if (mTasks.containsKey(t.id()))
                throw new IllegalArgumentException(String.format("Task with id %1$s already exists.", t.id()));
            mTasks.put(t.id(), t);
        }

        if (mAdapter != null) {
            mAdapter.submitList(new ArrayList<>(mTasks.values()));
        } else {
            Log.w(TAG, "addTasks adapter is not available");
        }
    }

    @UiThread
    protected void setProgress(@NonNull String id, @IntRange(from = -4, to = 100) int progress) throws NoSuchElementException {
        final Task task = getTask(id);
        final Task newTask = Task.create(id, task.title(), progress);
        mBuffer.insert(newTask);
    }

    @Override
    @WorkerThread
    public void onBatchUpdate(@NonNull List<Task> tasks) {
        Log.d(TAG, "Batch update received: " + tasks.size());

        requireActivity().runOnUiThread(() -> {
            for (final Task t : tasks) {
                mTasks.put(t.id(), t);
            }
            if (mAdapter != null)
                mAdapter.submitList(new ArrayList<>(mTasks.values()));
        });
    }

    @Override
    public void onReady() {
        requireActivity().runOnUiThread(this::attemptRun);
    }

    @Override
    public void onDestroy() {
        mBuffer.quit();
        super.onDestroy();
    }

    public abstract void run();

    private void attemptRun() {
        if (mAlreadyAttemptRun) return;
        if (mBuffer == null || !mBuffer.handlerReady()) return;
        if (mAdapter == null) return;
        mAlreadyAttemptRun = true;
        run();
    }
}
