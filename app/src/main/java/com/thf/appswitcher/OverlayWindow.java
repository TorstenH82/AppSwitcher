package com.thf.AppSwitcher;

import android.content.Context;
import android.view.ViewGroup;
import android.view.View;
import android.graphics.Color;
import android.view.WindowManager;
import android.widget.ImageView;

public class OverlayWindow {
	private Context context;
	private AppSwitcherApp mApplication;
	private WindowManager windowManager;
	private static ImageView a;
	private WindowManager.LayoutParams layoutParams;

	public OverlayWindow(Context context, int brightness) {
		this.context = context;
		mApplication = (AppSwitcherApp) context.getApplicationContext();

		windowManager = (WindowManager) context.getSystemService("window");
		if (a == null) {
			a = new ImageView(context);
			a.setBackgroundColor(Color.BLACK);
		}

		setBrightness(brightness);

		layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
		layoutParams.height = -1;
		layoutParams.width = -1;
		layoutParams.flags = 3864;
		layoutParams.format = 1;
		layoutParams.gravity = 48;
		layoutParams.windowAnimations = 0;
		layoutParams.x = 0;
		layoutParams.y = 0;
	}

	public void show() {
		if (!mApplication.getOverlayVisibility()) {
			try {
				windowManager.addView((View) a, (ViewGroup.LayoutParams) layoutParams);
			} catch (IllegalStateException ignore) {
			}
		}
		mApplication.setOverlayVisibility(true);
		 
	}

	public void setBrightness(Integer brightness) {
		int alpha = (int) (((100 - brightness.floatValue()) / 100) * 255);
		a.getBackground().setAlpha(alpha);
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

	 

}