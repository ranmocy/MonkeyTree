package me.ranmocy.monkeytree;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneticNameStyle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.Transliterator;

import java.lang.Character.UnicodeScript;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST = 0;
    private static final List<String> REQUIRED_PERMISSIONS = Arrays.asList(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkPermission()) {
            initUi();
        } else {
            if (shouldShowPermissionRationale()) {
                showDialogToExplainPermission();
            } else {
                requestPermissions();
            }
        }
    }

    private boolean checkPermission() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldShowPermissionRationale() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS.toArray(new String[REQUIRED_PERMISSIONS.size()]),
                PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PERMISSIONS_REQUEST == requestCode) {
            if (grantResults.length < permissions.length) {
                showDialogToExplainPermission();
            } else {
                initUi();
            }
        }
    }

    private void showDialogToExplainPermission() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.dialog_permission_message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        requestPermissions();
                    }
                })
                .setNegativeButton(R.string.btn_quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .show();
    }

    private void initUi() {
        setContentView(R.layout.activity_main);
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
            Toast.makeText(this, "Transliterator data is missing, can not process", Toast.LENGTH_SHORT).show();
        }
    }

    private void fixLatinContacts() {
        fixContactPhonetic(getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                int codePoint = contact.displayName.codePointAt(0);
                UnicodeScript script = UnicodeScript.of(codePoint);
                Log.i(TAG, String.format("Unicode:%s:%s:%s", contact.displayName, script, codePoint));
                return script == UnicodeScript.LATIN;
            }
        }), new NullTransliterator(), PhoneticNameStyle.UNDEFINED);
    }

    private void fixChineseContacts() {
        fixContactPhonetic(getContactData(new ContactFilter() {
            @Override
            public boolean shouldKeep(ContactLite contact) {
                UnicodeScript script = UnicodeScript.of(contact.displayName.codePointAt(0));
                return script == UnicodeScript.HAN;
            }
        }), Transliterator.getInstance("Han-Latin/Names"), PhoneticNameStyle.PINYIN);
    }

    private Set<ContactLite> getContactData(ContactFilter contactFilter) {
        Set<ContactLite> contacts = new HashSet<>();
        Cursor cursor = getContentResolver().query(Data.CONTENT_URI,
                new String[]{
                        Data._ID,
                        Data.MIMETYPE,
                        StructuredName.DISPLAY_NAME,
                        StructuredName.GIVEN_NAME,
                        StructuredName.MIDDLE_NAME,
                        StructuredName.FAMILY_NAME,
                        StructuredName.PHONETIC_GIVEN_NAME,
                        StructuredName.PHONETIC_FAMILY_NAME},
                String.format("(%s = ?) AND (%s NOT NULL)",
                        Data.MIMETYPE, StructuredName.DISPLAY_NAME),
                new String[]{StructuredName.CONTENT_ITEM_TYPE} /*select args*/,
                null /*sort*/);
        try {
            if (cursor != null) {
                Log.i(TAG, "Contact data:" + cursor.getCount());
                while (cursor.moveToNext()) {
                    int dataId = cursor.getInt(cursor.getColumnIndex(Data._ID));
                    String displayName = cursor.getString(cursor.getColumnIndex(StructuredName.DISPLAY_NAME));
                    String givenName = cursor.getString(cursor.getColumnIndex(StructuredName.GIVEN_NAME));
                    String middleName = cursor.getString(cursor.getColumnIndex(StructuredName.MIDDLE_NAME));
                    String familyName = cursor.getString(cursor.getColumnIndex(StructuredName.FAMILY_NAME));
                    String phoneticGivenName = cursor.getString(cursor.getColumnIndex(StructuredName.PHONETIC_GIVEN_NAME));
                    String phoneticFamilyName = cursor.getString(cursor.getColumnIndex(StructuredName.PHONETIC_FAMILY_NAME));
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

            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                    .withSelection(
                            String.format("%s = ?", Data._ID),
                            new String[]{String.valueOf(contact.dataId)})
                    .withValue(StructuredName.PHONETIC_GIVEN_NAME, givenNamePinyin)
                    .withValue(StructuredName.PHONETIC_FAMILY_NAME, familyNamePinyin)
                    .withValue(StructuredName.PHONETIC_NAME_STYLE, phoneticNameStyle)
                    .build());
        }
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
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
