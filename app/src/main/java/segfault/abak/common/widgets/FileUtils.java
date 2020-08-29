package segfault.abak.common.widgets;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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

    @NonNull
    public static String allLines(@NonNull BufferedReader reader) throws IOException {
        final StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return builder.toString().trim();
    }

    @NonNull
    public static List<File> walk(@NonNull File file) {
        final List<File> lst = new ArrayList<>();

        File[] list = file.listFiles();
        if (list == null) return lst;

        for (File f : list) {
            if (f.isDirectory() ) {
                lst.addAll(walk(f));
            } else if (f.isFile()) {
                lst.add(f);
            }
        }
        return lst;
    }

    public static boolean deleteDirectory(@NonNull File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
