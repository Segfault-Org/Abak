package segfault.abak.restore.ui;

import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.auto.value.AutoValue;
import segfault.abak.common.backupformat.entries.Entry;

import java.io.File;
import java.util.ArrayList;

@AutoValue
abstract class RestoreParseResult implements Parcelable {
    @NonNull
    static RestoreParseResult create(@NonNull ArrayList<Entry> entries,
                                            @NonNull File extractedFile) {
        return new AutoValue_RestoreParseResult(entries, extractedFile);
    }

    @NonNull
    public abstract ArrayList<Entry> entries();

    @NonNull
    public abstract File extractedFile();
}
