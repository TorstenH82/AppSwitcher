package com.thf.AppSwitcher.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.thf.AppSwitcher.BuildConfig;
import com.thf.AppSwitcher.R;
import com.thf.AppSwitcher.utils.Utils.SuCommandException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    private static final String TAG = "AppSwitcherService";

    public interface UtilCallbacks {
        public void onException(Throwable e);
    }

    public static class SuCommandException extends Exception {
        public SuCommandException(String message) {
            super(message);
        }
    }

    public static class SysPropException extends Exception {
        public SysPropException(String message) {
            super(message);
        }
    }

    public class FileReadModException extends Exception {
        public FileReadModException(String message) {
            super(message);
        }
    }

    public static class SetAutostartException extends Exception {
        public SetAutostartException(String message) {
            super(message);
        }
    }

    public static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static String getSystemProperty(String key) throws SysPropException {
        String value = null;

        try {
            value =
                    (String)
                            Class.forName("android.os.SystemProperties")
                                    .getMethod("get", String.class)
                                    .invoke(null, key);
        } catch (Exception e) {
            throw new SysPropException(
                    "Error getting system property \"" + key + "\": " + e.getMessage());
        }
        return value;
    }

    public static void enableDisableDuraspeed(Context context, boolean enable)
            throws SysPropException {
        String commandDuraspeedApp = "";
        String commandDuraspeedSupport = "";
        if (enable) {
            commandDuraspeedApp = context.getString(R.string.commandEnableDuraspeedApp);
            commandDuraspeedSupport = context.getString(R.string.commandEnableDuraspeedSupport);
        } else {
            commandDuraspeedApp = context.getString(R.string.commandDisableDuraspeedApp);
            commandDuraspeedSupport = context.getString(R.string.commandDisableDuraspeedSupport);
        }
        try {
            sudoForResult(context, commandDuraspeedApp);
        } catch (SuCommandException e) {
            Log.e(
                    TAG,
                    "Error executing command: \""
                            + commandDuraspeedApp
                            + "\" Exception:"
                            + e.getMessage());
            throw new SysPropException(
                    "Error executing command: \""
                            + commandDuraspeedApp
                            + "\" Exception:"
                            + e.getMessage());
        }

        try {
            sudoForResult(context, commandDuraspeedSupport);
        } catch (SuCommandException e) {
            Log.e(
                    TAG,
                    "Error executing command: \""
                            + commandDuraspeedSupport
                            + "\" Exception:"
                            + e.getMessage());
            throw new SysPropException(
                    "Error executing command: \""
                            + commandDuraspeedSupport
                            + "\" Exception:"
                            + e.getMessage());
        }
    }

    public static Boolean isDuraspeedEnabled(Context context) throws SysPropException {
        try {
            String prop = getSystemProperty(context.getString(R.string.duraspeedCheckProperty));
            if (prop == null) prop = "";
            switch (prop) {
                case "0":
                    return false;
                case "1":
                    return true;
                default:
                    return null;
            }
        } catch (SysPropException e) {
            throw new SysPropException(e.getMessage());
        }
    }

    public static void enableDisableNaviMainActivity(
            Context context, boolean disable, UtilCallbacks utilCallbacks) {
        String command =
                "pm "
                        + (disable ? "disable" : "enable")
                        + " "
                        + context.getString(R.string.naviActivity);
        UtilCallbacks listener = utilCallbacks;

        Thread thread =
                new Thread(
                        new Runnable() {
                            public void run() {
                                try {
                                    sudoForResult(context, command);
                                } catch (SuCommandException e) {
                                    throw new RuntimeException(
                                            "Error executing command: \""
                                                    + command
                                                    + "\" Exception: "
                                                    + e.getMessage());
                                }
                            }
                        });

        thread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread th, Throwable e) {
                        Log.e(
                                TAG,
                                "Error executing command: \""
                                        + command
                                        + "\" Exception: "
                                        + e.getMessage());
                        new Handler(Looper.getMainLooper())
                                .post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.onException(e);
                                            }
                                        });
                    }
                });
        thread.start();
    }

    public static void enableDisableHideActivity(
            Context context, boolean disable, UtilCallbacks utilCallbacks) {
        String command =
                "pm "
                        + (disable ? "disable" : "enable")
                        + " "
                        + context.getString(R.string.hideActivity);
        UtilCallbacks listener = utilCallbacks;

        Thread thread =
                new Thread(
                        new Runnable() {
                            public void run() {
                                try {
                                    sudoForResult(context, command);
                                } catch (SuCommandException e) {
                                    throw new RuntimeException(
                                            "Error executing command: \""
                                                    + command
                                                    + "\" Exception: "
                                                    + e.getMessage());
                                }
                            }
                        });

        thread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread th, Throwable e) {
                        Log.e(
                                TAG,
                                "Error executing command: \""
                                        + command
                                        + "\" Exception: "
                                        + e.getMessage());
                        new Handler(Looper.getMainLooper())
                                .post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.onException(e);
                                            }
                                        });
                    }
                });
        thread.start();
    }

    public static void enableService(
            Context context, String pckge, String srv, UtilCallbacks utilCallbacks) {
        UtilCallbacks listener = utilCallbacks;
        Thread thread =
                new Thread(
                        new Runnable() {
                            public void run() {
                                Boolean enabled = false;
                                String fQService = pckge + "/" + srv;
                                try {
                                    String enabledServices =
                                            Settings.Secure.getString(
                                                    context.getContentResolver(),
                                                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

                                    if (enabledServices == null || "".equals(enabledServices)) {
                                        Settings.Secure.putString(
                                                context.getContentResolver(),
                                                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                                                fQService);
                                        enabled = true;
                                    } else if (!enabledServices.contains(pckge)) {
                                        Settings.Secure.putString(
                                                context.getContentResolver(),
                                                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                                                enabledServices + ":" + fQService);
                                        enabled = true;
                                    }
                                } catch (Exception ex) {
                                    throw new RuntimeException(
                                            "Error enabling service: \""
                                                    + srv
                                                    + "\" Exception: "
                                                    + ex.getMessage());
                                }
                            }
                        });

        thread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread th, Throwable e) {
                        Log.e(
                                TAG,
                                "Error enabling service: \""
                                        + srv
                                        + "\" Exception: "
                                        + e.getMessage());
                        new Handler(Looper.getMainLooper())
                                .post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.onException(e);
                                            }
                                        });
                    }
                });
        thread.start();
    }

    public static int getDarkMode(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "ui_night_mode", 1);
    }

    public static void setDarkMode(Context context, Boolean dark) {
        if (dark) {
            Settings.Secure.putInt(context.getContentResolver(), "ui_night_mode", 2);
        } else {
            Settings.Secure.putInt(context.getContentResolver(), "ui_night_mode", 1);
        }
    }

    public static void startAutomateFlow(Context context, String uri, UtilCallbacks utilCallbacks) {
        UtilCallbacks listener = utilCallbacks;
        Thread thread =
                new Thread(
                        new Runnable() {
                            public void run() {
                                try {
                                    Intent i =
                                            new Intent(
                                                    context.getString(
                                                            R.string.automateFlowStartIntent),
                                                    Uri.parse(uri));
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(i);
                                } catch (Exception ex) {
                                    throw new RuntimeException(
                                            "Error starting Automate Flow: " + ex.getMessage());
                                }
                            }
                        });

        thread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread th, Throwable e) {
                        Log.e(TAG, "Error starting Automate Flow: " + e.getMessage());
                        new Handler(Looper.getMainLooper())
                                .post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.onException(e);
                                            }
                                        });
                    }
                });
        thread.start();
    }

    public static void selfAuthorizeSecureSettings(Context context) throws SuCommandException {

        String command = context.getString(R.string.commandSecureSettings);
        sudoForResult(context, command);
    }

    public static void selfAuthorizeReadLogs(Context context) throws SuCommandException {

        String command = context.getString(R.string.commandReadLogs);
        sudoForResult(context, command);
    }

    public static String getSetFullscreenFlag(Context context, String newValue)
            throws FileReadModException {
        String returnValue = "";
        boolean modification = false;
        boolean found = false;
        if (newValue != null) {
            /*
            if (!"825X_Pro".equals(android.os.Build.DEVICE)) {
                throw new Utils().new FileReadModException("This device is not a 825X_Pro");
            }
            */
            newValue = newValue.trim();
        }
        try {
            // copy file from /vendor folder and change permission
            String copyCommand =
                    String.format(
                            context.getString(R.string.commandCopy),
                            "/vendor/build.prop",
                            context.getFilesDir() + "/build.prop");
            sudoForResult(context, copyCommand);
            String chmodCommand =
                    String.format(
                            context.getString(R.string.commandChmod777),
                            context.getFilesDir() + "/build.prop");
            sudoForResult(context, chmodCommand);

            File file = new File(context.getFilesDir() + "/build.prop");
            File temp = File.createTempFile("build", ".tmp");

            BufferedReader br =
                    new BufferedReader(new FileReader(context.getFilesDir() + "/build.prop"));
            PrintWriter pw = new PrintWriter(new FileWriter(temp));
            String line;

            while ((line = br.readLine()) != null) {
                boolean printLine = true;
                if (line.contains("forfan.force_direct")) {
                    found = true;
                    returnValue = line.split("=")[1].trim();
                    if (newValue == null) {
                        // we have the value and no need to change the file
                        break;
                    } else if (newValue.equals(returnValue)) {
                        // nothing to change - current value is new value
                        break;
                    } else {
                        // value need to be changed - write new entry and skip current line
                        pw.println("forfan.force_direct=" + newValue);
                        printLine = false;
                        modification = true;
                    }
                }
                // write the line into temp file build.tmp
                if (printLine) pw.println(line);
            }

            if (!found && newValue != null) {
                // entry not in file - we have to add it
                pw.println("forfan.force_direct=" + newValue);
                pw.println("");
                modification = true;
                returnValue = "1";
            }

            br.close();
            pw.close();

            file.delete();
            temp.renameTo(file);

            if (modification) {
                /*
                String[] rootCmds =
                        new String[] {
                            "echo 0 > /sys/kernel/debug/mmc0/sw_wp_en",
                            "mount -o remount,rw -t ext4 /dev/root /",
                            "mount -o remount,rw -t ext4 /dev/block/platform/bootdevice/by-name/vendor /vendor"
                        };

                String[] unRootCmds = new String[]{"chmod 0751 /system", "mount -o ro,remount /system";
                */

                // file was modified - replace original
                /*
                String remountCommand = context.getString(R.string.commandRemount1);
                try {
                    execSuCommand(context, remountCommand);
                } catch (SuCommandException e) {
                    Log.w(TAG, "Error ignored on executing remount command: " + e.getMessage());
                }
                */

                String remountCommand = context.getString(R.string.commandRemountMtk);
                sudoForResult(context, remountCommand);

                // copy file to vendors folder and change permission

                copyCommand =
                        String.format(
                                context.getString(R.string.commandCopy),
                                context.getFilesDir() + "/build.prop",
                                "/vendor/build.prop");
                sudoForResult(context, copyCommand);
                chmodCommand =
                        String.format(
                                context.getString(R.string.commandChmod644), "/vendor/build.prop");
                sudoForResult(context, chmodCommand);

                returnValue = newValue;
            }

            temp.delete();
            return returnValue;
        } catch (IOException e) {
            throw new Utils().new FileReadModException("IOException: " + e.getMessage());
        } catch (SuCommandException e) {
            throw new Utils().new FileReadModException("SuCommandException: " + e.getMessage());
        }
    }

    public static String sudoForResult(Context context, String command) throws SuCommandException {

        if (command == null || "".equals(command)) {
            throw new SuCommandException("Command is empty");
        }

        String[] commandArr = {context.getString(R.string.su), command};
        String output = "";
        try {
            Process exec = Runtime.getRuntime().exec(context.getString(R.string.su));
            DataOutputStream dataOutputStream = new DataOutputStream(exec.getOutputStream());

            dataOutputStream.writeBytes(command + "\n");
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();

            String line;
            BufferedReader in = new BufferedReader(new InputStreamReader(exec.getInputStream()));
            Thread.sleep(10);

            while ((line = in.readLine()) != null) {
                if ("".equals(command)) {
                    output = line;
                } else {
                    output += "\n" + line;
                }
            }

            BufferedReader er = new BufferedReader(new InputStreamReader(exec.getErrorStream()));
            Thread.sleep(10);
            while ((line = er.readLine()) != null) {
                if ("".equals(command)) {
                    output = line;
                } else {
                    output += "\n" + line;
                }
            }

            /* Clean-up */
            exec.waitFor();
        } catch (IOException ex) {
            throw new SuCommandException("IOException: " + ex.getMessage());
        } catch (InterruptedException e) {
            throw new SuCommandException("InterruptedException: " + e.getMessage());
        }

        return output;
    }

    

    

    public static void enableAutostart(Context context, boolean enable)
            throws SetAutostartException {
        try {
            boolean added = false;
            String addProp =
                    "P "
                            + context.getPackageName()
                            + "#A "
                            + context.getPackageName()
                            + ".WakeUpActivity"
                            + "#T 2"
                            + "#I 5";

            String prop = getSystemProperty("sys.qb.startapp_onresume");
            if (prop == null) prop = "";

            if (!enable && !prop.contains(context.getPackageName())) return;

            if (enable && addProp.equals(prop)) return;

            String newProp = "";
            if (enable) newProp = addProp;

            sudoForResult(
                    context,
                    String.format("setprop %s \"%s\"", "sys.qb.startapp_onresume", newProp));
        } catch (SysPropException e) {
            throw new SetAutostartException("SysPropException: " + e.getMessage());
        } catch (SuCommandException e) {
            throw new SetAutostartException("SuCommandException: " + e.getMessage());
        }
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
