package me.ranmocy.monkeytree;


import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.Transliterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


/**
 * Fragment contains the main layout.
 */
public class MainFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "MainFragment";

    public static MainFragment create() {
        return new MainFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        rootView.findViewById(R.id.btn_fix_latin).setOnClickListener(this);
        rootView.findViewById(R.id.btn_fix_chinese).setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.btn_fix_latin:
                    fixLatinContacts();
                    break;
                case R.id.btn_fix_chinese:
                    fixChineseContacts();
                    break;
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Transliterator data is missing");
            Toast.makeText(getActivity(), R.string.toast_data_missing_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void fixLatinContacts() {
        fixContactPhonetic(getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                int codePoint = contact.displayName.codePointAt(0);
                Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
                Log.i(TAG, String.format("Unicode:%s:%s:%s", contact.displayName, script, codePoint));
                return script == Character.UnicodeScript.LATIN;
            }
        }), new NullTransliterator(), ContactsContract.PhoneticNameStyle.UNDEFINED);
    }

    private void fixChineseContacts() {
        fixContactPhonetic(getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                Character.UnicodeScript script = Character.UnicodeScript.of(contact.displayName.codePointAt(0));
                return script == Character.UnicodeScript.HAN;
            }
        }), Transliterator.getInstance("Han-Latin/Names"), ContactsContract.PhoneticNameStyle.PINYIN);
    }

    private Set<ContactLite> getContactData(ContactFilter contactFilter) {
        Set<ContactLite> contacts = new HashSet<>();
        Cursor cursor = getContext().getContentResolver().query(
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
                    String phoneticGivenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME));
                    String phoneticFamilyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME));
                    Log.i(TAG, String.format("%s[%s|%s] : [%s|%s]",
                            displayName, givenName, familyName, phoneticGivenName, phoneticFamilyName));
                    ContactLite contactLite = new ContactLite(dataId, displayName, givenName, middleName, familyName);
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

    private void fixContactPhonetic(Set<ContactLite> contactIds, Transliterator transliterator, int phoneticNameStyle) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (ContactLite contact : contactIds) {
            String givenNamePinyin = transliterate(transliterator, contact.givenName);
            String middleNamePinyin = transliterate(transliterator, contact.middleName);
            String familyNamePinyin = transliterate(transliterator, contact.familyName);
            Log.i(TAG, String.format("Contact %s[%s|%s|%s] => [%s|%s|%s]",
                    contact.displayName, contact.givenName, contact.middleName, contact.familyName,
                    givenNamePinyin, middleNamePinyin, familyNamePinyin));

            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                            String.format("%s = ?", ContactsContract.Data._ID),
                            new String[]{String.valueOf(contact.dataId)})
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, givenNamePinyin)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, familyNamePinyin)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME_STYLE, phoneticNameStyle)
                    .build());
        }
        try {
            getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
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

    private static final class ContactLite {

        private final int dataId;
        private final String displayName;
        private final String givenName;
        private final String middleName;
        private final String familyName;

        private ContactLite(int dataId, String displayName, String givenName, String middleName, String familyName) {
            this.dataId = dataId;
            this.displayName = displayName;
            this.givenName = givenName;
            this.middleName = middleName;
            this.familyName = familyName;
        }
    }

    private interface ContactFilter {
        boolean shouldKeep(ContactLite contact);
    }

    private static final class NullTransliterator extends Transliterator {

        /**
         * NullTransliterator will change any string into empty string.
         * ICU 2.0
         */
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
