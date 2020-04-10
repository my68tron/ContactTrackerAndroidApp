package com.ta.nearby;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    SharedPreferences mSharedPreferences;
    AlertDialog.Builder mAlertDialogBuilder;
    AlertDialog mAlertDialog;
    EditText mFullNameEditText;
    String mFullName;
    EditText mMobNoEditText;
    String mMobNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SplashActivity.this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mSharedPreferences.getBoolean(Constants.KEY_IS_INFO_SET, false)) {
                    createDialogSplashName();
                    mAlertDialog.show();
                } else {
                    initialise();
                }
            }
        }, 500);
    }

    private void initialise() {
        mFullName = mSharedPreferences.getString(Constants.KEY_FULL_NAME, "");
        Toast.makeText(SplashActivity.this, "Welcome, " + mFullName, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void createDialogSplashName() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View dialogSplashNameView = layoutInflater.inflate(R.layout.dialog_splash_name, null);
        mAlertDialogBuilder = new AlertDialog.Builder(SplashActivity.this);
        mAlertDialogBuilder.setView(dialogSplashNameView);

        mFullNameEditText = dialogSplashNameView.findViewById(R.id.full_name_edit_text);
        mMobNoEditText = dialogSplashNameView.findViewById(R.id.mob_no_edit_text);

        mAlertDialog = mAlertDialogBuilder
                .setCancelable(false)
                .setTitle("User Details")
                .setPositiveButton("Save",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result edit text
                                mFullName = mFullNameEditText.getText().toString().trim();
                                mMobNo = mMobNoEditText.getText().toString().trim();
                                if (mFullName.isEmpty()) {
                                    mFullNameEditText.setText("");
                                    Toast.makeText(SplashActivity.this, "Invalid Name", Toast.LENGTH_SHORT).show();
                                    mAlertDialog.cancel();
                                } else if (mMobNo.isEmpty() || mMobNo.length() != 10) {
                                    Toast.makeText(SplashActivity.this, "Please Enter 10 digit mob no", Toast.LENGTH_SHORT).show();
                                    mAlertDialog.cancel();
                                } else {
                                    if (mFullName.split(" ").length < 2) {
                                        Toast.makeText(SplashActivity.this, "Enter Full Name", Toast.LENGTH_SHORT).show();
                                        mAlertDialog.cancel();
                                    } else {
                                        String[] nameWords = mFullName.split(" ");
                                        mFullName = "";
                                        for (String word : nameWords) {
                                            if (!word.trim().isEmpty()) {
                                                mFullName += word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase() + " ";
                                            }
                                        }
                                        mFullName = mFullName.trim();
                                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                                        editor.putBoolean(Constants.KEY_IS_INFO_SET, true);
                                        editor.putString(Constants.KEY_FULL_NAME, mFullName);
                                        editor.putString(Constants.KEY_MOB_NO, mMobNo);
                                        editor.apply();

                                        initialise();
                                    }
                                }
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mAlertDialog.show();
                    }
                })
                .create();
    }
}
