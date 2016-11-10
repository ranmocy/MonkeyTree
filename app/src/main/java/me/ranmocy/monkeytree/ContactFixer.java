package me.ranmocy.monkeytree;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.Transliterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Util class that fix contacts.
 */
final class ContactFixer {

    private static final String TAG = "ContactFixer";

    private Context context;

    ContactFixer(Context context) {
        this.context = context;
    }

    Set<ContactLite> getLatinContactData() {
        return getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                int codePoint = contact.displayName.codePointAt(0);
                return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN;
            }
        }, new NullTransliterator(), ContactsContract.PhoneticNameStyle.UNDEFINED);
    }

    Set<ContactLite> getChineseContactData() {
        return getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                Character.UnicodeScript script = Character.UnicodeScript.of(contact.displayName.codePointAt(0));
                return script == Character.UnicodeScript.HAN;
            }
        }, Transliterator.getInstance("Han-Latin/Names"), ContactsContract.PhoneticNameStyle.PINYIN);
    }

    private Set<ContactLite> getContactData(
            ContactFilter contactFilter, Transliterator transliterator, int phoneticNameStyle) {
        Set<ContactLite> contacts = new HashSet<>();
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.Data._ID,
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME},
                String.format("(%s = ?) AND (%s NOT NULL)",
                        ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME),
                new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE} /*select args*/,
                null /*sort*/);
        try {
            if (cursor != null) {
                Log.i(TAG, "Contact data:" + cursor.getCount());
                while (cursor.moveToNext()) {
                    int dataId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Data._ID));
                    String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                    String givenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                    String middleName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
                    String familyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));

                    String phoneticGivenName = transliterate(transliterator, givenName);
                    String phoneticMiddleName = transliterate(transliterator, middleName);
                    String phoneticFamilyName = transliterate(transliterator, familyName);

                    Log.i(TAG, String.format("%s[%s|%s] : [%s|%s|%s]",
                            displayName, givenName, familyName, phoneticGivenName, phoneticMiddleName, phoneticFamilyName));

                    ContactLite contactLite = new ContactLite(
                            dataId, displayName, givenName, middleName, familyName,
                            phoneticGivenName, phoneticMiddleName, phoneticFamilyName, phoneticNameStyle);
                    if (contactFilter.shouldKeep(contactLite)) {
                        contacts.add(contactLite);
                    } else {
                        Log.v(TAG, "skip");
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.i(TAG, "Contact final data:" + contacts.size());
        return contacts;
    }

    void fixContactPhonetic(Set<ContactLite> contactIds) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (ContactLite contact : contactIds) {
            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                            String.format("%s = ?", ContactsContract.Data._ID),
                            new String[]{String.valueOf(contact.dataId)})
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, contact.phoneticGivenName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME, contact.phoneticMiddleName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, contact.phoneticFamilyName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME_STYLE, contact.phoneticNameStyle)
                    .build());
        }
        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error when updating", e);
        }
        Log.i(TAG, "Fix done:" + contactIds.size());
    }

    @Nullable
    private String transliterate(Transliterator transliterator, @Nullable String text) {
        if (text == null) {
            return null;
        }
        String result = transliterator.transliterate(text);
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result;
    }

    private interface ContactFilter {
        boolean shouldKeep(ContactLite contact);
    }

    /** NullTransliterator will change any string into empty string. */
    private static final class NullTransliterator extends Transliterator {

        NullTransliterator() {
            super("Any-Null", null /*filter*/);
        }

        @Override
        protected void handleTransliterate(Replaceable text, Position pos, boolean incremental) {
            text.replace(pos.start, pos.limit, "");
            pos.start = text.length();
            pos.limit = text.length();
        }
    }
}
