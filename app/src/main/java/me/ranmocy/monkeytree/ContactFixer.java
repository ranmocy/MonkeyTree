package me.ranmocy.monkeytree;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Build;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ibm.icu.text.Transliterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Util class that fix contacts.
 */
final class ContactFixer {

    private static final String TAG = "ContactFixer";

    private final Context context;

    ContactFixer(Context context) {
        this.context = context;
    }

    Set<ContactLite> getAllContactsToUpdate() {
        return getContactData(new ContactTransistor() {
            @Nullable
            @Override
            public ContactLite translate(
                    int dataId,
                    String displayName, String givenName, String middleName, String familyName,
                    String phoneticName, String phoneticGivenName, String phoneticMiddleName, String phoneticFamilyName) {

                Transliterator transliterator;
                int style;
                if (isLatin(displayName)) {
                    transliterator = Transliterators.ANY_TO_NULL;
                    style = ContactsContract.PhoneticNameStyle.UNDEFINED;
                } else if (isChinese(displayName)) {
                    transliterator = Transliterators.HAN_TO_PINYIN_NAME;
                    style = ContactsContract.PhoneticNameStyle.PINYIN;
                } else {
                    Log.w(TAG, "Unknown contact type:" + displayName);
                    return null;
                }

                String newPhoneticGivenName = transliterate(transliterator, givenName);
                String newPhoneticMiddleName = transliterate(transliterator, middleName);
                String newPhoneticFamilyName = transliterate(transliterator, familyName);

                // if nothing changed, do not display
                if (equalName(phoneticGivenName, newPhoneticGivenName)
                        && equalName(phoneticMiddleName, newPhoneticMiddleName)
                        && equalName(phoneticFamilyName, newPhoneticFamilyName)) {
                    return null;
                }
                Log.d(TAG, String.format("diff: %s[%s|%s|%s] => [%s|%s|%s]",
                        phoneticName, phoneticGivenName, phoneticMiddleName, phoneticFamilyName,
                        newPhoneticGivenName, newPhoneticMiddleName, newPhoneticFamilyName));
                return new ContactLite(
                        dataId,
                        displayName, givenName, middleName, familyName,
                        newPhoneticGivenName, newPhoneticMiddleName, newPhoneticFamilyName,
                        style);
            }

            private boolean equalName(String a, String b) {
                return ((a == null) && (b == null)) || TextUtils.equals(a, b);
            }

            private boolean isLatin(String displayName) {
                int codePoint = displayName.codePointAt(0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN;
                } else {
                    Character.UnicodeBlock ub = Character.UnicodeBlock.of(codePoint);
                    return ub == Character.UnicodeBlock.BASIC_LATIN
                            || ub == Character.UnicodeBlock.LATIN_EXTENDED_A
                            || ub == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL
                            || ub == Character.UnicodeBlock.LATIN_EXTENDED_B
                            || ub == Character.UnicodeBlock.LATIN_EXTENDED_C
                            || ub == Character.UnicodeBlock.LATIN_EXTENDED_D
                            || ub == Character.UnicodeBlock.LATIN_1_SUPPLEMENT;
                }
            }

            private boolean isChinese(String displayName) {
                int codePoint = displayName.codePointAt(0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
                } else {
                    Character.UnicodeBlock ub = Character.UnicodeBlock.of(codePoint);
                    return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
                            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
                            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
                }
            }
        });
    }

    /**
     * Get all contacts whose phonetic data will be removed.
     */
    Set<ContactLite> getAllContactsToClear() {
        return getContactData(new ContactTransistor() {
            @Nullable
            @Override
            public ContactLite translate(
                    int dataId,
                    String displayName, String givenName, String middleName, String familyName,
                    String phoneticName, String phoneticGivenName, String phoneticMiddleName, String phoneticFamilyName) {
                return new ContactLite(
                        dataId,
                        displayName, givenName, middleName, familyName,
                        null, null, null,
                        ContactsContract.PhoneticNameStyle.UNDEFINED);
            }
        });
    }

    private Set<ContactLite> getContactData(ContactTransistor contactTransliterator) {
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
                        ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME,
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
                    String phoneticName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME));
                    String phoneticGivenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME));
                    String phoneticMiddleName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME));
                    String phoneticFamilyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME));

                    ContactLite contactLite = contactTransliterator.translate(dataId,
                            displayName, givenName, middleName, familyName,
                            phoneticName, phoneticGivenName, phoneticMiddleName, phoneticFamilyName);
                    if (contactLite != null) {
                        contacts.add(contactLite);
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

    private interface ContactTransistor {
        @Nullable
        ContactLite translate(
                int dataId,
                String displayName, String givenName, String middleName, String familyName,
                String phoneticName, String phoneticGivenName, String phoneticMiddleName, String phoneticFamilyName);
    }
}
