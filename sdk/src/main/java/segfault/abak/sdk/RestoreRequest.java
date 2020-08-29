package segfault.abak.sdk;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

public class RestoreRequest implements Parcelable {
    @NonNull
    public final String application;

    @NonNull
    public final Uri data;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public RestoreRequest(@NonNull String application, @NonNull Uri data) {
        this.application = application;
        this.data = data;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected RestoreRequest(Parcel in) {
        application = in.readString();
        data = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<RestoreRequest> CREATOR = new Creator<RestoreRequest>() {
        @Override
        public RestoreRequest createFromParcel(Parcel in) {
            return new RestoreRequest(in);
        }

        @Override
        public RestoreRequest[] newArray(int size) {
            return new RestoreRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(application);
        parcel.writeParcelable(data, i);
    }
}
