package me.ranmocy.monkeytree;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Lite version of a contact name.
 */
final class ContactLite implements Parcelable{

    final int dataId;
    final String displayName;
    final String givenName;
    final String middleName;
    final String familyName;

    ContactLite(int dataId,
                String displayName,
                String givenName,
                String middleName,
                String familyName) {
        this.dataId = dataId;
        this.displayName = displayName;
        this.givenName = givenName;
        this.middleName = middleName;
        this.familyName = familyName;
    }

    private ContactLite(Parcel in) {
        this.dataId = in.readInt();
        this.displayName = in.readString();
        this.givenName = in.readString();
        this.middleName = in.readString();
        this.familyName = in.readString();
    }

    public static final Creator<ContactLite> CREATOR = new Creator<ContactLite>() {
        @Override
        public ContactLite createFromParcel(Parcel in) {
            return new ContactLite(in);
        }

        @Override
        public ContactLite[] newArray(int size) {
            return new ContactLite[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(dataId);
        parcel.writeString(displayName);
        parcel.writeString(givenName);
        parcel.writeString(middleName);
        parcel.writeString(familyName);
    }
}
