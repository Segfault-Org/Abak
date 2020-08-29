package segfault.abak.sdk;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * This entity stores all information of the backup file, after completing.
 */
public class BackupResponse implements Parcelable {
    /**
     * The packaged data. It will be copied to the final package.
     */
    @NonNull
    public final Uri data;

    public BackupResponse(@NonNull Uri data) {
        this.data = data;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected BackupResponse(Parcel in) {
        data = in.readParcelable(BackupResponse.class.getClassLoader());
    }

    public static final Creator<BackupResponse> CREATOR = new Creator<BackupResponse>() {
        @Override
        public BackupResponse createFromParcel(Parcel in) {
            return new BackupResponse(in);
        }

        @Override
        public BackupResponse[] newArray(int size) {
            return new BackupResponse[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(data, i);
    }
}
