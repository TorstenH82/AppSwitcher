package com.thf.AppSwitcher.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.thf.AppSwitcher.utils.Utils;
import java.io.IOException;

public class RestartSrv {
    private static final String TAG = "AppSwitcherService";

    public static class RestartSrvException extends Exception {
        public RestartSrvException(String message) {
            super(message);
        }
    }

    public static void restartService(Context context) {
        if (!"4G".equals(getNetworkType(context))) {
            try {
                Utils.execSuCommand(context, "setprop vendor.mtk.md1.status init");
                Log.d(TAG, "set status: vendor.mtk.md1.status=init");
                Thread.sleep(3000);
                Utils.execSuCommand(context, "setprop vendor.mtk.md1.status bootup");
                Log.d(TAG, "set status: vendor.mtk.md1.status=bootup");
                Thread.sleep(1000);
                Utils.execSuCommand(context, "setprop vendor.mtk.md1.status ready");
                Log.d(TAG, "set status: vendor.mtk.md1.status=ready");
                // Utils.execSuCommand(context, "stop vendor.epdg_wod");
                // Utils.execSuCommand(context, "start vendor.epdg_wod");
                // Log.d(TAG, "Restarted service vendor.epdg_wod");
            } catch (Utils.SuCommandException e) {
                Log.e(TAG, "SuCommandException: " + e.getMessage());
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException in restartService");
            }
        }
    }

    public static String getNetworkType(Context context) {
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
