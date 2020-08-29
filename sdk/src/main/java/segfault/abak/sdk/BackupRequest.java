package segfault.abak.sdk;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * A simplified BackupOptions object which stores basic information to perform a backup.
 */
public class BackupRequest implements Parcelable {
    /**
     * Application ID to backup.
     */
    @NonNull
    public final String application;

    /**
     * User selected options
     */
    @Nullable
    public final Bundle options;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public BackupRequest(@NonNull String application,
                         @Nullable Bundle options) {
        this.application = application;
        this.options = options;
    }

    protected BackupRequest(Parcel in) {
        application = in.readString();
        options = in.readBundle(getClass().getClassLoader());
    }

    public static final Creator<BackupRequest> CREATOR = new Creator<BackupRequest>() {
        @Override
        public BackupRequest createFromParcel(Parcel in) {
            return new BackupRequest(in);
        }

        @Override
        public BackupRequest[] newArray(int size) {
            return new BackupRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(application);
        parcel.writeBundle(options);
    }
}
