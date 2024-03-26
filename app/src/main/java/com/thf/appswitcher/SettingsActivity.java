package com.thf.AppSwitcher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import com.thf.AppSwitcher.utils.AppDataIcon;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.SimpleDialog;
import com.thf.AppSwitcher.utils.Utils;
import com.thf.AppSwitcher.utils.Utils.SuCommandException;
import com.thf.AppSwitcher.utils.Utils.SysPropException;
import com.thf.AppSwitcher.utils.Utils.FileReadModException;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsActivity extends AppCompatActivity {
  private static AppSwitcherApp mApplication;
  private static Context context;
  private static SharedPreferencesHelper sharedPreferencesHelper;
  private static Activity activity;
  private static PrefFragment settingsFragment;
  private static String screen;
  private static Utils.UtilCallbacks utilCallbacksEnableAutomateSrv =
      new Utils.UtilCallbacks() {
        @Override
        public void onException(Throwable e) {
          new SimpleDialog(
                  "",
                  activity,
                  null,
                  "Error enabling Automate Service",
                  "Error occured while enabling Automate Service: " + e.getMessage(),
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
                context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
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
      getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

      if (screen != null && "apps_activities".equals(screen)) {
        // List<AppData> selectedList = sharedPreferencesHelper.loadList("selected");
        List<AppDataIcon> selectedList = sharedPreferencesHelper.getSelected(true);
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

        // int iNaviCount = SharedPreferencesHelper.getCountOfDict(context, "my_dict" +
        // "navi");

        if (findPreference("intentNavis") != null)
          findPreference("intentNavis")
              .setSummary(Integer.toString(iNaviCount) + " navigation apps selected");

        // int iActCount = SharedPreferencesHelper.getCountOfDict(context, "my_dict" +
        // "activities");
        if (findPreference("intentActivities") != null)
          findPreference("intentActivities")
              .setSummary(Integer.toString(iActCount) + " apps/activities selected");
      }
    }

    @Override
    public void onPause() {
      super.onPause();
      getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

      PreferenceManager preferenceManager = getPreferenceManager();
      preferenceManager.setSharedPreferencesName("USERDATA");

      // Load the preferences from an XML resource

      if (screen == null) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
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

      } else if ("apps_activities".equals(screen)) {
        setPreferencesFromResource(R.xml.pref_apps_activities, rootKey);
        SwitchPreference smartList = findPreference("smartList");
        findPreference("intentSort").setEnabled(!smartList.isChecked());
        findPreference("intentSort").setSelectable(!smartList.isChecked());
        findPreference("addLauncher")
            .setSummary(
                "Add '" + mApplication.getLauncher().getName() + "'");

        SwitchPreference addLauncher = findPreference("addLauncher");
        findPreference("genericHome").setEnabled(addLauncher.isChecked());
        findPreference("genericHome").setSelectable(addLauncher.isChecked());

      } else if ("log_listener".equals(screen)) {
        setPreferencesFromResource(R.xml.pref_log_listener, rootKey);

        SwitchPreference enableLogListener = findPreference("enableLogListener");

        findPreference("logTag")
            .setSummary(((EditTextPreference) findPreference("logTag")).getText());
        findPreference("logTag").setEnabled(enableLogListener.isChecked());
        findPreference("logTag").setSelectable(enableLogListener.isChecked());
        findPreference("logOnPress")
            .setSummary(((EditTextPreference) findPreference("logOnPress")).getText());
        findPreference("logOnPress").setEnabled(enableLogListener.isChecked());
        findPreference("logOnPress").setSelectable(enableLogListener.isChecked());
        findPreference("logShortPress")
            .setSummary(((EditTextPreference) findPreference("logShortPress")).getText());
        findPreference("logShortPress").setEnabled(enableLogListener.isChecked());
        findPreference("logShortPress").setSelectable(enableLogListener.isChecked());
        findPreference("logLongPress")
            .setSummary(((EditTextPreference) findPreference("logLongPress")).getText());
        findPreference("logLongPress").setEnabled(enableLogListener.isChecked());
        findPreference("logLongPress").setSelectable(enableLogListener.isChecked());

      } else if ("dialog".equals(screen)) {
        setPreferencesFromResource(R.xml.pref_dialog, rootKey);

        SeekBarPreference dialogDelay = findPreference("dialogDelay");
        dialogDelay.setMin(500);
        dialogDelay.setUpdatesContinuously(true);
        int delay = dialogDelay.getValue();
        dialogDelay.setSummary(
            String.format("%.1f", Float.intBitsToFloat(delay) / Float.intBitsToFloat(1000)) + "s");

        SeekBarPreference brightness = findPreference("itemsBrightness");
        brightness.setMin(10);
        brightness.setUpdatesContinuously(true);
        brightness.setSummary(brightness.getValue() + "%");

      } else if ("screen".equals(screen)) {
        setPreferencesFromResource(R.xml.pref_screen, rootKey);
        SeekBarPreference dimScreen = findPreference("dimScreen");
        dimScreen.setMin(20);
        dimScreen.setUpdatesContinuously(true);
        dimScreen.setSummary(dimScreen.getValue() + "%");

        SwitchPreference darkmode = findPreference("darkmode");
        darkmode.setChecked(Utils.getDarkMode(context) != 1);

        SwitchPreference fullscreen = findPreference("fullscreen");
        if (!"825X_Pro".equals(android.os.Build.DEVICE)) {
          fullscreen.setEnabled(false);
          fullscreen.setSelectable(false);
          fullscreen.setSummary("This is not a 825X_Pro device");
        } else {
          prepFullscreen(fullscreen);
        }

      } else if ("start".equals(screen)) {
        setPreferencesFromResource(R.xml.pref_start, rootKey);
        SwitchPreference runMediaApp = findPreference("runMediaApp");
        SeekBarPreference runMediaAppDelay = findPreference("runMediaAppDelay");
        runMediaAppDelay.setEnabled(runMediaApp.isChecked());
        runMediaAppDelay.setSelectable(runMediaApp.isChecked());
        runMediaAppDelay.setMin(0);
        runMediaAppDelay.setUpdatesContinuously(true);
        int delayRunMediaApp = runMediaAppDelay.getValue();
        runMediaAppDelay.setSummary(
            String.format(
                    "%.0f", Float.intBitsToFloat(delayRunMediaApp) / Float.intBitsToFloat(1000))
                + "s");

        SwitchPreference runMediaAppTwice = findPreference("runMediaAppTwice");
        runMediaAppTwice.setEnabled(runMediaApp.isChecked());
        runMediaAppTwice.setSelectable(runMediaApp.isChecked());

        boolean automateOn = ((SwitchPreference) findPreference("enableAutomateSrv")).isChecked();
        EditTextPreference automateFlow = findPreference("automateFlow");
        automateFlow.setEnabled(automateOn);
        automateFlow.setSelectable(automateOn);
        String currentValue = automateFlow.getText();
        if (!context.getString(R.string.pref_automateFlow).equals(currentValue)
            && !"".equals(currentValue)) {
          automateFlow.setSummary(currentValue);
        } else {
          automateFlow.setSummary("");
        }

        /*} else if ("automate".equals(screen)) {
          setPreferencesFromResource(R.xml.pref_automate, rootKey);
          boolean automateOn = ((SwitchPreference) findPreference("enableAutomateSrv")).isChecked();
          EditTextPreference automateFlow = findPreference("automateFlow");
          automateFlow.setEnabled(automateOn);
          automateFlow.setSelectable(automateOn);
          String currentValue = automateFlow.getText();
          if (!context.getString(R.string.pref_automateFlow).equals(currentValue)
              && !"".equals(currentValue)) {
            automateFlow.setSummary(currentValue);
          } else {
            automateFlow.setSummary("");
          }

          return;
        */
      } else if ("others".equals(screen)) {
        setPreferencesFromResource(R.xml.pref_others, rootKey);
        SwitchPreference duraspeed = findPreference("duraspeed");
        prepDuraspeed(duraspeed);
        /*
        Preference qbproperty = findPreference("qbproperty");
        try {
          String propertyValue = Utils.getSystemProperty("sys.qb.startapp_onresume");
          if (propertyValue == null || "".equals(propertyValue)) {
            qbproperty.setSummary("<empty>");
          } else {
            qbproperty.setSummary(propertyValue);
          }
        } catch (Utils.SysPropException ex) {
          qbproperty.setSummary("Can't read property");
        }
        */

      }
    }

    private void prepDuraspeed(SwitchPreference duraspeed) {
      try {
        Boolean isDuraspeedEnabled = Utils.isDuraspeedEnabled(context);
        if (isDuraspeedEnabled == null) {
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
        duraspeed.setChecked(false);
        duraspeed.setSummary("Error reading DuraSpeed system property");
      }
    }

    private void prepFullscreen(SwitchPreference fullscreen) {
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
        fullscreen.setSummary(fullscreen.getSummary() + "\nRequires reboot");
      } catch (FileReadModException e) {
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

      Preference preference;
      SeekBarPreference seekBarPreference;
      SwitchPreference switchPreference;
      EditTextPreference editTextPreference;

      int progress = 0;

      switch (key) {
        case "logOnPress":
        case "logShortPress":
        case "logLongPress":
        case "logTag":
          preference = findPreference(key);
          preference.setSummary(sharedPreferences.getString(key, ""));
          break;

        case "itemsBrightness":
          seekBarPreference = findPreference(key);
          seekBarPreference.setSummary(seekBarPreference.getValue() + "%");
          progress = (Math.round(seekBarPreference.getValue() / 10)) * 10;
          seekBarPreference.setValue(progress);
          break;

        case "smartList":
          switchPreference = findPreference(key);
          findPreference("intentSort").setEnabled(!switchPreference.isChecked());
          findPreference("intentSort").setSelectable(!switchPreference.isChecked());
          break;

        case "addLauncher":
          switchPreference = findPreference(key);
          findPreference("genericHome").setEnabled(switchPreference.isChecked());
          findPreference("genericHome").setSelectable(switchPreference.isChecked());
          break;

        case "dialogDelay":
          seekBarPreference = findPreference(key);
          int delay = seekBarPreference.getValue();
          seekBarPreference.setSummary(
              String.format("%.1f", Float.intBitsToFloat(delay) / Float.intBitsToFloat(1000))
                  + "s");
          progress = (Math.round(seekBarPreference.getValue() / 500)) * 500;
          seekBarPreference.setValue(progress);
          break;

        case "dimScreen":
          seekBarPreference = findPreference(key);
          seekBarPreference.setSummary(seekBarPreference.getValue() + "%");
          progress = (Math.round(seekBarPreference.getValue() / 5)) * 5;
          seekBarPreference.setValue(progress);
          break;

        case "runMediaApp":
          switchPreference = findPreference(key);
          findPreference("runMediaAppDelay").setEnabled(switchPreference.isChecked());
          findPreference("runMediaAppDelay").setSelectable(switchPreference.isChecked());
          findPreference("runMediaAppTwice").setEnabled(switchPreference.isChecked());
          findPreference("runMediaAppTwice").setSelectable(switchPreference.isChecked());
          break;

        case "runMediaAppDelay":
          seekBarPreference = findPreference(key);
          delay = seekBarPreference.getValue();
          seekBarPreference.setSummary(
              String.format("%.0f", Float.intBitsToFloat(delay) / Float.intBitsToFloat(1000))
                  + "s");
          progress = (Math.round(seekBarPreference.getValue() / 1000)) * 1000;
          seekBarPreference.setValue(progress);
          break;

        case "duraspeed":
          switchPreference = findPreference(key);
          try {
            Utils.enableDisableDuraspeed(context, switchPreference.isChecked());
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
          prepDuraspeed(switchPreference);
          break;

        case "fullscreen":
          switchPreference = findPreference(key);
          try {
            String value = switchPreference.isChecked() ? "1" : "0";
            Utils.getSetFullscreenFlag(context, value);
            prepFullscreen(switchPreference);
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
          break;

        case "darkmode":
          switchPreference = findPreference(key);
          int checkVal =
              context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
          if (checkVal == PackageManager.PERMISSION_GRANTED) {

            Utils.setDarkMode(context, switchPreference.isChecked());
          } else if (checkVal == PackageManager.PERMISSION_DENIED) {
            ignoreKey = key;
            switchPreference.setChecked(!switchPreference.isChecked());
            new SimpleDialog(
                    key,
                    activity,
                    simpleDialogCallbacksSelfAuth,
                    "Missing permission",
                    "This option requires permission to modify Android Settings.\nAppSwitcher can try to authorize itself.\nTry self authorization?",
                    true)
                .show();
          }
          break;

        case "enableLogListener":
          switchPreference = findPreference(key);
          findPreference("logTag").setEnabled(switchPreference.isChecked());
          findPreference("logTag").setSelectable(switchPreference.isChecked());
          findPreference("logOnPress").setEnabled(switchPreference.isChecked());
          findPreference("logOnPress").setSelectable(switchPreference.isChecked());
          findPreference("logShortPress").setEnabled(switchPreference.isChecked());
          findPreference("logShortPress").setSelectable(switchPreference.isChecked());
          findPreference("logLongPress").setEnabled(switchPreference.isChecked());
          findPreference("logLongPress").setSelectable(switchPreference.isChecked());

          break;

        case "enableAutomateSrv":
          switchPreference = findPreference(key);
          EditTextPreference automateFlow = findPreference("automateFlow");
          if (switchPreference.isChecked()) {
            checkVal =
                context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
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
              switchPreference.setChecked(!switchPreference.isChecked());
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
          break;

        case "automateFlow":
          editTextPreference = findPreference(key);
          String currentValue = editTextPreference.getText().trim();
          if (context.getString(R.string.pref_automateFlow).equals(currentValue)
              || "".equals(currentValue)) {
            editTextPreference.setSummary("");
            editTextPreference.setText(context.getString(R.string.pref_automateFlow));
          } else {
            editTextPreference.setSummary(currentValue);
          }
          break;
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApplication = (AppSwitcherApp) getApplicationContext();

    context = getApplicationContext();
    sharedPreferencesHelper = new SharedPreferencesHelper(context);

    activity = SettingsActivity.this;
    setContentView(R.layout.activity_settings);

    Intent intent = getIntent();
    screen = intent.getStringExtra("screen");
    if (screen == null) {
      ((LinearLayout) findViewById(R.id.startService)).setVisibility(View.VISIBLE);

    } else {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // If you want to insert data in your settings
    settingsFragment = new PrefFragment();
    // settingsFragment. ...
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.frmSettings, settingsFragment)
        .commit();
  }

  public void startService(View view) {
    Intent intent = new Intent(context, StartServiceActivity.class);
    // intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  @Override // android.app.Activity
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    return true;
  }
}
