package com.thf.AppSwitcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.thf.AppSwitcher.service.AppSwitcherService;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import java.util.List;
//import java.util.Hashtable;

public class StartServiceActivity extends Activity {
	private static final String TAG = "AppSwitcherService";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		List<AppData> selectedList = SharedPreferencesHelper.loadList(getApplicationContext(), "selected");
		
		if (selectedList.size() == 0) {
			Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
			startActivity(intent);
			Toast.makeText(getBaseContext(), "Please set relevant apps and activities to allow start of AppSwitcher Service", Toast.LENGTH_LONG).show();
			return;
		}

		if (!AppSwitcherService.isRunning()) {
			Intent intent = new Intent(getBaseContext(), AppSwitcherService.class);
			startForegroundService(intent);
		}
		finish();
	}

}