package com.thf.AppSwitcher.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import com.thf.AppSwitcher.AppSwitcherApp;

public class AppData {

	private String packageName;
	private String name;
	private String category;
	private String list;
	//private Drawable icon;
	private Integer flags;
	private String activityName;
	private String activityDescription;
	private Integer sort;
	private String[] permissions;

	public String[] getPermissions() {
		return permissions;
	}

	public void setPermissions(String[] permissions) {
		this.permissions = permissions;
	}

	public String getActivityName() {
		return activityName;
	}

	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	public String getFullDescription() {
		if (activityDescription != null) {
			if (!activityDescription.equals(name)) {
				return name + " (" + activityDescription + ")";
			} else {
				return activityDescription;
			}
		} else {
			return name;
		}
	}

	public String getDescription() {
		if (activityDescription != null) {
			return activityDescription;
		} else {
			return name;
		}
	}

	public String getActivityDescription() {
		return activityDescription;
	}

	public void setActivityDescription(String activityDescription) {
		this.activityDescription = activityDescription;
	}

	public Integer getFlags() {
		return flags;
	}

	public void setFlags(Integer flags) {
		this.flags = flags;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getList() {
		return list;
	}

	public void setList(String list) {
		this.list = list;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public Drawable getIcon(Context context) {
		try {
            AppSwitcherApp mApplication = (AppSwitcherApp) context.getApplicationContext();
            return mApplication.getPackageManager().getActivityIcon(new ComponentName(this.getPackageName(), this.getActivityName()));
			//return context.getPackageManager().getActivityIcon(new ComponentName(this.getPackageName(), this.getActivityName()));
		} catch (Exception ex) {
			return null;
		}
	}

	public Integer getSort() {
		return sort;
	}

	public void setSort(Integer sort) {
		this.sort = sort;
	}

	/*
	public void setIcon(Drawable icon) {
		this.icon = icon;
	}
	
	public Object getObject() {
		return object;
	}
	
	public void setObject(Object object) {
		this.object = object;
	}
	*/
	public String getKey() {
		if (getCategory().equals("app")) {
			return getPackageName();
		} else {
			return getPackageName() + "/" + getActivityName();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AppData) {
			AppData p = (AppData) o;
			return this.getKey().equals(p.getKey());
		} else {
			return false;
		}
	}
}