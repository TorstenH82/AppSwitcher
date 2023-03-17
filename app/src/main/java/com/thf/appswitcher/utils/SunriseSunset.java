package com.thf.AppSwitcher.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.location.LocationListener;
import android.location.Location;
import android.location.Criteria;
import android.os.Handler;
import android.os.Looper;
import android.location.LocationManager;
import android.util.Log;
import com.thf.AppSwitcher.R;
import java.time.ZonedDateTime;
import org.shredzone.commons.suncalc.SunTimes;

public class SunriseSunset {
    private static final String TAG = "AppSwitcherService";
    private LocationManager locationManager;
    private static Location location;
    private String provider;
    private Criteria criteria;
    private int currentMode = 0;
    private Thread thread;
    private boolean autoEnabled = false;
    private static final String STATUS_SUNSET = "SUNSET";
    private static final String STATUS_SUNRISE = "SUNRISE";

    LocationListener locationListener =
            new LocationListener() {
                public void onLocationChanged(Location loc) {
                    Log.d(TAG, "location provider onLocationChanged");
                    location = loc;
                    Log.i(
                            TAG,
                            "New current location from provider "
                                    + location.getProvider()
                                    + ": lat: "
                                    + location.getLatitude()
                                    + " / lon: "
                                    + location.getLongitude());
                    if (autoEnabled) enableAuto();
                    locationManager.removeUpdates(this);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // deprecated
                }

                public void onProviderDisabled(String provider) {
                    Log.d(TAG, "location provider onProviderDisabled " + provider);
                }

                public void onProviderEnabled(String provider) {
                    Log.d(TAG, "location provider onProviderEnabled " + provider);
                }
            };

    // private String status = "";

    private SunriseSunsetCallbacks listener;

    public SunriseSunset(Context context, SunriseSunsetCallbacks listener) {

        this.listener = listener;

        int checkVal =
                context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (checkVal != PackageManager.PERMISSION_GRANTED) {
            listener.onMissingPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            return;
        }

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // Define the criteria how to select the location provider
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE); // default

        // user defines the criteria
        criteria.setCostAllowed(false);
        // get the best provider depending on the criteria
        provider = locationManager.getBestProvider(criteria, false);

        Log.i(TAG, "Location provider: " + provider);

        // the last known location of this provider
        Location loc = locationManager.getLastKnownLocation(provider);
        if (loc != null) location = loc;
        if (location != null) {
            Log.i(
                    TAG,
                    "Last known location from "
                            + provider
                            + ": lat: "
                            + location.getLatitude()
                            + " / lon: "
                            + location.getLongitude());
        }

        if (location == null && !LocationManager.GPS_PROVIDER.equals(provider)) {

            checkVal =
                    context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (checkVal != PackageManager.PERMISSION_GRANTED) {
                listener.onMissingPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }

            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location != null) {
                Log.i(
                        TAG,
                        "Last known location from "
                                + LocationManager.GPS_PROVIDER
                                + ": lat: "
                                + location.getLatitude()
                                + " / lon: "
                                + location.getLongitude());
            }
        }

        if (location == null) {
            Log.d(
                    TAG,
                    "Location provider '"
                            + provider
                            + "' enabled: "
                            + locationManager.isProviderEnabled(provider));
            locationManager.requestLocationUpdates(provider, 0, 0, locationListener);

            if (!LocationManager.GPS_PROVIDER.equals(provider)) {
                Log.d(
                        TAG,
                        "Location provider '"
                                + LocationManager.GPS_PROVIDER
                                + "' enabled: "
                                + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    public Location getLocation() {
        return location;
    }

    public interface SunriseSunsetCallbacks {
        public void onSunrise();

        public void onSunset();

        public void onMissingPermission(String permission);
    }

    public void disableAuto() {
        autoEnabled = false;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    public void enableAuto() {
        autoEnabled = true;

        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }

        thread = new Thread(runnable);
        thread.start();
        Log.i(TAG, "(Re-)started SunriseSunset");
    }

    private Runnable runnable =
            new Runnable() {
                private String status = null;

                @Override
                public void run() {
                    while (true) {
                        Location loc =
                                getLocation(); // locationManager.getLastKnownLocation(provider);

                        if (loc == null) {
                            Log.i(TAG, "no location - retry in 5 seconds...");
                            try {
                                Thread.sleep(5 * 1000);
                            } catch (InterruptedException ex) {
                                Log.i(TAG, "interrupted SunriseSunset");
                                return;
                            }
                            continue;
                        }

                        ZonedDateTime dateTime =
                                ZonedDateTime.now(); // date, time and timezone of calculation
                        // double lat, lng = // geolocation
                        SunTimes times =
                                SunTimes.compute()
                                        .on(dateTime) // set a date
                                        .at(
                                                loc.getLatitude(),
                                                loc.getLongitude()) // set a location
                                        .execute(); // get the results
                        Log.i(
                                TAG,
                                "Sunrise: "
                                        + times.getRise()
                                        + " / "
                                        + "Sunset: "
                                        + times.getSet());

                        if (times.getSet().toEpochSecond() < times.getRise().toEpochSecond()) {
                            Log.i(TAG, "based on sun times show: bright screen");
                            if (listener != null && !STATUS_SUNRISE.equals(status))
                                status = STATUS_SUNRISE;
                            new Handler(Looper.getMainLooper())
                                    .post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    listener.onSunrise();
                                                }
                                            });
                        } else {
                            Log.i(TAG, "based on sun times show: dark screen");
                            if (listener != null && !STATUS_SUNSET.equals(status))
                                status = STATUS_SUNSET;
                            new Handler(Looper.getMainLooper())
                                    .post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    listener.onSunset();
                                                }
                                            });
                        }

                        try {
                            Thread.sleep(5 * 60 * 1000);
                        } catch (InterruptedException ex) {
                            Log.i(TAG, "interrupted SunriseSunset");
                            return;
                        }
                    }
                }
            };
}
