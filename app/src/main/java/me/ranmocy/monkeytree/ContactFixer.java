package me.ranmocy.monkeytree;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Build;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.Transliterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Util class that fix contacts.
 */
final class ContactFixer {

    private static final String TAG = "ContactFixer";

    private static final Transliterator HAN_TO_PINYIN = Transliterator.getInstance("Han-Latin/Names");
    static final Transliterator PINYIN_TO_ASCII = Transliterator.getInstance("Latin-Ascii");

    private Context context;

    ContactFixer(Context context) {
        this.context = context;
    }

    Set<ContactLite> getLatinContactData() {
        return getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                int codePoint = contact.displayName.codePointAt(0);
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
        }, new NullTransliterator(), ContactsContract.PhoneticNameStyle.UNDEFINED);
    }

    Set<ContactLite> getChineseContactData() {
        return getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                int codePoint = contact.displayName.codePointAt(0);
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
        }, new ChineseNameTransliterator(), ContactsContract.PhoneticNameStyle.PINYIN);
    }

    Set<ContactLite> getAllContactData() {
        return getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                return true;
            }
        }, new NullTransliterator(), ContactsContract.PhoneticNameStyle.UNDEFINED);
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
                        ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME},
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

                    ContactLite contactLite = new ContactLite(
                            dataId, displayName, givenName, middleName, familyName,
                            phoneticGivenName, phoneticMiddleName, phoneticFamilyName, phoneticNameStyle);
                    if (contactFilter.shouldKeep(contactLite)) {
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

    private interface ContactFilter {
        boolean shouldKeep(ContactLite contact);
    }

    /**
     * NullTransliterator will change any string into empty string.
     */
    private static final class NullTransliterator extends Transliterator {

        private NullTransliterator() {
            super("Any-Null", null /*filter*/);
        }

        @Override
        protected void handleTransliterate(Replaceable text, Position pos, boolean incremental) {
            text.replace(pos.start, pos.limit, "");
            pos.start = text.length();
            pos.limit = text.length();
        }
    }

    /**
     * A {@link Transliterator} wrapper of "Han-Latin/Names" with enhancement of Chinese names.
     */
    @VisibleForTesting
    static final class ChineseNameTransliterator extends Transliterator {

        private static final Map<String, String> ENHANCEMENTS = getEnhancements();

        private static Map<String, String> getEnhancements() {
            HashMap<String, String> map = new HashMap<>();
            map.put("阚", "kàn");
            map.put("缪", "miào");
            map.put("朴", "piáo");
            map.put("么", "yāo");
            map.put("肖", "xiāo");
            return map;
        }

        ChineseNameTransliterator() {
            super("Han-Latin/NamesEnhanced", Transliterator.getInstance("Han-Latin/Names").getFilter());
        }

        @Override
        protected void handleTransliterate(Replaceable text, Position pos, boolean incremental) {
            String source = text.toString();
            String target = ENHANCEMENTS.containsKey(source)
                    ? ENHANCEMENTS.get(source)
                    : HAN_TO_PINYIN.transliterate(source);

            text.replace(pos.start, pos.limit, target);
            pos.start = text.length();
            pos.limit = text.length();
        }
    }
}
