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

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MainFragment.OnActionSelected {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST = 0;
    private static final List<String> REQUIRED_PERMISSIONS = Arrays.asList(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                .replace(android.R.id.content, MainFragment.create())
                .commit();
    }

    @Override
    public void onActionSelected() {
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, ContactSelectFragment.create())
                .addToBackStack()
                .commit();
    }
}
