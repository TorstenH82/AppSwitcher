package com.thf.AppSwitcher;

import android.app.Application;
import android.content.pm.PackageManager;

public class AppSwitcherApp extends Application {
	private static AppSwitcherApp singleton;
	private static boolean isOverlayVisible = false;
	private static boolean isSwitchActivityRunning = false;
    private static PackageManager packageManager;

	public AppSwitcherApp getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
        packageManager = this.getPackageManager();
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