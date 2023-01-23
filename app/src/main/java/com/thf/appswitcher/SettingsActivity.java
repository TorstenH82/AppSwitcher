package com.thf.AppSwitcher;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
// import android.content.pm.PackageManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
// import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.SimpleDialog;
import com.thf.AppSwitcher.utils.Utils;
import com.thf.AppSwitcher.utils.Utils.SuCommandException;
import com.thf.AppSwitcher.utils.Utils.SysPropException;
import com.thf.AppSwitcher.utils.Utils.FileReadModException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SettingsActivity extends AppCompatActivity {
    // private static final String TAG = "AppSwitcherService";

    private static Context context;
    private static Activity activity;
    private static PrefFragment settingsFragment;
    private static Utils.UtilCallbacks utilCallbacksEnableAutomateSrv =
            new Utils.UtilCallbacks() {
                @Override
                public void onException(Throwable e) {
                    new SimpleDialog(
                                    "",
                                    activity,
                                    null,
                                    "Error enabling Automate Service",
                                    "Error occured while enabling Automate Service: "
                                            + e.getMessage(),
                                    false)
                            .show();
                    ((SwitchPreference) settingsFragment.findPreference("enableAutomateSrv"))
                            .setChecked(false);
                }
            };

    private static SimpleDialog.SimpleDialogCallbacks simpleDialogCallbacksSelfAuth =
            new SimpleDialog.SimpleDialogCallbacks() {
                @Override
                public void onClick(boolean positive, String reference) {
                    boolean checked = false;

                    if (positive) {
                        try {
                            Utils.selfAuthorizeSecureSettings(context);
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
                        }
                        int checkVal =
                                context.checkCallingOrSelfPermission(
                                        context.getString(R.string.permissionSecureSettings));
                        if (checkVal == PackageManager.PERMISSION_GRANTED) {
                            new SimpleDialog(
                                            "",
                                            activity,
                                            null,
                                            "Permission granted",
                                            "Self authorization was succesful",
                                            false)
                                    .show();
                        }
                    } else {
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

    public static class PrefFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen()
                    .getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);

            List<AppData> selectedList = SharedPreferencesHelper.loadList(context, "selected");
            int iNaviCount =
                    selectedList.stream()
                            .filter(appData -> "navi".equals(appData.getList()))
                            .collect(Collectors.toList())
                            .size();
            int iActCount =
                    selectedList.stream()
                            .filter(appData -> "media".equals(appData.getList()))
                            .collect(Collectors.toList())
                            .size();

            // int iNaviCount = SharedPreferencesHelper.getCountOfDict(context, "my_dict" + "navi");
            findPreference("intentNavis")
                    .setSummary(Integer.toString(iNaviCount) + " navigation apps selected");

            // int iActCount = SharedPreferencesHelper.getCountOfDict(context, "my_dict" +
            // "activities");
            findPreference("intentActivities")
                    .setSummary(Integer.toString(iActCount) + " apps/activities selected");
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen()
                    .getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            PreferenceManager preferenceManager = getPreferenceManager();
            preferenceManager.setSharedPreferencesName("USERDATA");

            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);

            String currentValue = ((EditTextPreference) findPreference("logTag")).getText();
            ((EditTextPreference) findPreference("logTag")).setSummary(currentValue);

            currentValue = ((EditTextPreference) findPreference("logOnPress")).getText();
            ((EditTextPreference) findPreference("logOnPress")).setSummary(currentValue);

            currentValue = ((EditTextPreference) findPreference("logShortPress")).getText();
            ((EditTextPreference) findPreference("logShortPress")).setSummary(currentValue);

            currentValue = ((EditTextPreference) findPreference("logLongPress")).getText();
            ((EditTextPreference) findPreference("logLongPress")).setSummary(currentValue);

            SeekBarPreference dialogDelay = findPreference("dialogDelay");
            dialogDelay.setMin(500);
            dialogDelay.setUpdatesContinuously(true);
            int delay = dialogDelay.getValue();
            dialogDelay.setSummary(
                    String.format("%.1f", Float.intBitsToFloat(delay) / Float.intBitsToFloat(1000))
                            + "s");

            SwitchPreference runMediaApp = findPreference("runMediaApp");

            SeekBarPreference runMediaAppDelay = findPreference("runMediaAppDelay");
            runMediaAppDelay.setEnabled(runMediaApp.isChecked());
            runMediaAppDelay.setSelectable(runMediaApp.isChecked());
            runMediaAppDelay.setMin(0);
            runMediaAppDelay.setUpdatesContinuously(true);
            int delayRunMediaApp = runMediaAppDelay.getValue();
            runMediaAppDelay.setSummary(
                    String.format(
                                    "%.0f",
                                    Float.intBitsToFloat(delayRunMediaApp)
                                            / Float.intBitsToFloat(1000))
                            + "s");

            SwitchPreference runMediaAppTwice = findPreference("runMediaAppTwice");
            runMediaAppTwice.setEnabled(runMediaApp.isChecked());
            runMediaAppTwice.setSelectable(runMediaApp.isChecked());

            SwitchPreference smartList = findPreference("smartList");
            findPreference("intentSort").setEnabled(!smartList.isChecked());
            findPreference("intentSort").setSelectable(!smartList.isChecked());

            SeekBarPreference brightness = findPreference("itemsBrightness");
            brightness.setMin(10);
            brightness.setUpdatesContinuously(true);
            brightness.setSummary(brightness.getValue() + "%");
            // brightness.setShowSeekBarValue(true);

            SeekBarPreference dimScreen = findPreference("dimScreen");
            dimScreen.setMin(20);
            dimScreen.setUpdatesContinuously(true);
            dimScreen.setSummary(dimScreen.getValue() + "%");

            SwitchPreference duraspeed = findPreference("duraspeed");
            prepDuraspeed(duraspeed);

            SwitchPreference fullscreen = findPreference("fullscreen");
            prepFullscreen(fullscreen);

            SwitchPreference darkmode = findPreference("darkmode");
            darkmode.setChecked(Utils.getDarkMode(context) != 1);

            boolean automateOn =
                    ((SwitchPreference) findPreference("enableAutomateSrv")).isChecked();
            EditTextPreference automateFlow = findPreference("automateFlow");
            automateFlow.setEnabled(automateOn);
            automateFlow.setSelectable(automateOn);
            currentValue = automateFlow.getText();
            if (!context.getString(R.string.pref_automateFlow).equals(currentValue)
                    && !"".equals(currentValue)) {
                automateFlow.setSummary(currentValue);
            } else {
                automateFlow.setSummary("");
            }
            try {
                PackageInfo pInfo =
                        context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                findPreference("prefAbout")
                        .setSummary(
                                "version "
                                        + pInfo.versionName
                                        + "\n\nManufacturer: "
                                        + android.os.Build.MANUFACTURER
                                        + "\nProduct: "
                                        + android.os.Build.PRODUCT
                                        + "\nDevice: "
                                        + android.os.Build.DEVICE
                                        + "\nBoard: "
                                        + android.os.Build.BOARD);
            } catch (Exception ignore) {
            }
        }

        private void prepDuraspeed(SwitchPreference duraspeed) {
            try {
                Boolean isDuraspeedEnabled = Utils.isDuraspeedEnabled(context);
                if (isDuraspeedEnabled == null) {
                    // duraspeed.setEnabled(true);
                    // duraspeed.setSelectable(true);
                    duraspeed.setChecked(false);
                    duraspeed.setSummary("DuraSpeed system property not available");
                } else if (isDuraspeedEnabled) {
                    duraspeed.setEnabled(true);
                    duraspeed.setSelectable(true);
                    duraspeed.setChecked(true);
                    duraspeed.setSummary("DuraSpeed is enabled");
                } else {
                    duraspeed.setEnabled(true);
                    duraspeed.setSelectable(true);
                    duraspeed.setChecked(false);
                    duraspeed.setSummary("DuraSpeed is disabled");
                }
            } catch (SysPropException e) {
                // Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                duraspeed.setChecked(false);
                duraspeed.setSummary("Error reading DuraSpeed system property");
            }
        }

        private void prepFullscreen(SwitchPreference fullscreen) {
            /*
            if (!"825X_Pro".equals(android.os.Build.DEVICE)) {
                fullscreen.setSummary("Can only be changed on 825X_Pro devices");
                return;
            }
            */
            try {
                String fullscreenValue = Utils.getSetFullscreenFlag(context, null);
                switch (fullscreenValue) {
                    case "0":
                        fullscreen.setSummary("Fullscreen is disabled");
                        fullscreen.setChecked(false);
                        break;
                    default:
                        fullscreen.setSummary("Fullscreen is enabled (system default)");
                        fullscreen.setChecked(true);
                }
                fullscreen.setEnabled(true);
                fullscreen.setSelectable(true);
                    fullscreen.setSummary(fullscreen.getSummary()+"\nRequires reboot");
            } catch (FileReadModException e) {
                // Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                fullscreen.setChecked(false);
                fullscreen.setSummary("Can't read fullscreen system property");
            }
        }

        String ignoreKey = "";

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (ignoreKey.equals(key)) {
                ignoreKey = "";
                return;
            }
            ignoreKey = "";

            if (key.equals("logOnPress")
                    || key.equals("logShortPress")
                    || key.equals("logLongPress")
                    || key.equals("logTag")) {
                EditTextPreference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(sharedPreferences.getString(key, ""));
            } else if (key.equals("itemsBrightness")) {
                SeekBarPreference brightness = findPreference(key);
                brightness.setSummary(brightness.getValue() + "%");
                int progress = ((int) Math.round(brightness.getValue() / 10)) * 10;
                brightness.setValue(progress);
            } else if (key.equals("smartList")) {
                SwitchPreference smartList = findPreference(key);
                findPreference("intentSort").setEnabled(!smartList.isChecked());
                findPreference("intentSort").setSelectable(!smartList.isChecked());
            } else if (key.equals("dialogDelay")) {
                SeekBarPreference dialogDelay = findPreference(key);
                int delay = dialogDelay.getValue();
                dialogDelay.setSummary(
                        String.format(
                                        "%.1f",
                                        Float.intBitsToFloat(delay) / Float.intBitsToFloat(1000))
                                + "s");
                int progress = ((int) Math.round(dialogDelay.getValue() / 500)) * 500;
                dialogDelay.setValue(progress);
            } else if (key.equals("dimScreen")) {
                SeekBarPreference dimScreen = findPreference(key);
                dimScreen.setSummary(dimScreen.getValue() + "%");
                int progress = ((int) Math.round(dimScreen.getValue() / 5)) * 5;
                dimScreen.setValue(progress);
            } else if (key.equals("runMediaApp")) {
                SwitchPreference runMediaApp = findPreference(key);
                findPreference("runMediaAppDelay").setEnabled(runMediaApp.isChecked());
                findPreference("runMediaAppDelay").setSelectable(runMediaApp.isChecked());
                findPreference("runMediaAppTwice").setEnabled(runMediaApp.isChecked());
                findPreference("runMediaAppTwice").setSelectable(runMediaApp.isChecked());
            } else if (key.equals("runMediaAppDelay")) {
                SeekBarPreference runMediaAppDelay = findPreference(key);
                int delay = runMediaAppDelay.getValue();
                runMediaAppDelay.setSummary(
                        String.format(
                                        "%.0f",
                                        Float.intBitsToFloat(delay) / Float.intBitsToFloat(1000))
                                + "s");
                int progress = ((int) Math.round(runMediaAppDelay.getValue() / 1000)) * 1000;
                runMediaAppDelay.setValue(progress);
            } else if (key.equals("duraspeed")) {
                SwitchPreference duraspeed = findPreference(key);
                try {
                    Utils.enableDisableDuraspeed(context, duraspeed.isChecked());
                } catch (SysPropException e) {
                    new SimpleDialog(
                                    "",
                                    activity,
                                    null,
                                    "Error changing duraspeed preferences",
                                    e.getMessage(),
                                    false)
                            .show();
                }
                prepDuraspeed(duraspeed);
            } else if (key.equals("fullscreen")) {
                SwitchPreference fullscreen = findPreference(key);
                try {
                    String value = fullscreen.isChecked() ? "1" : "0";
                    Utils.getSetFullscreenFlag(context, value);
                    prepFullscreen(fullscreen);
                } catch (FileReadModException e) {
                    new SimpleDialog(
                                    "",
                                    activity,
                                    null,
                                    "Error changing fullscreen preferences",
                                    e.getMessage(),
                                    false)
                            .show();
                }
            } else if (key.equals("darkmode")) {
                SwitchPreference darkmode = findPreference(key);
                int checkVal =
                        context.checkCallingOrSelfPermission(
                                context.getString(R.string.permissionSecureSettings));
                if (checkVal == PackageManager.PERMISSION_GRANTED) {

                    Utils.setDarkMode(context, darkmode.isChecked());
                } else if (checkVal == PackageManager.PERMISSION_DENIED) {
                    ignoreKey = key;
                    darkmode.setChecked(!darkmode.isChecked());
                    new SimpleDialog(
                                    key,
                                    activity,
                                    simpleDialogCallbacksSelfAuth,
                                    "Missing permission",
                                    "This option requires permission to modify Android Settings.\nAppSwitcher can try to authorize itself.\nTry self authorization?",
                                    true)
                            .show();
                }
            } else if (key.equals("automateFlow")) {
                String currentValue = ((EditTextPreference) findPreference(key)).getText().trim();
                if (context.getString(R.string.pref_automateFlow).equals(currentValue)
                        || "".equals(currentValue)) {
                    ((EditTextPreference) findPreference(key)).setSummary("");
                    ((EditTextPreference) findPreference(key))
                            .setText(context.getString(R.string.pref_automateFlow));
                } else {
                    ((EditTextPreference) findPreference(key)).setSummary(currentValue);
                }
            } else if (key.equals("enableAutomateSrv")) {
                SwitchPreference enableAutomateSrv = findPreference(key);
                EditTextPreference automateFlow = findPreference("automateFlow");
                if (enableAutomateSrv.isChecked()) {
                    int checkVal =
                            context.checkCallingOrSelfPermission(
                                    context.getString(R.string.permissionSecureSettings));
                    if (checkVal == PackageManager.PERMISSION_GRANTED) {
                        Utils.enableService(
                                context,
                                context.getString(R.string.automatePackage),
                                context.getString(R.string.automateService),
                                utilCallbacksEnableAutomateSrv);
                        automateFlow.setEnabled(true);
                        automateFlow.setSelectable(true);
                    } else if (checkVal == PackageManager.PERMISSION_DENIED) {
                        ignoreKey = key;
                        enableAutomateSrv.setChecked(!enableAutomateSrv.isChecked());
                        new SimpleDialog(
                                        key,
                                        activity,
                                        simpleDialogCallbacksSelfAuth,
                                        "Missing permission",
                                        "This option requires permission to modify Android Settings.\nAppSwitcher can try to authorize itself.\nTry self authorization?",
                                        true)
                                .show();
                    }
                } else {
                    automateFlow.setEnabled(false);
                    automateFlow.setSelectable(false);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        activity = SettingsActivity.this;
        setContentView(R.layout.activity_settings);

        // If you want to insert data in your settings
        settingsFragment = new PrefFragment();
        // settingsFragment. ...
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frmSettings, settingsFragment)
                .commit();
    }
}
