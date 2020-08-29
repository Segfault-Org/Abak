package segfault.abak.restore;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import segfault.abak.common.backupformat.BackupLayout;
import segfault.abak.common.backupformat.InvalidFormatException;
import segfault.abak.common.backupformat.RestoreDriver;
import segfault.abak.common.backupformat.manifest.ManifestFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

public class ParsePkgThread implements Runnable {
    private static final String TAG = "ParsePkgThread";

    private final Uri mPkg;
    private final Context mContext;

    private final Callback mCallback;

    public ParsePkgThread(@NonNull Uri mPkg, @NonNull Callback callback, @NonNull Context context) {
        this.mPkg = mPkg;
        this.mCallback = callback;
        this.mContext = context;
    }

    @Override
    public void run() {
        try {
            final File parent = new File(mContext.getCacheDir(), "extracts");
            parent.mkdir();
            final File extract = new File(parent, "extract-" + UUID.randomUUID().toString());
            Log.d(TAG, "Extracting to " + extract.getAbsolutePath());
            extract.mkdir();

            final InputStream pkgInput = mContext.getContentResolver().openInputStream(mPkg);
            RestoreDriver.extract(pkgInput, extract);
            pkgInput.close();

            final ManifestFile manifest = RestoreDriver.parseManifest(RestoreDriver.findManifest(extract));
            final BackupLayout layout = RestoreDriver.parseLayout(extract, manifest);

            final RestoreParseResult parseResult = RestoreParseResult.create(
                    new ArrayList<>(layout.entries()),
                    extract
            );
            mCallback.success(parseResult);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            mCallback.fail(e);
        } catch (InvalidFormatException e) {
            Log.e(TAG, "Invalid format", e);
            mCallback.fail(e);
        }
    }

    public interface Callback {
        @WorkerThread
        void success(@NonNull RestoreParseResult options);
        @WorkerThread
        void fail(@NonNull Throwable e);
    }
}
