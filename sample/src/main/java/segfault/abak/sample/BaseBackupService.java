package segfault.abak.sample;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.FileUtils;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import segfault.abak.sdk.BackupRequest;
import segfault.abak.sdk.BackupResponse;
import segfault.abak.sdk.PluginService;
import segfault.abak.sdk.RestoreRequest;

import java.io.*;

public class BaseBackupService extends PluginService {
    @NonNull
    @Override
    public BackupResponse backup(@NonNull BackupRequest request) throws Throwable {
        final File parent = new File(getCacheDir(), "temps");
        parent.mkdir();
        final File file = new File(parent, "file-" + request.application + "-" + getClass().getSimpleName());
        final OutputStream out = new FileOutputStream(file);
        for (int i = 0; i < 1000 * 10; i ++) {
            out.write(0);
            reportProgress(i * 100 / (1000 * 10));
        }
        out.close();
        final Uri uri = FileProvider.getUriForFile(this, "segfault.abak.sample.fileprovider", file);
        grantUriPermission("segfault.abak", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return new BackupResponse(uri);
    }

    @Override
    public void restore(@NonNull RestoreRequest request) throws Throwable {

    }
}
