package com.thf.AppSwitcher.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.thf.AppSwitcher.AppSwitcherApp;
import com.thf.AppSwitcher.GetPermissionsActivity;
import com.thf.AppSwitcher.OverlayWindow;
import com.thf.AppSwitcher.R;
import com.thf.AppSwitcher.SettingsActivity;
import com.thf.AppSwitcher.SwitchActivity;
import com.thf.AppSwitcher.utils.LogReaderUtil;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.SunriseSunset;
import com.thf.AppSwitcher.utils.UsageStatsUtil;
import com.thf.AppSwitcher.utils.Utils;
import com.thf.AppSwitcher.utils.RunMediaApp;
import com.thf.AppSwitcher.utils.AutoLinkBroadcast;

public class AppSwitcherService extends Service
    implements SharedPreferences.OnSharedPreferenceChangeListener {
  private static final String TAG = "AppSwitcherService";
  private AppSwitcherApp mApplication;
  private Context context;
  private SharedPreferencesHelper sharedPreferencesHelper;
  private static LogReaderUtil logReaderUtil;
  private static UsageStatsUtil usageStatsUtil;
  private static SunriseSunset sunriseSunset;
  private static AutoLinkBroadcast autoLinkBroadcast;
  private boolean enableLogListener;
  private String logTag;
  private String logOnPress;
  private String logShortPress;
  private String logLongPress;

  // private SharedPreferences sharedPreferences;

  private static boolean disableNaviMainActivity = false;
  private boolean runMediaApp;
  private Thread runMediaAppThread;

  private OverlayWindow overlayWindow;
  private int dimMode;
  private boolean autoDimActive = false;
  private boolean autolinkDim = false;
  private boolean forceLandscape = false;
  public static final int DIM_MODE_ON = 1;
  public static final int DIM_MODE_OFF = 0;
  public static final int DIM_MODE_AUTO = 2;

  private boolean buttonSound = false;
  private MediaPlayer mediaPlayer;

  private Handler handler = new Handler(Looper.getMainLooper());

  private static BroadcastReceiver bootUpReceiver = null;
  private Long lastLongPressTime = 0L;

  public Handler logHandler =
      new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(android.os.Message msg) {
          int action = msg.what;
          Log.i(TAG, "Received action: " + action);

          switch (action) {
            case LogReaderUtil.ACTION_ON_PRESS:
              // got action on press - short or long press will follow
              if (mApplication.getSwitchActivityRunning()) {
                Log.i(TAG, "send onPress broadcast - short or long press will follow");
                Intent intent = new Intent("switch-message");
                intent.putExtra("onPress", true);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
              }
              // do not open switch activity because this can be the start of a long press
              break;
            case LogReaderUtil.ACTION_LONG_PRESS:
              boolean cancel = false;
              if (lastLongPressTime != 0L) {
                if ((System.currentTimeMillis() - lastLongPressTime) < 1000) cancel = true;
              }
              lastLongPressTime = System.currentTimeMillis();
              if (cancel) return;

              // send bc to close if activity is running
              if (mApplication.getSwitchActivityRunning()) {
                Log.i(TAG, "send broadcast to close to dialog");
                Intent intent = new Intent("switch-message");
                intent.putExtra("close", true);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
              } else {
                // switch to home on long press if activity and so dialog is not visible
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
              }
              break;
            case LogReaderUtil.ACTION_SHORT_PRESS:
              lastLongPressTime = 0L;
              if (!mApplication.getSwitchActivityRunning()) {
                String foregroundApp;
                // get the current foreground app
                // we may get it from MainUI in future
                if (msg.obj == null) {
                  foregroundApp = usageStatsUtil.getCurrentActivity();
                } else {
                  foregroundApp = (String) msg.obj;
                }
                Log.i(TAG, "start dialog");
                Intent intent = new Intent(context, SwitchActivity.class);
                intent.putExtra("foregroundApp", foregroundApp);
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                // intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                try {
                  startActivity(intent);
                } catch (Exception ex) {
                  Log.e(TAG, ex.getMessage());
                }
              } else {
                Log.i(TAG, "send broadcast to dialog");
                Intent intent = new Intent("switch-message");
                // Adding some data
                // intent.putExtra("my-integer", 1);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
              }
              break;
          }

          if (buttonSound && LogReaderUtil.ACTION_ON_PRESS != action) {
            mediaPlayer.start();
          }

          if (runMediaApp && runMediaAppThread != null && runMediaAppThread.isAlive()) {
            runMediaAppThread.interrupt();
          }
        }
      };

  public AppSwitcherService() {}

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals("selected")) {
      usageStatsUtil.stopProgress();
      usageStatsUtil = new UsageStatsUtil(context, usageStatsCallbacks);
      usageStatsUtil.startProgress();
      sharedPreferencesHelper.getSelected(true);
    } else if (key.equals("enableLogListener")) {
      enableLogListener = sharedPreferencesHelper.getBoolean(key);
    } else if (key.equals("logTag")) {
      logTag = sharedPreferencesHelper.getString(key);
    } else if (key.equals("logOnPress")) {
      logOnPress = sharedPreferencesHelper.getString(key);
    } else if (key.equals("logShortPress")) {
      logShortPress = sharedPreferencesHelper.getString(key);
    } else if (key.equals("logLongPress")) {
      logLongPress = sharedPreferencesHelper.getString(key);
    } else if (key.equals("dimMode")) {
      dimMode = sharedPreferencesHelper.getInteger("dimMode");
      updateNotification("running...");
      setDimming(dimMode);
      if (autolinkDim) setAutoLinkDayNight(dimMode);
    } else if (key.equals("forceLandscape")) {
      overlayWindow.setLandscape(sharedPreferencesHelper.getBoolean("forceLandscape"));
    } else if (key.equals("dimScreen")) {
      overlayWindow.setBrightness(sharedPreferencesHelper.getInteger("dimScreen"));
    } else if (key.equals("buttonSound")) {
      buttonSound = sharedPreferencesHelper.getBoolean("buttonSound");
    } else if (key.equals("autolinkDim")) {
      autolinkDim = sharedPreferencesHelper.getBoolean("autolinkDim");
      if (autolinkDim) setAutoLinkDayNight(dimMode);
    }
    if (key.equals("logTag")
        || key.equals("logOnPress")
        || key.equals("logShortPress")
        || key.equals("logLongPress")
        || key.equals("enableLogListener")) {

      if (logReaderUtil != null) logReaderUtil.stopProgress();

      if (enableLogListener) {
        logReaderUtil =
            new LogReaderUtil(logHandler, logTag, logOnPress, logShortPress, logLongPress);
        logReaderUtil.startProgress();
      } else {
        logReaderUtil = null;
      }
    }
  }

  @Override
  public void onCreate() {
    isRunning = true;
    mApplication = (AppSwitcherApp) getApplicationContext();
    context = this;

    sharedPreferencesHelper = new SharedPreferencesHelper(context, this);

    mApplication.registerPermissionGrantedCallback(
        new AppSwitcherApp.PermissionGrantedCallbacks() {
          @Override
          public void onGrant(String permission, boolean granted) {
            if (granted) {
              sunriseSunset = new SunriseSunset(context, sunriseSunsetCallbacks);
              setDimming(dimMode);
              if (autolinkDim) setAutoLinkDayNight(dimMode);
            } else {
              if (dimMode == DIM_MODE_AUTO) {
                dimMode = DIM_MODE_OFF;
                sharedPreferencesHelper.setInteger("dimMode", AppSwitcherService.DIM_MODE_OFF);
              }
            }
          }
        });

    Notification notification = getNotification("running...");
    createchannel();
    startForeground(NOTIFCATION_ID, notification);
    // createchannel();

    enableLogListener = sharedPreferencesHelper.getBoolean("enableLogListener");
    logTag = sharedPreferencesHelper.getString("logTag");
    logOnPress = sharedPreferencesHelper.getString("logOnPress");
    logShortPress = sharedPreferencesHelper.getString("logShortPress");
    logLongPress = sharedPreferencesHelper.getString("logLongPress");

    if (enableLogListener) {
      Log.i(TAG, "LogListener create");
      logReaderUtil =
          new LogReaderUtil(logHandler, logTag, logOnPress, logShortPress, logLongPress);
    }
    usageStatsUtil = new UsageStatsUtil(this, usageStatsCallbacks);

    forceLandscape = sharedPreferencesHelper.getBoolean("forceLandscape");
    overlayWindow =
        new OverlayWindow(mApplication, sharedPreferencesHelper.getInteger("dimScreen"));
    overlayWindow.setLandscape(forceLandscape);
    dimMode = sharedPreferencesHelper.getInteger("dimMode");
    sunriseSunset = new SunriseSunset(context, sunriseSunsetCallbacks);

    autolinkDim = sharedPreferencesHelper.getBoolean("autolinkDim");
    autoLinkBroadcast = new AutoLinkBroadcast(context);

    buttonSound = sharedPreferencesHelper.getBoolean("buttonSound");
    mediaPlayer = MediaPlayer.create(context, R.raw.buttonpress);
    mediaPlayer.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build());
    /* try {
      mediaPlayer.prepare();
    } catch (IOException ex) {
    }
    */
  }

  private void registerBootUpRecv() {
    if (bootUpReceiver == null) {
      bootUpReceiver = new BootUpReceiver();
      IntentFilter filter = new IntentFilter();
      // filter.addAction("autochips.intent.action.QB_POWERON");
      filter.addAction("com.ts.main.uiaccon");
      // filter.addAction("autochips.intent.action.QB_POWEROFF");
      filter.addAction("com.ts.main.uiaccoff");
      filter.addAction("com.ts.main.DEAL_KEY");
      filter.addAction("broadcast_send_carinfo");
      filter.addAction("com.qf.action.ACC_ON"); // Ossuret
      filter.addAction("com.qf.action.ACC_OFF");
      filter.setPriority(1000);
      getApplicationContext().registerReceiver(bootUpReceiver, filter);
      Log.i(TAG, "Registered broadcast receiver");
    }
  }

  public static final String ACTION_WAKE_UP = "ACTION_WAKE_UP";
  public static final String ACTION_SLEEP = "ACTION_SLEEP";
  private static final String ACTION_STOP_SERVICE = "ACTION_STOP";
  private static final String ACTION_OPEN_SETTINGS = "ACTION_SETTINGS";
  public static final String ACTION_KEY = "ACTION_KEY";
  public static final String ACTION_ILL = "ACTION_ILL";
  private static final int NOTIFCATION_ID = 1337;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    String action = "";
    if (intent != null) action = intent.getAction();

    if (ACTION_STOP_SERVICE.equals(action)) {
      Log.d(TAG, "called to stop");
      stopForeground(true);
      stopSelf();
      return START_NOT_STICKY;

    } else if (ACTION_OPEN_SETTINGS.equals(action)) {
      Log.d(TAG, "called to open settings");
      Intent intentSettings = new Intent(context, SettingsActivity.class);
      intentSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      // intentSettings.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
      startActivity(intentSettings);
      return START_STICKY;

    } else if (ACTION_SLEEP.equals(action)) {
      isSleeping = true;
      Log.d(TAG, "called to sleep");
      stopThreads();
      updateNotification("sleeping...");
      return START_STICKY;

    } else if (ACTION_WAKE_UP.equals(action)) {
      Log.d(TAG, "called to wake up");
      Toast.makeText(this, "Wake up AppSwitcher Service", Toast.LENGTH_SHORT).show();

    } else if (ACTION_KEY.equals(action)) {
      int key = intent.getExtras().getInt("key");
      int cameraMode = intent.getExtras().getInt("cammode");

      if (cameraMode == 0) {
        Message completeMessage;
        switch (key) {
          case 9809:
            completeMessage = logHandler.obtainMessage(LogReaderUtil.ACTION_ON_PRESS);
            completeMessage.sendToTarget();
            break;
          case 9010:
          case 9810:
            String fgApp = intent.getStringExtra("topact");
            completeMessage = logHandler.obtainMessage(LogReaderUtil.ACTION_SHORT_PRESS);
            completeMessage.obj = fgApp;
            completeMessage.sendToTarget();
            break;
          case 9811:
            completeMessage = logHandler.obtainMessage(LogReaderUtil.ACTION_LONG_PRESS);
            completeMessage.sendToTarget();
            break;
        }
      }
      return START_STICKY;
      /*
      } else if (ACTION_ILL.equals(action)) {
        int ill = intent.getExtras().getInt("ill");
        Toast.makeText(this, "Illumination is: " + ill, Toast.LENGTH_SHORT).show();
        Intent intentSuding = new Intent();
        intentSuding.putExtra("command", "REQ_NIGHT_MODE_CMD");
        intentSuding.setAction("com.suding.speedplay");

        return START_STICKY;
      */
    } else { // standard start of service
      Log.d(TAG, "called to start");
      Toast.makeText(this, "Starting AppSwitcher Service", Toast.LENGTH_SHORT).show();
    }

    isSleeping = false;
    usageStatsUtil.startProgress(); // start before run of media app
    registerBootUpRecv();

    mediaPlayer = MediaPlayer.create(context, R.raw.buttonpress);
    mediaPlayer.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build());

    runMediaApp = sharedPreferencesHelper.getBoolean("runMediaApp");
    if (runMediaApp) {
      runMediaAppThread = new Thread(new RunMediaApp(context, usageStatsUtil));
      runMediaAppThread.start();
    }

    boolean enableAutomate = sharedPreferencesHelper.getBoolean("enableAutomateSrv");
    if (enableAutomate) {
      Utils.enableService(
          context,
          context.getString(R.string.automatePackage),
          context.getString(R.string.automateService),
          utilCallbacks);

      String automateFlow = sharedPreferencesHelper.getString("automateFlow");
      if (!"".equals(automateFlow)
          && !context.getString(R.string.pref_automateFlow).equals(automateFlow))
        Utils.startAutomateFlow(context, automateFlow, utilCallbacks);
    }

    updateNotification("running...");
    setDimming(dimMode);

    autolinkDim = sharedPreferencesHelper.getBoolean("autolinkDim");
    if (autolinkDim) setAutoLinkDayNight(dimMode);

    enableLogListener = sharedPreferencesHelper.getBoolean("enableLogListener");
    if (enableLogListener) {
      logReaderUtil.stopProgress();
      Toast.makeText(this, "Enable log listener", Toast.LENGTH_SHORT).show();
      logReaderUtil.startProgress();
    }
    // usageStatsUtil.startProgress();
    // If we get killed, after returning from here, restart
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    // We don't provide binding, so return null
    return null;
  }

  @Override
  public void onDestroy() {
    isRunning = false;
    sharedPreferencesHelper.unregisterOnSharedPreferenceChangeListener(this);
    stopThreads();
    if (autolinkDim) autoLinkBroadcast.setDay(false);
    mediaPlayer = null;
    Toast.makeText(this, "AppSwitcher Service stopped", Toast.LENGTH_LONG).show();
  }

  private void stopThreads() {
    if (logReaderUtil != null) {
      logReaderUtil.stopProgress();
    }
    if (usageStatsUtil != null) {
      usageStatsUtil.stopProgress();
    }
    if (sunriseSunset != null) {
      sunriseSunset.disableAuto();
    }
    if (overlayWindow != null) overlayWindow.hide();
  }

  // build a persistent notification and return it.
  public Notification getNotification(String message) {

    Intent stopSelf = new Intent(this, AppSwitcherService.class);
    stopSelf.setAction(ACTION_STOP_SERVICE);
    PendingIntent pStopSelf =
        PendingIntent.getService(
            this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Intent openSettings = new Intent(this, AppSwitcherService.class);
    openSettings.setAction(ACTION_OPEN_SETTINGS);
    PendingIntent pOpenSettings =
        PendingIntent.getService(
            this,
            0,
            openSettings,
            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    return new NotificationCompat.Builder(getApplicationContext(), id1)
        .setSmallIcon(R.drawable.transparenticon)
        .setLargeIcon(
            BitmapFactory.decodeResource(
                context.getResources(), R.mipmap.ic_stat_ic_launcher_adaptive_fore))
        .setOngoing(true) // persistent notification!
        .setChannelId(id1)
        .setContentTitle("AppSwitcher Service") // Title message top row.
        .setContentText(message) // message when looking at the notification, second row
        .setContentIntent(pOpenSettings)
        .addAction(R.mipmap.ic_stat_ic_launcher_adaptive_fore, "Stop", pStopSelf)
        .addAction(R.mipmap.ic_stat_ic_launcher_adaptive_fore, "Settings", pOpenSettings)
        .build(); // finally build and return a Notification.
  }

  private void updateNotification(String message) {
    Notification notification = getNotification(message);
    NotificationManager notificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(NOTIFCATION_ID, notification);
  }

  private static boolean isRunning = false;

  public static boolean isRunning() {
    return isRunning;
  }

  private static boolean isSleeping = false;

  public static boolean isSleeping() {
    return isSleeping;
  }

  public static String id1 = "appswitcher_channel_01";

  private void createchannel() {
    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    NotificationChannel mChannel =
        new NotificationChannel(
            id1,
            getString(R.string.channel_name), // name of the channel
            NotificationManager.IMPORTANCE_LOW); // importance level
    // important level: default is is high on the phone.  high is urgent on the phone.  low is
    // medium, so none is low?
    // Configure the notification channel.
    mChannel.setDescription(getString(R.string.channel_description));
    mChannel.enableLights(true);
    // Sets the notification light color for notifications posted to this channel, if the device
    // supports this feature.
    mChannel.setShowBadge(true);
    nm.createNotificationChannel(mChannel);
  }

  private Utils.UtilCallbacks utilCallbacks =
      new Utils.UtilCallbacks() {
        @Override
        public void onException(Throwable e) {
          Toast.makeText(context, "Error occured: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
      };

  private SunriseSunset.SunriseSunsetCallbacks sunriseSunsetCallbacks =
      new SunriseSunset.SunriseSunsetCallbacks() {
        @Override
        public void onSunrise() {
          if (forceLandscape) {
            overlayWindow.show(OverlayWindow.OverlayMode.OM_TRANSPARENT);
          } else {
            overlayWindow.hide();
          }
          if (autolinkDim) autoLinkBroadcast.setDay(false);
          autoDimActive = false;
          updateNotification("Auto dimming active (off)");
        }

        @Override
        public void onSunset() {
          showOverlayNight();
          if (autolinkDim) autoLinkBroadcast.setNight(false);
          autoDimActive = true;
          updateNotification("Auto dimming active (on)");
        }

        @Override
        public void onMissingPermission(String permission) {
          Intent intentGetPermission = new Intent(context, GetPermissionsActivity.class);
          intentGetPermission.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intentGetPermission.putExtra("permission", permission);
          startActivity(intentGetPermission);
        }
      };

  private UsageStatsUtil.UsageStatsCallbacks usageStatsCallbacks =
      new UsageStatsUtil.UsageStatsCallbacks() {
        @Override
        public void onForegroundApp(String foreground) {
          // hide overlay if Google PlayStore is in foreground
          if (foreground.startsWith("com.android.vending")
              || foreground.startsWith(getString(R.string.autoLinkPackage))) {
            overlayWindow.hideTmp(); // hide if currently visible
          } else {
            overlayWindow.reShow(); // show again if overlay was visible
          }

          if (autolinkDim) {
            if (foreground.startsWith(getString(R.string.autoLinkPackage))) {
              setAutoLinkDayNight(dimMode);
            } else {
              autoLinkBroadcast.stop();
            }
          }
        }
      };

  private void setDimming(int dimMode) {
    switch (dimMode) {
      case DIM_MODE_ON:
        sunriseSunset.disableAuto();
        showOverlayNight();
        break;
      case DIM_MODE_OFF:
        sunriseSunset.disableAuto();
        if (forceLandscape) {
          overlayWindow.show(OverlayWindow.OverlayMode.OM_TRANSPARENT);
        } else {
          overlayWindow.hide();
        }
        break;
      case DIM_MODE_AUTO:
        if (sunriseSunset.getLocation() == null) {
          sunriseSunset = new SunriseSunset(context, sunriseSunsetCallbacks);
        }
        sunriseSunset.enableAuto();
        break;
    }
  }

  private void showOverlayNight() {
    if (usageStatsUtil.getCurrentActivity().startsWith("com.android.vending")
        || usageStatsUtil.getCurrentActivity().startsWith(getString(R.string.autoLinkPackage))) {
      overlayWindow.prepareReShow(OverlayWindow.OverlayMode.OM_DIM);
    } else {
      overlayWindow.show(OverlayWindow.OverlayMode.OM_DIM);
    }
  }

  private void setAutoLinkDayNight(int dimMode) {
    switch (dimMode) {
      case DIM_MODE_ON:
        autoLinkBroadcast.setNight(true);
        break;
      case DIM_MODE_OFF:
        autoLinkBroadcast.setDay(false);
        break;
      case DIM_MODE_AUTO:
        if (autoDimActive) {
          autoLinkBroadcast.setNight(true);
        } else {
          autoLinkBroadcast.setDay(false);
        }
        break;
    }
  }
}
