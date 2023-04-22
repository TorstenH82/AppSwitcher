package com.thf.AppSwitcher;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.view.Window;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.State;
import com.thf.AppSwitcher.SwitchDialog;
import com.thf.AppSwitcher.utils.FlashButton;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.service.AppSwitcherService;
import android.widget.TextClock;
import android.content.DialogInterface;
import android.widget.ProgressBar;
import com.thf.AppSwitcher.utils.SwitchAppsAdapter;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.AppDataIcon;
import java.util.List;
import android.content.Intent;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

public class SwitchDialog extends Dialog {
  private static final String TAG = "AppSwitcherService";

  private static Bomb bomb;
  private Activity activity;
  private SharedPreferencesHelper sharedPreferencesHelper;
  private SwitchDialogCallbacks listener;
  private Handler handler = new Handler(Looper.getMainLooper());
  private LinearLayoutManager linearLayoutManager;
  private ProgressBar progressBar;
  private LinearLayout settingsSel;
  private FlashButton dimScreenMode;
  private TextClock textClock;
  private TextView txtTitle;
  private static SwitchAppsAdapter adapter;
  private AppSwitcherApp mApplication;

  private static boolean adapterDataSet = false;
  private static boolean layoutComplete = false;
  private int dialogDelay;
  private float brightness;
  private boolean grayscaleIcons = false;
  private boolean showClock = true;
  // private boolean forceLandscape = false;
  private int position = -99;

