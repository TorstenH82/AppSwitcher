package com.thf.AppSwitcher;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.View;
import android.graphics.Color;
import android.view.WindowManager;
import android.widget.ImageView;

public class OverlayWindow {
  private static final String TAG = "AppSwitcherService";
  private Context context;
  private AppSwitcherApp mApplication;
  private WindowManager windowManager;
  private static ImageView a;
  private WindowManager.LayoutParams layoutParams;
  private boolean landscape = false;
  private boolean reShow = false;
  private int brightness;
  private OverlayMode overlayMode;

  public enum OverlayMode {
    OM_DIM,
    OM_TRANSPARENT,
    OM_KEEP
  }

  public OverlayWindow(Context context, int brightness) {
    this.context = context;
    this.brightness = brightness;
    mApplication = (AppSwitcherApp) context.getApplicationContext();

    windowManager = (WindowManager) context.getSystemService("window");
    if (a == null) {
      a = new ImageView(context);
      a.setBackgroundColor(Color.BLACK);
    }

    // setBrightness(brightness);

    /*
    layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
    layoutParams.height = -1;
    layoutParams.width = -1;
    layoutParams.flags = 3864;
    layoutParams.format = 1;
    layoutParams.gravity = 48;
    layoutParams.windowAnimations = 0;
    layoutParams.x = 0;
    layoutParams.y = 0;
    */

    layoutParams =
        new WindowManager.LayoutParams(
            // WindowManager.LayoutParams.WRAP_CONTENT,
            // WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
    layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
    // layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
  }

  public void setLandscape(boolean landscape) {

    if (landscape) {
      layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    } else {
      layoutParams.screenOrientation = -1;
    }

    if (this.landscape != landscape ) {
      if (OverlayMode.OM_DIM.equals(overlayMode) && mApplication.getOverlayVisibility()) {
        hide();
        show(OverlayMode.OM_DIM);
      } else if (landscape) {
        hide();
        show(OverlayMode.OM_TRANSPARENT);
      } else {
        hide(); // no need to show overlay if landscape is false and dimming not active
      }
    }

    this.landscape = landscape;
  }

  public void show(OverlayMode overlayMode) {
    if (this.overlayMode == null || !this.overlayMode.equals(overlayMode)) {
      switch (overlayMode) {
        case OM_DIM:
          this.overlayMode = overlayMode;
          setDimming();
          break;
        case OM_TRANSPARENT:
          this.overlayMode = overlayMode;
          setTransparent();
          break;
        case OM_KEEP:
          if (this.overlayMode == null) {
            Log.e(TAG, "keep overlay mode only possible if set");
            return;
          }
          break;
      }
    }
    if (!mApplication.getOverlayVisibility()) {
      try {
        windowManager.addView((View) a, (ViewGroup.LayoutParams) layoutParams);
      } catch (IllegalStateException ignore) {
      }
    }
    mApplication.setOverlayVisibility(true);
  }

  private void setDimming() {
    Integer brightness = this.brightness;
    int alpha = (int) (((100 - brightness.floatValue()) / 100) * 255);
    a.getBackground().setAlpha(alpha);
  }

  public void setBrightness(int brightness) {
    this.brightness = brightness;
    if (overlayMode != null && overlayMode.equals(OverlayMode.OM_DIM)) {
      setDimming();
    }
  }

  private void setTransparent() {
    a.getBackground().setAlpha(0);
  }

  public void hide() {
    if (mApplication.getOverlayVisibility()) {
      try {
        windowManager.removeView((View) a);
      } catch (IllegalArgumentException ignore) {
      }
    }
    mApplication.setOverlayVisibility(false);
  }

  public void hideTmp() {
    if (mApplication.getOverlayVisibility()) {
      hide();
      reShow = true;
    }
  }

  public void reShow() {
    if (reShow) {
      show(OverlayMode.OM_KEEP);
    }
  }
}
