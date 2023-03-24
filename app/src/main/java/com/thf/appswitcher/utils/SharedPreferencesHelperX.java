package com.thf.AppSwitcher.utils;

import android.content.Context;
import static android.content.Context.MODE_PRIVATE;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.text.TextUtilsCompat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SharedPreferencesHelperX {
	private static final String TAG = "AppSwitcherService";

	private static String value;
	private static String sDictAsString;
	private static Hashtable<String, AppData> dictHashtable;
	private static List<AppData> recentApps;

	public static void SaveDict(Context context, Hashtable ht, String key) {
		SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
		Gson gson = new Gson();
		java.lang.reflect.Type type = new TypeToken<Hashtable<String, AppData>>() {
		}.getType();
		String hashTableString = gson.toJson(ht, type);
		pSharedPref.edit().putString(key, hashTableString).apply();
	}

    
	public static Hashtable<String, AppData> LoadDict(Context context, String key) {

		Hashtable<String, AppData> loadedHt;
		SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
		String storedString = pSharedPref.getString(key, (new JsonObject()).toString());
		if (!TextUtils.equals(storedString, "{}")) {
			java.lang.reflect.Type type = new TypeToken<Hashtable<String, AppData>>() {
			}.getType();
			Gson gson = new Gson();
			loadedHt = gson.fromJson(storedString, type);
		} else {
			loadedHt = new Hashtable<String, AppData>();
		}
		return loadedHt;
	}
    
/*
	public static String getDictKeysAsString(Context context, String key) {
		Hashtable ht = LoadDict(context, key);
		//	Enumeration<String> enumeration = ht.keys().toString();
		Set<String> keys = ht.keySet();
		String partkey = "";
		for (String k : keys) {
			partkey = partkey + k + ";";
		}
		sDictAsString = partkey;
		return partkey;
	}
*/
	public static Hashtable<String, AppData> putValueForKey(Hashtable ht, String key, AppData value) {
		if (value == null) {
			if (ht.containsKey(key)) {
				ht.remove(key);
			}
		} else {
			ht.put(key, value);
		}
		return ht;
	}

	public static AppData getValueForKey(Context context, String key, String dictKey) {
		Hashtable ht = LoadDict(context, key);
		if (dictKey.equals("1st")) {
			if (!ht.isEmpty()) {
				return (AppData) ht.get(ht.keySet().toArray()[0]);
			} else {
				return null;
			}
		}
		if (ht.containsKey(dictKey)) {
			return (AppData) ht.get(dictKey);
		} else {
			return null;
		}
	}

	public static int getCountOfDict(Context context, String key) {
		Hashtable ht = LoadDict(context, key);
		return ht.keySet().toArray().length;
	}

	private static String getResStringId(Context context, String aString) {
		String packageName = context.getPackageName();
		int resId = context.getResources().getIdentifier(aString, "string", packageName);
		if (resId == 0) {
			return "";
		} else {
			return context.getString(resId);
		}
	}

	public static String getString(Context context, String key) {
		SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
		String value = pSharedPref.getString(key, "");
		if (TextUtils.equals(value.trim(), "")) {
			value = getResStringId(context, "pref_" + key);
		}
		return value;
	}

	/*
		public static void SaveInteger(Context context, Integer value, String key) {
			if (!TextUtils.equals(lastRequestedIntegerKey, key) || SharedPreferencesHelper.valueInt != value) {
				SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
				pSharedPref.edit().putInt(key, value).apply();
				lastRequestedIntegerKey = null;
			}
		}
	
		public static int LoadInteger(Context context, String key) {
			
			SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
			String stringRes = pSharedPref.getString(key, "");
			int valueInt;
			if (stringRes.equals("")) {
				stringRes = getResStringId(context, "pref_" + key);
				if (TextUtils.equals(stringRes, "")) {
					valueInt = 0;
				} else {
					valueInt = Integer.parseInt(stringRes);
				}
			} else {
				valueInt = Integer.parseInt(stringRes);
			}
			return valueInt;
		}
	
		public static void SaveBoolean(Context context, Boolean value, String key) {
			if (!TextUtils.equals(lastRequestedBooleanKey, key) || SharedPreferencesHelper.valueBool != value) {
				SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
				String boolString = value ? "true" : "false";
				pSharedPref.edit().putString(key, boolString).apply();
				Log.d(TAG, boolString);
				lastRequestedBooleanKey = null;
			}
		}
	*/

	public static int getInteger(Context context, String key) {
		SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
		int valueInt = pSharedPref.getInt(key, -99);
		if (valueInt == -99) {
			String stringRes = getResStringId(context, "pref_" + key);
			if (TextUtils.equals(stringRes, "")) {
				valueInt = 0;
			} else {
				valueInt = Integer.parseInt(stringRes);
			}
		}
		return valueInt;
	}

	public static void setInteger(Context context, String key, int valueInt) {
		SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
		pSharedPref.edit().putInt(key, valueInt).apply();
	}

	public static Boolean getBoolean(Context context, String key) {
		SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
		Boolean valueBool = pSharedPref.getBoolean(key, false);
		return valueBool;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static List<AppData> loadList(Context context, String key) {
		SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
		String storedString = pSharedPref.getString(key, (new JsonObject()).toString());
		//Log.e("AppStarterService", storedStringString);
		List<AppData> list;
		if (!TextUtils.equals(storedString, "{}")) {
			java.lang.reflect.Type type = new TypeToken<List<AppData>>() {
			}.getType();
			Gson gson = new Gson();
			list = gson.fromJson(storedString, type);
		} else {
			list = new ArrayList<AppData>();
		}
		return list;
	}

	public static void saveList(Context context, List list, String key) {
		SharedPreferences pSharedPref = context.getSharedPreferences("USERDATA", MODE_PRIVATE);
		Gson gson = new Gson();
		java.lang.reflect.Type type = new TypeToken<List<AppData>>() {
		}.getType();
		String hashTableString = gson.toJson(list, type);
		pSharedPref.edit().putString(key, hashTableString).apply();
	}

	public static void putIntoRecentsList(Context context, AppData appData) {
		if (recentApps == null)
			recentApps = loadList(context, "recentAppsList");

		//keep the list short
		if (recentApps.size() > 10) {
			recentApps.remove(recentApps.size() - 1); //remove first/oldest entry
		}
		//delete entries from list if package/activity was already collected
		//List<AppData> selectedList = loadList(context, "selected");
		Iterator<AppData> i = recentApps.iterator();
		while (i.hasNext()) {
			AppData s = i.next(); // must be called before you can call i.remove()
			if (s.getKey().equals(appData.getKey())) // || selectedList.indexOf(s) == -1)
				i.remove();
		}

		recentApps.add(0, appData);
		saveList(context, recentApps, "recentAppsList");
	}

	public static List<AppData> getRecentsList(Context context) {
		if (recentApps == null) {
			try {
				recentApps = loadList(context, "recentAppsList");

			} catch (Exception ignore) {
			}
		}
		return recentApps;
	}

	public static void putIntoList(Context context, AppData app, String listName) {
		List<AppData> list;
		list = loadList(context, listName);

		//don't allow to add an app as navi and media
		int idx = list.indexOf(app);
		if (idx != -1)
			list.remove(idx);

		list.add(app);
		saveList(context, list, listName);

	}

	public static void removeFromList(Context context, AppData appData, String listKey) {
		List<AppData> list;
		list = loadList(context, listKey);
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
			saveList(context, list, listKey);
		}
	}

}