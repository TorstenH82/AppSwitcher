package com.thf.AppSwitcher;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;
import com.thf.AppSwitcher.service.BootUpReceiver;
import com.thf.AppSwitcher.utils.ActivityUtil;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.AppDataIcon;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;


public class AppSwitcherApp extends Application {
  private static final String TAG = "AppSwitcherService";
  private static AppSwitcherApp appSwitcherApp;
  // private static BroadcastReceiver mQuickBootRecv = null;
  private static boolean isOverlayVisible = false;
  private static boolean isSwitchActivityRunning = false;
  private static PackageManager packageManager;
  private static AppDataIcon launcher;

  public static AppSwitcherApp getInstance() {
    return appSwitcherApp;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    appSwitcherApp = this;
    packageManager = this.getPackageManager();
    ActivityUtil activityUtil = new ActivityUtil(appSwitcherApp, null, null);
    launcher = activityUtil.getLauncher();
        
        SharedPreferencesHelper sharedPreferencesHelper = new SharedPreferencesHelper(this);
        sharedPreferencesHelper.getSelected(true);
        
  }

  public AppDataIcon getLauncher() {
    return launcher;
  }

  public void setOverlayVisibility(boolean running) {
    isOverlayVisible = running;
  }

  public boolean getOverlayVisibility() {
    return isOverlayVisible;
  }

  public void setSwitchActivityRunning(boolean running) {
    isSwitchActivityRunning = running;
  }

  public boolean getSwitchActivityRunning() {
    return isSwitchActivityRunning;
  }

  private PermissionGrantedCallbacks permissionGrantedCallbacks;

  public interface PermissionGrantedCallbacks {
    public void onGrant(String permission, boolean granted);
  }

  public void registerPermissionGrantedCallback(PermissionGrantedCallbacks listener) {
    permissionGrantedCallbacks = listener;
  }

  public void permissionWasGranted(String permission) {
    if (permissionGrantedCallbacks != null) {
      permissionGrantedCallbacks.onGrant(permission, true);
    }
  }

  public void permissionWasDenied(String permission) {
    if (permissionGrantedCallbacks != null) {
      permissionGrantedCallbacks.onGrant(permission, false);
    }
  }

  public PackageManager getStaticPackageManager() {
    return packageManager;
  }
}
