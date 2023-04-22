package com.thf.AppSwitcher.service;

 

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

/**
 * Created by Wen on 1/14/15.
 */
public class FgCheckService extends AccessibilityService {
    private static FgCheckService sInstance;

    public static FgCheckService getInstance() {
        return sInstance;
    }

    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.i("AppSwitcher" , event.getPackageName() + "vvv");
            
            //if (SPHelper.isShowWindow(this)) {
            //    TasksWindow.show(this, event.getPackageName() + "\n" + event.getClassName());
            //}
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        sInstance = this;
        //if (SPHelper.isShowWindow(this)) {
        //    NotificationActionReceiver.showNotification(this, false);
        //}
        //sendBroadcast(new Intent(QuickSettingTileService.ACTION_UPDATE_TITLE));
        super.onServiceConnected();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sInstance = null;
        //TasksWindow.dismiss(this);
        //NotificationActionReceiver.cancelNotification(this);
        //sendBroadcast(new Intent(QuickSettingTileService.ACTION_UPDATE_TITLE));
        return super.onUnbind(intent);
    }
}