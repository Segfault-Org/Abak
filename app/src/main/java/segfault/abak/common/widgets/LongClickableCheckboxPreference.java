package segfault.abak.common.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import moe.shizuku.preference.CheckBoxPreference;
import moe.shizuku.preference.PreferenceViewHolder;

public class LongClickableCheckboxPreference extends CheckBoxPreference {
    private final View.OnLongClickListener mListener;

    public LongClickableCheckboxPreference(Context context, AttributeSet attrs, int defStyleAttr, @Nullable View.OnLongClickListener listener) {
        super(context, attrs, defStyleAttr);
        this.mListener = listener;
    }

    public LongClickableCheckboxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, @Nullable View.OnLongClickListener listener) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mListener = listener;
    }

    public LongClickableCheckboxPreference(Context context, AttributeSet attrs, @Nullable View.OnLongClickListener listener) {
        super(context, attrs);
        this.mListener = listener;
    }

    public LongClickableCheckboxPreference(Context context, @Nullable View.OnLongClickListener listener) {
        super(context);
        this.mListener = listener;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setOnLongClickListener(mListener);
    }
}
