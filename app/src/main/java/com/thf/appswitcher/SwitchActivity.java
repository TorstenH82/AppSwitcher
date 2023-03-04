package com.thf.AppSwitcher;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.thf.AppSwitcher.service.AppSwitcherService;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.FlashButton;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.SwitchAppsAdapter;
import com.thf.AppSwitcher.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class SwitchActivity extends Activity {
    private static final String TAG = "AppSwitcherService";
    private AppSwitcherApp mApplication;
    private List<AppData> newAppList = new ArrayList<AppData>();
    private static SwitchAppsAdapter adapter;
    private static Context context;
    private static int dialogDelay;

    private static Bomb myBomb;

    public Handler handler = new Handler(Looper.getMainLooper());

    private RecyclerView.LayoutManager layoutManager;
    private Intent mainIntent;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    public static Dialog dialog;
    private LinearLayoutManager linearLayoutManager;

    private boolean disableNaviMainActivity = false;
    private boolean showClock = true;
    private String barAnimation = "3STEP";
    private float brightness;

    private LinearLayout settingsSel;
    private FlashButton dimScreenMode;
    private TextView txtTitle;
    private TextClock textClock;

    private static boolean dataLoaded = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = (AppSwitcherApp) getApplicationContext();
        mApplication.setSwitchActivityRunning(true);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("switch-message"));

        Log.i(TAG, "SwitchActivity onCreate");
        context = getApplicationContext();

        dialogDelay = SharedPreferencesHelper.getInteger(context, "dialogDelay");
        Log.i(TAG, "Dialog delay: " + Integer.toString(dialogDelay));

        disableNaviMainActivity = SharedPreferencesHelper.getBoolean(context, "disableNaviStart");
        showClock = SharedPreferencesHelper.getBoolean(context, "showClock");
        brightness = SharedPreferencesHelper.getInteger(context, "itemsBrightness");
        brightness = brightness / 100;

        Runnable runnable =
                new Runnable() {
                    @Override
                    public void run() {

                        List<AppData> selectedList =
                                SharedPreferencesHelper.loadList(
                                        getApplicationContext(), "selected");

                        boolean launcherIsInForeground = false;
                        boolean addLauncher =
                                SharedPreferencesHelper.getBoolean(context, "addLauncher");

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
                            } else if (Utils.listContainsKey(
                                    selectedList, foregroundApp.split("/")[0], "media")) {
                                foregroundApp = foregroundApp.split("/")[0];
                                mediaAppInForeground = true;
                            } else if (Utils.listContainsKey(
                                    selectedList, foregroundApp.split("/")[0], "navi")) {
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
                                AppData s =
                                        i.next(); // must be called before you can call i.remove()

                                // skip current app and apps not selected by user anymore
                                if (TextUtils.equals(s.getKey(), foregroundApp)
                                        || !Utils.listContainsKey(selectedList, s.getKey(), null))
                                    continue;

                                Boolean posSet = false;

                                // special handling for recent navis
                                if (Utils.listContainsKey(selectedList, s.getKey(), "navi")) {

                                    // navi is not in front, media app in front
                                    // -> offer recent navi on 1st pos
                                    if (!naviIsInForeground
                                            && mediaAppInForeground
                                            && !prioPosSet) {
                                        s.setSort(-1);
                                        prioPosSet = true;
                                        posSet = true;

                                        // navi is not in front, media app not in front
                                        // --> offer recent navi on 2nd pos
                                    } else if (!naviIsInForeground
                                            && !mediaAppInForeground
                                            && !prioPosSet) {
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
                                            cmp =
                                                    o1.getDescription()
                                                            .compareTo(o2.getDescription());
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
                                        setItems(newAppList);
                                    }
                                });
                    }
                };

        new Thread(runnable).start();

        adapter =
                new SwitchAppsAdapter(
                        getApplicationContext(),
                        newAppList,
                        brightness,
                        new SwitchAppsAdapter.Listener() {
                            @Override
                            public void onItemClick(View item, AppData app) {
                                myBomb.disarm();
                                execActivity(app);
                                dialog.cancel();
                                finish();
                            }

                            @Override
                            public void onTouch() {
                                myBomb.disarm();
                                adapter.clearPosition();
                                textClock.setVisibility(View.GONE);
                                settingsSel.setVisibility(View.VISIBLE);
                                dimScreenMode.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onTitleChanged(String title) {
                                txtTitle.setText(title);
                            }
                        });

        showDialog(SwitchActivity.this);
    }

    public void showDialog(Activity activity) {

        dialog = new Dialog(activity);
        dialog.setCancelable(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_switch);
        dialog.getWindow()
                .setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // hide status bar during dialog display:

        // dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialogbackground);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        /* click on area outside of recycler */
        dialog.findViewById(R.id.constraintLayout)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                myBomb.disarm();
                                adapter.clearPosition();
                                textClock.setVisibility(View.GONE);
                                settingsSel.setVisibility(View.VISIBLE);
                                dimScreenMode.setVisibility(View.VISIBLE);
                            }
                        });

        /* settings selection */
        settingsSel = dialog.findViewById(R.id.settingsSel);
        settingsSel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        myBomb.disarm();
                        Intent intent = new Intent(context, SettingsActivity.class);
                        startActivity(intent);
                    }
                });
        settingsSel.setVisibility(View.GONE);

        dimScreenMode = dialog.findViewById(R.id.dimScreenMode);
        dimScreenMode.setFlashListener(
                new FlashButton.FlashListener() {
                    @Override
                    public void onState(FlashButton.FlashEnum state) {
                        myBomb.disarm();
                        switch (state) {
                            case ON:
                                SharedPreferencesHelper.setInteger(
                                        context, "dimMode", AppSwitcherService.DIM_MODE_ON);
                                break;
                            case OFF:
                                SharedPreferencesHelper.setInteger(
                                        context, "dimMode", AppSwitcherService.DIM_MODE_OFF);
                                break;
                            case AUTO:
                                int checkVal =
                                        context.checkCallingOrSelfPermission(
                                                Manifest.permission.ACCESS_COARSE_LOCATION);
                                if (checkVal == PackageManager.PERMISSION_GRANTED) {
                                    SharedPreferencesHelper.setInteger(
                                            context, "dimMode", AppSwitcherService.DIM_MODE_AUTO);
                                } else {
                                    // SharedPreferencesHelper.setInteger(context, "dimMode",
                                    // AppSwitcherService.DIM_MODE_ON);
                                    dimScreenMode.setState(FlashButton.FlashEnum.OFF);
                                    SharedPreferencesHelper.setInteger(
                                            context, "dimMode", AppSwitcherService.DIM_MODE_OFF);
                                }
                                break;
                        }
                    }
                });
        dimScreenMode.setVisibility(View.GONE);

        int mode = SharedPreferencesHelper.getInteger(context, "dimMode");
        switch (mode) {
            case AppSwitcherService.DIM_MODE_ON:
                dimScreenMode.setState(FlashButton.FlashEnum.ON);
                break;
            case AppSwitcherService.DIM_MODE_OFF:
                dimScreenMode.setState(FlashButton.FlashEnum.OFF);
                break;
            case AppSwitcherService.DIM_MODE_AUTO:
                dimScreenMode.setState(FlashButton.FlashEnum.AUTO);
                break;
        }

        txtTitle = dialog.findViewById(R.id.titleTxt);
        textClock = dialog.findViewById(R.id.textClock);
        if (showClock) {
            textClock.setVisibility(View.VISIBLE);
        } else {
            textClock.setVisibility(View.GONE);
        }

        RecyclerView recyclerView = dialog.findViewById(R.id.activityRecycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        linearLayoutManager =
                new LinearLayoutManager(
                        getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });

        dialog.show();
        progressBar = (ProgressBar) dialog.findViewById(R.id.progressbarSw);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "pause SwitchActivity");
        if (myBomb != null) myBomb.disarm();
        if (disableNaviMainActivity)
            Utils.enableDisableNaviMainActivity(context, false, utilCallbacks);
    }

    private Utils.UtilCallbacks utilCallbacks =
            new Utils.UtilCallbacks() {
                @Override
                public void onException(Throwable e) {
                    Toast.makeText(context, "error" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };

    public static void setItems(List<AppData> newData) {
        adapter.setItems(newData);
        dataLoaded = true;
    }

    Runnable startBomb =
            new Runnable() {
                @Override
                public void run() {
                    while (!dataLoaded) {}
                    myBomb = new Bomb(dialogDelay);
                    myBomb.startProgress();
                }
            };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume SwitchActivity");
        if (myBomb != null) myBomb.disarm();

        if (disableNaviMainActivity)
            Utils.enableDisableNaviMainActivity(context, true, utilCallbacks);

        // myBomb = new Bomb(dialogDelay);
        // myBomb.startProgress();
        new Thread(startBomb).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // myBomb.disarm();
        mApplication.setSwitchActivityRunning(false);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.cancel();
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

                    myBomb.disarm();

                    if (close) {
                        finish();
                    }

                    if (onPress) {
                        // nothing to do because bomb already disarmed
                        // next bc will come after user releases swc button
                        return;
                    }

                    int position = adapter.setPosition();
                    linearLayoutManager.scrollToPosition(position);
                    myBomb = new Bomb(dialogDelay);
                    myBomb.startProgress();
                }
            };

    public class Bomb {
        private Handler handler;
        private Integer wait = 2000;
        private Boolean disarmed = false;
        private Thread t;

        public Bomb(Integer wait) {
            this.handler = new Handler(Looper.getMainLooper());
            this.wait = wait;
        }

        public void disarm() {
            this.disarmed = true;
            if (t != null && t.isAlive()) t.interrupt();
        }

        public void startProgress() {
            // do something long
            Runnable runnable =
                    new Runnable() {
                        private int i;

                        @Override
                        public void run() {
                            for (i = 0; i < 4; i++) {
                                handler.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                progressBar.setProgress(99 - 33 * i);
                                                // progressBar.setProgress(i);
                                            }
                                        });
                                
                                try {
                                    Thread.sleep(wait / 3); // 100
                                } catch (InterruptedException ex) {
                                    Log.i(TAG, "timer of SwitchActivity interrupted");
                                    handler.post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressBar.setProgress(100);
                                                }
                                            });
                                    return;
                                }
                            }
                            if (!disarmed) {
                                handler.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                execActivity(adapter.getCurrentApp());
                                                dialog.cancel();
                                                finish();
                                            }
                                        });
                            }
                        }
                    };
            t = new Thread(runnable);
            // Log.i(TAG, "tick");
            this.disarmed = false;
            t.start();
        }
    }

    public void execActivity(AppData app) {
        if ("launcher".equals(app.getCategory())) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            return;
        }

        ComponentName name = new ComponentName(app.getPackageName(), app.getActivityName());
        Intent intentSelectedApp = new Intent(Intent.ACTION_MAIN);
        intentSelectedApp.addCategory(Intent.CATEGORY_LAUNCHER);
        intentSelectedApp.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intentSelectedApp.setComponent(name);
        try {
            startActivity(intentSelectedApp);
        } catch (Exception ex) {
            Log.e(TAG, "Error starting activity: " + ex.getMessage());
        }
    }
}
