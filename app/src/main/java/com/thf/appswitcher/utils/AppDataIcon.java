package com.thf.appswitcher.utils;
import android.graphics.drawable.Drawable;
import com.thf.AppSwitcher.utils.AppData;

public class AppDataIcon extends AppData {
    private Drawable icon;
    
    public void setIcon(Drawable icon) {
		this.icon = icon;
	}
    
    public Drawable getIcon() {
		return this.icon;
	}
}
