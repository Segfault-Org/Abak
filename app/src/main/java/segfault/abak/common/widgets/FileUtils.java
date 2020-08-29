package segfault.abak.common.widgets;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static int sizeOf(@NonNull Uri uri, @NonNull Context context) {
        try (Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    return cursor.getInt(sizeIndex);
                } else {
                    return -1;
                }
            }
        }
        return -1;
    }

    public static void copy(@NonNull InputStream in, @NonNull OutputStream out) {
        try {
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) > 0){
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static String allLines(@NonNull BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
        }
        return line;
    }
}
