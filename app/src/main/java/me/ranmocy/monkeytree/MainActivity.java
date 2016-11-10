package me.ranmocy.monkeytree;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class MainActivity extends AppCompatActivity
        implements InstructionFragment.OnActionSelected, ContactSelectFragment.OnContactsConfirmed {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST = 0;
    private static final List<String> REQUIRED_PERMISSIONS = Arrays.asList(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS);

    private ContactFixer contactFixer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contactFixer = new ContactFixer(this);


        if (checkPermission()) {
            initUI();
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
        Log.i(TAG, "requestPermissions");
        ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS.toArray(new String[REQUIRED_PERMISSIONS.size()]),
                PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PERMISSIONS_REQUEST == requestCode) {
            if (grantResults.length > 0 && allResultGranted(grantResults)) {
                initUI();
            } else {
                showDialogToExplainPermission();
            }
        }
    }

    private boolean allResultGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                return false;
            }
        }
        return true;
    }

    private void showDialogToExplainPermission() {
        Log.i(TAG, "Showing rationale dialog");
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

    private void initUI() {
        Log.i(TAG, "ALl permissions are granted. Init UI.");
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, InstructionFragment.create())
                .commit();
    }

    @Override
    public void onActionSelected(Action action) {
        Set<ContactLite> contacts;
        switch (action) {
            case FIX_LATIN_CONTACTS:
                contacts = contactFixer.getLatinContactData();
                break;
            case FIX_CHINESE_CONTACTS:
                contacts = contactFixer.getChineseContactData();
                break;
            default:
                throw new RuntimeException("Unknown action:" + action);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, ContactSelectFragment.create(action, contacts))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onContactsConfirmed(Action action, Set<ContactLite> contacts) {
        Log.i(TAG, String.format("Confirmed to fix %d contacts", contacts.size()));
        switch (action) {
            case FIX_LATIN_CONTACTS:
                contactFixer.fixLatinContacts(contacts);
                Toast.makeText(this, R.string.latin_fixed, Toast.LENGTH_SHORT).show();
                break;
            case FIX_CHINESE_CONTACTS:
                contactFixer.fixChineseContacts(contacts);
                Toast.makeText(this, R.string.chinese_fixed, Toast.LENGTH_SHORT).show();
                break;
            default:
                throw new RuntimeException("Unknown action:" + action);
        }
        while (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        }
    }
}
