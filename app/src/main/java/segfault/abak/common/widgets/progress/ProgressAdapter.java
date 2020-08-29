package segfault.abak.common.widgets.progress;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import segfault.abak.R;

import java.lang.ref.WeakReference;
import java.util.List;

class ProgressAdapter extends ListAdapter<Task, ProgressAdapter.ViewHolder> {
    private static final String TAG = "ProgressAdapter";

    ProgressAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final ViewHolder VH = new ViewHolder(inflater.inflate(R.layout.item_progress, parent, false));
        return VH;
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressAdapter.ViewHolder holder, int position) {
        final Context context = holder.itemView.getContext();

        final Task task = getCurrentList().get(position);
        final String status;
        switch (task.percent()) {
            case Task.PROG_COMPLETED:
                status = "OK";
                break;
            case Task.PROG_INDETERMINATE:
                status = "...";
                break;
            case Task.PROG_FAIL:
                status = "Fail";
                break;
            case Task.PROG_SKIPPED:
                status = "Skip";
                break;
            default:
                status = String.format("%02d%%%n", task.percent());
                break;
        }
        holder.mTextView.setText(context.getString(R.string.progress_status_item, status, task.title()));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.item_progress_status);
        }
    }

    public static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK
            = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.id().equals(newItem.id());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.percent() == newItem.percent();
        }
    };
}
