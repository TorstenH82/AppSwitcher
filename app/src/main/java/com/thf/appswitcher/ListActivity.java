package com.thf.AppSwitcher;

import android.app.backup.SharedPreferencesBackupHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.thf.AppSwitcher.utils.ActivityUtil;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.ItemMoveCallback;
import com.thf.AppSwitcher.utils.RecyclerViewAdapter;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ListActivity extends AppCompatActivity {
	private static final String TAG = "AppSwitcherService";
	private static RecyclerViewAdapter adapter;
	private List<AppData> selectedList = new ArrayList<>();
	private Boolean apps;
	ActivityUtil au;
	private int checkedPos = -1;
	private String data;
	private String list;
	public Handler handler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message message) {
			if (au == null) {
				Log.e(TAG, "au is null");
			}
			//List<AppData> newAppList = au.getValue();
			adapter.setItems(au.getValue());
			progressBar.setVisibility(View.GONE);
		}
	};
	private RecyclerView.LayoutManager layoutManager;
	private Intent mainIntent;

	private ProgressBar progressBar;
	private RecyclerView recyclerView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		progressBar = findViewById(R.id.progressBar);
		recyclerView = findViewById(R.id.listView);

		recyclerView.setHasFixedSize(true);
		layoutManager = new LinearLayoutManager(this);
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.setLayoutManager(layoutManager);

		progressBar.setVisibility(View.VISIBLE);

		Intent intent = getIntent();
		data = intent.getStringExtra("appDataList");

		String category = "";

		selectedList = SharedPreferencesHelper.loadList(getApplicationContext(), "selected");
		switch (data) {

		case "navi":
			apps = true;
			category = "app";
			list = "navi";
			selectedList = selectedList.stream().filter(appData -> list.equals(appData.getList()))
					.collect(Collectors.toList());
			break;
		case "activities":
			apps = false;
			category = "activity";
			list = "media";
			selectedList = selectedList.stream().filter(appData -> list.equals(appData.getList()))
					.collect(Collectors.toList());
			break;
		case "sort":
			apps = false;
			category = "navi";
			list = "sort";
			break;
		}

		adapter = new RecyclerViewAdapter(new ArrayList<AppData>(), getApplicationContext(), apps, list, selectedList);		
		
		recyclerView.setAdapter(adapter);

		if ("sort".equals(list)) {
			ItemTouchHelper.Callback callback = new ItemMoveCallback(adapter);
			ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
			touchHelper.attachToRecyclerView(recyclerView);
			
			adapter.setTouchHelper(touchHelper);
			
			Collections.sort(selectedList, new Comparator<AppData>() {
				public int compare(AppData o1, AppData o2) {
					// compare two instance of `Score` and return `int` as result.
					int cmp = Integer.compare(o1.getSort(), o2.getSort());
					if (cmp == 0) {
						cmp = o1.getDescription().compareTo(o2.getDescription());
					}
					return cmp;
				}
			});
			adapter.setItems(selectedList);
			progressBar.setVisibility(View.GONE);
		} else {
			au = new ActivityUtil(getApplicationContext(), handler, category);
			au.startProgress();
		}
		
	}

	protected void onPause() {
		super.onPause();
		//SharedPreferencesHelper.SaveDict(getApplicationContext(), my_dict, "my_dict" + data);
	}

	protected void onResume() {
		super.onResume();
	}

	@Override
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