  public SwitchDialog(
      Activity activity,
      SwitchDialogCallbacks listener,
      int dialogDelay,
      float brightness,
      boolean grayscaleIcons,
      boolean showClock) {

    // super(activity, R.style.Theme_Transparent);
    super(activity);
    this.activity = activity;
    this.sharedPreferencesHelper = new SharedPreferencesHelper(activity);
    this.listener = listener;
    this.dialogDelay = dialogDelay;
    this.showClock = showClock;
    bomb = new Bomb(dialogDelay);

    adapter =
        new SwitchAppsAdapter(
            activity,
            brightness,
            grayscaleIcons,
            new SwitchAppsAdapter.Listener() {
              @Override
              public void onItemClick(View item, AppDataIcon app) {
                SwitchDialog.this.dismiss();
                callBackAppData(app);
              }

              @Override
              public void onTouch() {
                bomb.disarm();
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

    prepareDialog();
  }

  public void setItems(List<AppDataIcon> newData) {
    adapter.setItems(newData);
    adapterDataSet = true;
  }

  private void setShowClock(boolean showClock) {
    this.showClock = showClock;
    if (showClock) {
      textClock.setVisibility(View.VISIBLE);
    } else {
      textClock.setVisibility(View.GONE);
    }
  }

  public interface SwitchDialogCallbacks {
    public void onResult(AppData app);
  }

  public void callBackAppData(AppData app) {
    this.dismiss();
    Intent intent;

    listener.onResult(app);
  }

  public enum Action {
    CLOSE,
    NOTHING,
    NEXT
  }

  public void action(Action action) {
    if (bomb != null) bomb.disarm();
    switch (action) {
      case CLOSE:
        this.dismiss();

      case NOTHING:
        // nothing to do. Bomb already disarmed
        break;

      case NEXT:
        Integer pos = adapter.setPosition();
        if (pos != null) {
          position = pos;
          linearLayoutManager.scrollToPosition(position);
        }
        bomb.start();
        break;
    }
  }

  @Override
  public void show() {
    if (bomb != null) bomb.disarm();
    // super.show();
    // wait for loaded data before bomb ticks
    new Thread(
            new Runnable() {
              /*
              Runnable showDialog =
                  new Runnable() {
                    @Override
                    public void run() {
                      SwitchDialog.super.show();
                    }
                  };
              */
              @Override
              public void run() {
                while (!adapterDataSet || !layoutComplete) {
                  // we just wait to get data
                }
                // handler.post(showDialog); // need to be executed on ui thread
                bomb.start();
              }
            })
        .start();
    super.show();
  }

  @Override
  public void dismiss() {
    if (bomb != null) bomb.disarm();
    setShowClock(showClock);
    settingsSel.setVisibility(View.GONE);
    dimScreenMode.setVisibility(View.GONE);
    super.dismiss();
  }

  private void prepareDialog() {
    this.setCancelable(true);
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.setContentView(R.layout.activity_switch);
    this.getWindow()
        .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    this.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
    this.getWindow().setBackgroundDrawableResource(R.drawable.dialogbackground);

    /*
    if (forceLandscape) {
      WindowManager.LayoutParams params =
          new WindowManager.LayoutParams(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
      params.copyFrom(this.getWindow().getAttributes());
      params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
      this.getWindow().setAttributes(params);
    }
    */

    /* click on area outside of recycler */
    this.findViewById(R.id.constraintLayout)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                bomb.disarm();
                adapter.clearPosition();
                textClock.setVisibility(View.GONE);
                settingsSel.setVisibility(View.VISIBLE);
                dimScreenMode.setVisibility(View.VISIBLE);
              }
            });

    /* settings selection */
    settingsSel = this.findViewById(R.id.settingsSel);
    settingsSel.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            AppData app = new AppData(activity.getPackageName(), SettingsActivity.class.getName());
            callBackAppData(app);
          }
        });
    settingsSel.setVisibility(View.GONE);

    dimScreenMode = this.findViewById(R.id.dimScreenMode);
    dimScreenMode.setFlashListener(
        new FlashButton.FlashListener() {
          @Override
          public void onState(FlashButton.FlashEnum state) {
            switch (state) {
              case ON:
                sharedPreferencesHelper.setInteger("dimMode", AppSwitcherService.DIM_MODE_ON);
                break;
              case OFF:
                sharedPreferencesHelper.setInteger("dimMode", AppSwitcherService.DIM_MODE_OFF);
                break;
              case AUTO:
                int checkVal =
                    AppSwitcherApp.getInstance()
                        .checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
                if (checkVal == PackageManager.PERMISSION_GRANTED) {
                  sharedPreferencesHelper.setInteger("dimMode", AppSwitcherService.DIM_MODE_AUTO);
                } else {
                  // SharedPreferencesHelper.setInteger(context, "dimMode",
                  // AppSwitcherService.DIM_MODE_ON);
                  dimScreenMode.setState(FlashButton.FlashEnum.OFF);
                  sharedPreferencesHelper.setInteger("dimMode", AppSwitcherService.DIM_MODE_OFF);
                }
                break;
            }
          }
        });
    dimScreenMode.setVisibility(View.GONE);

    int mode = sharedPreferencesHelper.getInteger("dimMode");
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
    txtTitle = this.findViewById(R.id.titleTxt);

    textClock = this.findViewById(R.id.textClock);
    setShowClock(showClock);

    RecyclerView recyclerView = this.findViewById(R.id.activityRecycler);
    recyclerView.setHasFixedSize(true);
    recyclerView.setItemAnimator(new DefaultItemAnimator());

    linearLayoutManager =
        new LinearLayoutManager(
            AppSwitcherApp.getInstance(), LinearLayoutManager.HORIZONTAL, false) {
          // after the layout has finished drawing on the screen
          @Override
          public void onLayoutCompleted(RecyclerView.State state) {
            super.onLayoutCompleted(state);
            layoutComplete = true;
          }
        };
    recyclerView.setLayoutManager(linearLayoutManager);

    this.setCanceledOnTouchOutside(true);
    this.setOnCancelListener(
        new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            SwitchDialog.this.dismiss();
            listener.onResult(null);
          }
        });

    progressBar = (ProgressBar) this.findViewById(R.id.progressbarSw);

    recyclerView.setAdapter(adapter);
  }

  private class Bomb {
    private Thread bombThread;
    private Handler handler;
    private int wait = 2000;

    public Bomb(int wait) {
      this.handler = new Handler(Looper.getMainLooper());
      this.wait = wait;
    }

    public void disarm() {
      if (bombThread != null && bombThread.isAlive()) {
        bombThread.interrupt();
        // Log.i(TAG, "Stopped Log reader");
        bombThread = null;
      }
    }

    public void start() {
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
                  Log.i(TAG, "timer of SwitchDialog interrupted");
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
              if (Thread.interrupted()) {
                return;
              }

              handler.post(
                  new Runnable() {
                    @Override
                    public void run() {
                      AppData app = adapter.getCurrentApp();
                      if (app != null) callBackAppData(app);
                      // SwitchDialog.this.dismiss();
                    }
                  });
            }
          };

      if (bombThread != null && bombThread.isAlive()) {
        bombThread.interrupt();
        bombThread = null;
      }
      bombThread = new Thread(runnable);
      bombThread.start();
    }
  }
}
