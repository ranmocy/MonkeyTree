package me.ranmocy.monkeytree;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Android test for {@link MonkeyActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class MonkeyActivityTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
    );

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("me.ranmocy.monkeytree", appContext.getPackageName());
    }

    @Test
    public void createContacts() throws RemoteException, OperationApplicationException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        ContactLite[] contacts = new ContactLite[]{
                new ContactLite("Display", "Alex", "Bob", "Cindy"),
                new ContactLite("中文", "三", "一", "张"),
                new ContactLite("中文2", "四", "一", "张"),
                new ContactLite("中文3", "五", "一", "孙"),
                new ContactLite("Mixed", "Eric", "Li", "李"),
        };

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (ContactLite contact : contacts) {
            ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_TYPE, null)
                    .withValue(RawContacts.ACCOUNT_NAME, null)
                    .build());
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.GIVEN_NAME, contact.givenName)
                    .withValue(StructuredName.MIDDLE_NAME, contact.middleName)
                    .withValue(StructuredName.FAMILY_NAME, contact.familyName)
                    .withValue(StructuredName.DISPLAY_NAME, contact.displayName)
                    .build());
        }

        ContentProviderResult[] results = appContext.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        assertEquals(contacts.length *2, results.length);
        ContentProviderResult result1 = results[0];
        assertTrue(result1.uri.toString().startsWith(RawContacts.CONTENT_URI.toString()));
        assertNull(result1.exception);
        ContentProviderResult result2 = results[1];
        assertTrue(result2.uri.toString().startsWith(Data.CONTENT_URI.toString()));
        assertNull(result2.exception);
    }
}
