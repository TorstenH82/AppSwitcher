package com.thf.AppSwitcher.utils;

import android.graphics.drawable.Drawable;
import com.thf.AppSwitcher.utils.AppData;

public class AppDataIcon extends AppData {
  private Drawable icon;

  public AppDataIcon() {}

  public AppDataIcon(AppData copy) {
    setPackageName(copy.getPackageName());
    setName(copy.getName());
    setCategory(copy.getCategory());
    setList(copy.getList());
    setFlags(copy.getFlags());
    setActivityName(copy.getActivityName());
    setActivityDescription(copy.getActivityDescription());
    setSort(copy.getSort());
    setPermissions(copy.getPermissions());
  }

  public void setIcon(Drawable icon) {
    this.icon = icon;
  }

  public Drawable getIcon() {
    return this.icon;
  }
}
