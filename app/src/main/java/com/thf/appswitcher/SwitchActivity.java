package com.thf.AppSwitcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.SwitchAppsAdapter;
import com.thf.AppSwitcher.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import com.thf.AppSwitcher.utils.LogReaderUtil;

public class SwitchActivity extends Activity {
  private static final String TAG = "AppSwitcherService";
  private AppSwitcherApp mApplication;
  private List<AppData> newAppList = new ArrayList<AppData>();
  private static SwitchAppsAdapter adapter;
  private static Context context;
  private static int dialogDelay;

  SwitchDialog newDialog;

  public Handler handler = new Handler(Looper.getMainLooper());
  private boolean disableNaviMainActivity = false;
  private boolean showClock = true;
  private float brightness;
  private static boolean grayscale = true;

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mApplication = (AppSwitcherApp) getApplicationContext();
    mApplication.setSwitchActivityRunning(true);
    context = getApplicationContext();

    LocalBroadcastManager.getInstance(this)
        .registerReceiver(messageReceiver, new IntentFilter("switch-message"));

    Log.i(TAG, "SwitchActivity onCreate");
    context = getApplicationContext();

    dialogDelay = SharedPreferencesHelper.getInteger(context, "dialogDelay");
    Log.i(TAG, "Dialog delay: " + Integer.toString(dialogDelay));
    disableNaviMainActivity = SharedPreferencesHelper.getBoolean(context, "disableNaviStart");
    showClock = SharedPreferencesHelper.getBoolean(context, "showClock");
    brightness = ((float) SharedPreferencesHelper.getInteger(context, "itemsBrightness")) / 100;
    grayscale = SharedPreferencesHelper.getBoolean(context, "grayscaleIcons");

    new Thread(runnablePrepareData).start();
        
