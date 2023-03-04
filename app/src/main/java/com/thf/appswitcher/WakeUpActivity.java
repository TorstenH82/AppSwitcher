package com.thf.AppSwitcher;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.thf.AppSwitcher.service.AppSwitcherService;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;

public class WakeUpActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intentMediaApp = new Intent(getBaseContext(), AppSwitcherService.class);
        intentMediaApp.setAction(AppSwitcherService.ACTION_WAKE_UP);
        startForegroundService(intentMediaApp);
        finish();
    }
}
