package segfault.abak.common.widgets.progress;

import android.os.Parcelable;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;

/**
 * The {@link ProgressFragment} displays a series of Tasks, which each of them represents a step, and contains their own progresses.
 *
 * Note: The task here is only for displaying. Applications should implement their own action processing mechanisms.
 */
@AutoValue
public abstract class Task implements Parcelable {
    public static final int PROG_INDETERMINATE = -1;
    public static final int PROG_COMPLETED = -2;
    public static final int PROG_FAIL = -3;
    public static final int PROG_SKIPPED = -4;

    @NonNull
    public static Task create(@NonNull String id, @NonNull CharSequence title) {
        return create(id, title, 0);
    }

    @NonNull
    public static Task create(@NonNull String id, @NonNull CharSequence title, @IntRange(from = -4, to = 100) int percent) {
        return new AutoValue_Task(id, title, percent);
    }

    @NonNull
    public abstract String id();

    @NonNull
    public abstract CharSequence title();

    /**
     * -1 for indeterminate
     * -2 for completed (OK)
     * -3 for fail (KO)
     */
    @IntRange(from = -4, to = 100)
    public abstract int percent();

    /*
    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Task)) return false;
        final Task that = (Task) obj;
        return this.id().equals(that.id());
    }
    */

    public final boolean completed() {
        return percent() == 100 || percent() == -2;
    }

    public final boolean indeterminate() {
        return percent() == -1;
    }
}
