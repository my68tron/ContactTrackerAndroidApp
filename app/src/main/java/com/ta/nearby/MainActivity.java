package com.ta.nearby;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 101;
    private static final int INTENT_REQUEST_CODE_OPEN_SETTINGS = 201;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    SharedPreferences mSharedPreferences;
    Intent mForegroundServiceIntent;
    TextView mSyncTimestamp;
    TextView mContactsCount;

    /**
     * Returns true if the app was granted all the permissions. Otherwise, returns false.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(Constants.KEY_LAST_UPLOAD_COUNT)) {
                mContactsCount.setText(sharedPreferences.getInt(key, 0) + "");
            }
            if (key.equals(Constants.KEY_LAST_UPLOADED_AT)) {
                mSyncTimestamp.setText(sharedPreferences.getString(key, "- - - : : :"));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasPermissions(MainActivity.this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
        mForegroundServiceIntent = new Intent(MainActivity.this, NearbyScannerService.class);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mSyncTimestamp = findViewById(R.id.sync_timestamp_text_view);
        mContactsCount = findViewById(R.id.contacts_count_text_view);

        mSyncTimestamp.setText(mSharedPreferences.getString(Constants.KEY_LAST_UPLOADED_AT, "- - - : : :"));
        mContactsCount.setText(mSharedPreferences.getInt(Constants.KEY_LAST_UPLOAD_COUNT, 0) + "");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    /**
     * Handles user acceptance (or denial) of our permission request.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }
        boolean requestAgain = false;
        boolean showRationale = false;
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                requestAgain = true;
                for (String permission : permissions) {
                    showRationale |= shouldShowRequestPermissionRationale(permission);
                    if (showRationale) {
                        break;
                    }
                }
            }
        }
        if (requestAgain) {
            if (!showRationale) {
                Snackbar.make(findViewById(R.id.linear_layout), "App requires both Storage and Location Permissions!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("SETTINGS", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                startActivityForResult(intent, INTENT_REQUEST_CODE_OPEN_SETTINGS);
                            }
                        })
                        .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                        .show();
            } else {
                Snackbar.make(findViewById(R.id.linear_layout), "App requires both Storage and Location Permissions!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Permissions", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
                            }
                        })
                        .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                        .show();
            }
        }
    }

    public void startForegroundService(View view) {
        NearbyJobIntentService.enqueueWork(MainActivity.this, mForegroundServiceIntent);
    }

    public void stopForegroundService(View view) {
        stopService(mForegroundServiceIntent);
    }
}
