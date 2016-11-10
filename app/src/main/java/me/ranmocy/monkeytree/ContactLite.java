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
    final String phoneticGivenName;
    final String phoneticMiddleName;
    final String phoneticFamilyName;
    final int phoneticNameStyle;

    ContactLite(int dataId,
                String displayName,
                String givenName,
                String middleName,
                String familyName,
                String phoneticGivenName,
                String phoneticMiddleName,
                String phoneticFamilyName,
                int phoneticNameStyle) {
        this.dataId = dataId;
        this.displayName = displayName;
        this.givenName = givenName;
        this.middleName = middleName;
        this.familyName = familyName;
        this.phoneticGivenName = phoneticGivenName;
        this.phoneticMiddleName = phoneticMiddleName;
        this.phoneticFamilyName = phoneticFamilyName;
        this.phoneticNameStyle = phoneticNameStyle;
    }

    private ContactLite(Parcel in) {
        this.dataId = in.readInt();
        this.displayName = in.readString();
        this.givenName = in.readString();
        this.middleName = in.readString();
        this.familyName = in.readString();
        this.phoneticGivenName = in.readString();
        this.phoneticMiddleName = in.readString();
        this.phoneticFamilyName = in.readString();
        this.phoneticNameStyle = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(dataId);
        parcel.writeString(displayName);
        parcel.writeString(givenName);
        parcel.writeString(middleName);
        parcel.writeString(familyName);
        parcel.writeString(phoneticGivenName);
        parcel.writeString(phoneticMiddleName);
        parcel.writeString(phoneticFamilyName);
        parcel.writeInt(phoneticNameStyle);
    }

    @Override
    public int describeContents() {
        return 0;
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
}
