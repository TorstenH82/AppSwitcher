package com.thf.AppSwitcher.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.thf.AppSwitcher.utils.Utils;

public class LteRecover implements Runnable {
  private static final String TAG = "AppSwitcherService";
  private Context context;
  private SharedPreferencesHelper sharedPreferencesHelper;

  public LteRecover(Context context) {
    this.context = context;
    this.sharedPreferencesHelper = new SharedPreferencesHelper(context);
  }

  @Override
  public void run() {
    int delay = sharedPreferencesHelper.getInteger("lterecoverDelay");
    int delayBtw = sharedPreferencesHelper.getInteger("lterecoverDelayBtw");
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Log.i(TAG, "recovery of LTE connection interrupted");
      return;
    }

    String netwType = getNetworkType();
    Log.d(TAG, "Current network type: " + netwType);
    if ("4G".equals(netwType)) {
      return;
    }

    try {
      Utils.sudoForResult(context, "stop vendor.ril-daemon-mtk");
      Log.d(TAG, "service 'vendor.ril-daemon-mtk' stopped");
      Utils.sudoForResult(context, "stop vendor.epdg_wod");
      Log.d(TAG, "service 'vendor.epdg_wod' stopped");
    } catch (Utils.SuCommandException ex) {
      Log.e(TAG, "error stopping services: " + ex.getMessage());
    }

    try {
      Thread.sleep(delayBtw);
    } catch (InterruptedException e) {
      Log.i(TAG, "recovery of LTE connection interrupted");
      return;
    }

    try {
      Utils.sudoForResult(context, "start vendor.ril-daemon-mtk");
      Log.d(TAG, "service 'vendor.ril-daemon-mtk' started");
      Utils.sudoForResult(context, "start vendor.epdg_wod");
      Log.d(TAG, "service 'vendor.epdg_wod' started");
    } catch (Utils.SuCommandException ex) {
      Log.e(TAG, "error starting services: " + ex.getMessage());
    }
  }

  public String getNetworkType() {
    ConnectivityManager mConnectivityManager =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo mInfo = mConnectivityManager.getActiveNetworkInfo();

    // If not connected, "-" will be displayed
    if (mInfo == null || !mInfo.isConnected()) return "-";

    // If Connected to Wifi
    if (ConnectivityManager.TYPE_WIFI == mInfo.getType()) return "WIFI";

    if (ConnectivityManager.TYPE_MOBILE == mInfo.getType()) {

      switch (mInfo.getSubtype()) {
        case TelephonyManager.NETWORK_TYPE_GPRS:
        case TelephonyManager.NETWORK_TYPE_EDGE:
        case TelephonyManager.NETWORK_TYPE_CDMA:
        case TelephonyManager.NETWORK_TYPE_1xRTT:
        case TelephonyManager.NETWORK_TYPE_IDEN:
        case TelephonyManager.NETWORK_TYPE_GSM:
          return "2G";

        case TelephonyManager.NETWORK_TYPE_UMTS:
        case TelephonyManager.NETWORK_TYPE_EVDO_0:
        case TelephonyManager.NETWORK_TYPE_EVDO_A:
        case TelephonyManager.NETWORK_TYPE_HSDPA:
        case TelephonyManager.NETWORK_TYPE_HSUPA:
        case TelephonyManager.NETWORK_TYPE_HSPA:
        case TelephonyManager.NETWORK_TYPE_EVDO_B:
        case TelephonyManager.NETWORK_TYPE_EHRPD:
        case TelephonyManager.NETWORK_TYPE_HSPAP:
        case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
          return "3G";

        case TelephonyManager.NETWORK_TYPE_LTE:
        case TelephonyManager.NETWORK_TYPE_IWLAN:
        case 19:
          return "4G";

        case TelephonyManager.NETWORK_TYPE_NR:
          return "5G";

        default:
          return mInfo.getSubtype() + "";
      }
    }
    return "unknown";
  }
}
