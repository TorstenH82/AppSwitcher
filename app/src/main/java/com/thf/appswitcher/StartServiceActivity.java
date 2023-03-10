package com.thf.AppSwitcher;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import com.thf.AppSwitcher.service.AppSwitcherService;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.SimpleDialog;
import com.thf.AppSwitcher.utils.Utils;
import java.util.List;
import com.thf.AppSwitcher.utils.Utils.SuCommandException;

public class StartServiceActivity extends Activity {
    private static final String TAG = "AppSwitcherService";
    private static Context context;
    private static Activity activity;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        context = getApplicationContext();
        activity = StartServiceActivity.this;

        if (AppSwitcherService.isRunning()) {
            finish();
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            new SimpleDialog(
                            "NO_OVERLAY",
                            activity,
                            simpleDialogCallbacks,
                            "No overlay permission",
                            "AppSwitcher needs permission to be shown on top of other apps",
                            false)
                    .show();
            return;
        }

        int checkVal = context.checkCallingOrSelfPermission(Manifest.permission.READ_LOGS);
        if (checkVal == PackageManager.PERMISSION_DENIED) {
            new SimpleDialog(
                            "NO_READ_LOGS",
                            activity,
                            simpleDialogCallbacks,
                            "No read logs permission",
                            "AppSwitcher needs permission to read Android logs.\n\nAppSwitcher can try to authorize itself.\nTry self authorization?",
                            true)
                    .show();
            return;
        }

        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        checkVal =
                appOps.checkOpNoThrow(
                        "android:get_usage_stats",
                        android.os.Process.myUid(),
                        context.getPackageName());

        // Toast.makeText(context, checkVal + "", Toast.LENGTH_LONG).show();

        boolean usageStatAccess = false;
        if (checkVal == AppOpsManager.MODE_ALLOWED) {
            usageStatAccess = true;
        } else if (checkVal == AppOpsManager.MODE_DEFAULT) {
            usageStatAccess =
                    (context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS)
                            == PackageManager.PERMISSION_GRANTED);
        }
        if (!usageStatAccess) {
            new SimpleDialog(
                            "NO_USAGE_STATISTICS",
                            activity,
                            simpleDialogCallbacks,
                            "No usage statistics permission",
                            "AppSwitcher needs permission to read usage statistics",
                            false)
                    .show();
            return;
        }

        startAppSwitcherService();
    }

    private void startAppSwitcherService() {
        List<AppData> selectedList =
                SharedPreferencesHelper.loadList(getApplicationContext(), "selected");

        if (selectedList.size() == 0) {
            new SimpleDialog(
                            "NO_APPS",
                            activity,
                            simpleDialogCallbacks,
                            "No apps selected",
                            "Please set relevant apps and activities to allow start of AppSwitcher Service",
                            false)
                    .show();
        } else {
            if (!AppSwitcherService.isRunning()) {
                Intent intent = new Intent(getBaseContext(), AppSwitcherService.class);
                startForegroundService(intent);
            }
        }
        finish();
    }

    private static SimpleDialog.SimpleDialogCallbacks simpleDialogCallbacks =
            new SimpleDialog.SimpleDialogCallbacks() {
                @Override
                public void onClick(boolean positive, String reference) {
                    if (reference == "NO_APPS") {
                        Intent intent = new Intent(context, SettingsActivity.class);
                        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } else if (reference == "NO_OVERLAY") {

                        Intent intent =
                                new Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + context.getPackageName()));
                        activity.startActivityForResult(intent, 5469);

                    } else if (reference == "NO_USAGE_STATISTICS") {
                        Intent intent =
                                new Intent(
                                        Settings.ACTION_USAGE_ACCESS_SETTINGS,
                                        Uri.parse("package:" + context.getPackageName()));
                        activity.startActivityForResult(intent, 5470);
                    } else if (reference == "NO_READ_LOGS") {
                        if (positive) {
                            try {
                                Utils.selfAuthorizeReadLogs(context);
                            } catch (SuCommandException e) {
                                new SimpleDialog(
                                                "",
                                                activity,
                                                null,
                                                "Error self authorization",
                                                "Error occured during self authorization:\n"
                                                        + e.getMessage()
                                                        + "\n\nYou can use adb to authorize AppSwitcher manually.",
                                                false)
                                        .show();
                                return;
                            }
                            int checkVal =
                                    context.checkCallingOrSelfPermission(
                                            Manifest.permission.READ_LOGS);

                            if (checkVal == PackageManager.PERMISSION_GRANTED) {
                                new SimpleDialog(
                                                "",
                                                activity,
                                                simpleDialogCallbacks,
                                                "Permission granted",
                                                "Please start AppSwitcher Service again",
                                                false)
                                        .show();
                                return;
                            }
                        }
                        new SimpleDialog(
                                        "",
                                        activity,
                                        null,
                                        "Missing permission",
                                        "You can use adb to authorize AppSwitcher manually.",
                                        false)
                                .show();
                    }
                }
            };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean granted = false;
        if (requestCode == 5469) {
            if (Settings.canDrawOverlays(this)) {
                granted = true;
            }
        } else if (requestCode == 5470) {
            int checkVal =
                    context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS);
            if (checkVal == PackageManager.PERMISSION_GRANTED) {
                granted = true;
            }
        }

        if (granted) {
            new SimpleDialog(
                            "",
                            activity,
                            simpleDialogCallbacks,
                            "Permission granted",
                            "Please start AppSwitcher Service again",
                            false)
                    .show();
        }
    }
}
