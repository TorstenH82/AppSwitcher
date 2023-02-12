package com.thf.AppSwitcher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.thf.AppSwitcher.service.AppSwitcherService;
import androidx.core.app.ActivityCompat;

public class GetPermissionsActivity extends Activity {
	private Context context;
	private AppSwitcherApp mApplication;
	private String permission;
	 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();
		mApplication = (AppSwitcherApp) getApplicationContext();

		Intent intent = getIntent();
		permission = intent.getStringExtra("permission");

		 

		if (!checkPermission(permission)) {
			requestPermission(permission);
		}
	}

	private boolean checkPermission(String permissionFull) {
		//int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
		int checkVal = context.checkSelfPermission(permissionFull);
		return checkVal == PackageManager.PERMISSION_GRANTED;
	}

	private void requestPermission(String permissionFull) {
		ActivityCompat.requestPermissions(this, new String[] { permissionFull }, 1);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
		case 1:
			if (grantResults.length > 0) {
				boolean granted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
				if (granted) {
					mApplication.permissionWasGranted(permission);
				} else {
					mApplication.permissionWasDenied(permission);
				}
			}
			break;
		}
		finish();
	}
}