    newDialog =
        new SwitchDialog(
            SwitchActivity.this,
            switchDialogListener,
            dialogDelay,
            brightness,
            grayscale,
            showClock);
  }

  private SwitchDialog.SwitchDialogCallbacks switchDialogListener =
      new SwitchDialog.SwitchDialogCallbacks() {
        @Override
        public void onResult(Intent intent) {
          if (intent != null) {
            context.startActivity(intent);
          }
          finish();
        }
      };

  @Override
  protected void onPause() {
    super.onPause();
    Log.d(TAG, "pause SwitchActivity");
    newDialog.dismiss();

    if (disableNaviMainActivity) Utils.enableDisableNaviMainActivity(context, false, utilCallbacks);
  }

  private Utils.UtilCallbacks utilCallbacks =
      new Utils.UtilCallbacks() {
        @Override
        public void onException(Throwable e) {
          Toast.makeText(context, "error" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
      };

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume SwitchActivity");
    newDialog.show();
    if (disableNaviMainActivity) Utils.enableDisableNaviMainActivity(context, true, utilCallbacks);
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mApplication.setSwitchActivityRunning(false);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (newDialog != null) {
      newDialog.dismiss();
    }
  }

  private BroadcastReceiver messageReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          // Extract data included in the Intent
          Log.i(TAG, "broadcast message received");
          // int yourInteger = intent.getIntExtra("my-integer", -1); // -1 is going to be
          // used as the default value
          boolean close = intent.getBooleanExtra("close", false);
          boolean onPress = intent.getBooleanExtra("onPress", false);

          if (close) {
            newDialog.dismiss();
            finish();
            return;
          }

          if (onPress) {
            // nothing to do because bomb already disarmed
            // next bc will come after user releases swc button
            newDialog.action(LogReaderUtil.ACTION_ON_PRESS);
            return;
          }
          Log.d(TAG, "jump to next entry");
          newDialog.action(LogReaderUtil.ACTION_SHORT_PRESS);
        }
      };

  

  private Runnable runnablePrepareData =
      new Runnable() {
        @Override
        public void run() {

          List<AppData> selectedList =
              SharedPreferencesHelper.loadList(getApplicationContext(), "selected");

          boolean launcherIsInForeground = false;
          boolean addLauncher = SharedPreferencesHelper.getBoolean(context, "addLauncher");

          if (SharedPreferencesHelper.getBoolean(context, "smartList")) {
            String foregroundApp = "";
            try {
              Intent intent = getIntent();
              foregroundApp = intent.getStringExtra("foregroundApp");
              Log.i(TAG, "Foreground app from intent: " + foregroundApp);

            } catch (Exception ex) {
              Log.e(TAG, ex.getMessage());
            }

            if (foregroundApp == null) {
              foregroundApp = "-/-";
            }

            List<AppData> recentsAppList =
                SharedPreferencesHelper.getRecentsList(getApplicationContext());

            boolean mediaAppInForeground = false;
            AppData naviInForeground = new AppData();
            boolean naviIsInForeground = false;

            if (Utils.listContainsKey(selectedList, foregroundApp, "media")) {
              mediaAppInForeground = true;
            } else if (Utils.listContainsKey(selectedList, foregroundApp.split("/")[0], "media")) {
              foregroundApp = foregroundApp.split("/")[0];
              mediaAppInForeground = true;
            } else if (Utils.listContainsKey(selectedList, foregroundApp.split("/")[0], "navi")) {
              foregroundApp = foregroundApp.split("/")[0];
              naviIsInForeground = true;
            } else if (foregroundApp.split("/")[0].equals(
                mApplication.getLauncher().getPackageName())) {
              launcherIsInForeground = true;
            }

            Log.i(TAG, "handle " + foregroundApp);

            Iterator<AppData> i = recentsAppList.iterator();
            int sort = -1;
            boolean prioPosSet = false;

            while (i.hasNext()) {
              AppData s = i.next(); // must be called before you can call i.remove()

              // skip current app and apps not selected by user anymore
              if (TextUtils.equals(s.getKey(), foregroundApp)
                  || !Utils.listContainsKey(selectedList, s.getKey(), null)) continue;

              Boolean posSet = false;

              // special handling for recent navis
              if (Utils.listContainsKey(selectedList, s.getKey(), "navi")) {

                // navi is not in front, media app in front
                // -> offer recent navi on 1st pos
                if (!naviIsInForeground && mediaAppInForeground && !prioPosSet) {
                  s.setSort(-1);
                  prioPosSet = true;
                  posSet = true;

                  // navi is not in front, media app not in front
                  // --> offer recent navi on 2nd pos
                } else if (!naviIsInForeground && !mediaAppInForeground && !prioPosSet) {
                  s.setSort(1);
                  prioPosSet = true;
                  posSet = true;

                  // navi ia in front and previous app is also a navi
                  // --> offer recent navi on 2nd pos
                } else if (sort == -1 && naviIsInForeground) {
                  s.setSort(1); // not on 1st position

                  posSet = true;
                }
              }

              if (!posSet) {
                sort++;
                // positions 1 is reserved
                if (sort == 1) sort = 2;

                s.setSort(sort);
              }

              newAppList.add(0, s);
              selectedList.remove(s);
            }

            for (AppData app : selectedList) {
              if (!app.getKey().equals(foregroundApp)) {
                // Log.d(TAG, "added " + app.getPackageName() + " to pos 9999");
                app.setSort(9999);
                newAppList.add(app);
              }
            }

          } else {
            newAppList = selectedList;
          }
          Collections.sort(
              newAppList,
              new Comparator<AppData>() {
                public int compare(AppData o1, AppData o2) {
                  // compare two instance of `Score` and return `int` as
                  // result.
                  int cmp = Integer.compare(o1.getSort(), o2.getSort());
                  if (cmp == 0) {
                    cmp = o1.getDescription().compareTo(o2.getDescription());
                  }
                  return cmp;
                }
              });

          if (addLauncher) {
            newAppList.add(1, mApplication.getLauncher());
          }

          handler.post(
              new Runnable() {
                @Override
                public void run() {
                  newDialog.setItems(newAppList);
                }
              });
        }
      };
}
