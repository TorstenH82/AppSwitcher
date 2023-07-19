package com.thf.AppSwitcher.utils;

import android.content.Context;
import static android.content.Context.MODE_PRIVATE;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SharedPreferencesHelper {
  private Context context;
  private SharedPreferences sharedPreferences;
  private static final String TAG = "AppSwitcherService";
  private static String value;
  private static String sDictAsString;
  // private static Hashtable<String, AppData> dictHashtable;
  private static List<AppData> recentApps;
  private static List<AppDataIcon> selectedApps;

  public SharedPreferencesHelper(Context context) {
    this.context = context;
    this.sharedPreferences = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
  }

  public SharedPreferencesHelper(
      Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
    this.context = context;
    this.sharedPreferences = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
    this.sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
  }

  public void unregisterOnSharedPreferenceChangeListener(
      SharedPreferences.OnSharedPreferenceChangeListener listener) {
    this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
  }

  private String getDefaultString(String key) {
    key = "pref_" + key;
    int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
    if (resId == 0) {
      return "";
    } else {
      return context.getString(resId);
    }
  }

  public String getString(String key) {
    return sharedPreferences.getString(key, getDefaultString(key));
  }

  private int getDefaultInt(String key) {
    key = "pref_" + key;
    int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
    if (resId == 0) {
      return -99;
    } else {
      return Integer.parseInt(context.getString(resId));
    }
  }

  public int getInteger(String key) {
    return sharedPreferences.getInt(key, getDefaultInt(key));
  }

  public void setInteger(String key, int valueInt) {
    sharedPreferences.edit().putInt(key, valueInt).apply();
  }

  private boolean getDefaultBool(String key) {
    key = "pref_" + key;
    int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
    if (resId == 0) {
      return false;
    } else {
      return Boolean.parseBoolean(context.getString(resId));
    }
  }

  public boolean getBoolean(String key) {
    return sharedPreferences.getBoolean(key, getDefaultBool(key));
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  public List<AppData> loadList(String key) {
    String storedString = sharedPreferences.getString(key, (new JsonObject()).toString());

    List<AppData> list;
    if (!TextUtils.equals(storedString, "{}")) {
      java.lang.reflect.Type type = new TypeToken<List<AppData>>() {}.getType();
      Gson gson = new Gson();
      list = gson.fromJson(storedString, type);
    } else {
      list = new ArrayList<AppData>();
    }
    return list;
  }

  public List<AppDataIcon> getSelected(boolean forceReload) {
    if (selectedApps != null && !forceReload) {
      return new ArrayList<AppDataIcon>(selectedApps);
    }

    Log.d(TAG, "read shared preferences");
    selectedApps = new ArrayList<AppDataIcon>();
    
    List<AppData> list = loadList("selected");
        
    Iterator<AppData> i = list.iterator();
    while (i.hasNext()) {
      AppData s = i.next(); // must be called before you can call i.remove()
      Drawable icon = s.getIcon(context);

      if (icon == null) {
        i.remove();
        // we could save the list after this
      } else {
        //icon.mutate();
        AppDataIcon appIcon = new AppDataIcon(s);
        appIcon.setIcon(icon);
        selectedApps.add(appIcon);
      }
    }

    return new ArrayList<AppDataIcon>(selectedApps);
  }

  public void saveList(List list, String key) {

    Gson gson = new Gson();
    java.lang.reflect.Type type = new TypeToken<List<AppData>>() {}.getType();
    String hashTableString = gson.toJson(list, type);
    sharedPreferences.edit().putString(key, hashTableString).apply();
  }

  public void putIntoRecentsList(AppData appData) {
    if (recentApps == null) recentApps = loadList("recentAppsList");

    // keep the list short
    if (recentApps.size() > 10) {
      recentApps.remove(recentApps.size() - 1); // remove first/oldest entry
    }
    // delete entries from list if package/activity was already collected
    // List<AppData> selectedList = loadList(context, "selected");
    Iterator<AppData> i = recentApps.iterator();
    while (i.hasNext()) {
      AppData s = i.next(); // must be called before you can call i.remove()
      if (s.getKey().equals(appData.getKey())) // || selectedList.indexOf(s) == -1)
      i.remove();
    }

    recentApps.add(0, appData);
    saveList(recentApps, "recentAppsList");
  }

  public List<AppData> getRecentsList() {
    if (recentApps == null) {
      try {
        recentApps = loadList("recentAppsList");

      } catch (Exception ignore) {
      }
    }
    return recentApps;
  }

  public void putIntoList(AppData app, String listName) {
    List<AppData> list;
    list = loadList(listName);

    // don't allow to add an app as navi and media
    int idx = list.indexOf(app);
    if (idx != -1) list.remove(idx);

    list.add(app);
    saveList(list, listName);
  }

  public void removeFromList(AppData appData, String listKey) {
    List<AppData> list;
    list = loadList(listKey);
    boolean removed = false;

    Iterator<AppData> i = list.iterator();
    while (i.hasNext()) {
      AppData s = i.next(); // must be called before you can call i.remove()
      if (s.equals(appData) && s.getList().equals(appData.getList())) {
        i.remove();
        removed = true;
      }
    }

    if (removed) {
      saveList(list, listKey);
    }
  }

  public static AppDataIcon getAppDataFromListByKey(final List<AppDataIcon> list, final String key) {
    if (list.stream().filter(o -> (o.getKey().equals(key))).findFirst().isPresent()) {
      return list.stream().filter(o -> o.getKey().equals(key)).collect(Collectors.toList()).get(0);
    } else {
      return null;
    }
  }

  public static boolean appDataListContainsKey(
      final List<AppData> list, final String key, final String listName) {
    return list.stream()
        .filter(
            o -> (o.getKey().equals(key)) && ((o.getList().equals(listName)) || listName == null))
        .findFirst()
        .isPresent();
  }

  public static boolean appDataListIconContainsKey(
      final List<AppDataIcon> list, final String key, final String listName) {
    return list.stream()
        .filter(
            o -> (o.getKey().equals(key)) && ((o.getList().equals(listName)) || listName == null))
        .findFirst()
        .isPresent();
  }
}
