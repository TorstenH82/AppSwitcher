package com.thf.AppSwitcher.utils;

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
  private Location location;
  private String provider;
  private Criteria criteria;
  private int currentMode = 0;
  private Thread thread;
  private boolean autoEnabled = false;
  private static final String STATUS_SUNSET = "SUNSET";
  private static final String STATUS_SUNRISE = "SUNRISE";
  // private String status = "";

  private SunriseSunsetCallbacks listener;

  public SunriseSunset(Context context, SunriseSunsetCallbacks listener) {

    this.listener = listener;

    int checkVal =
        context.checkCallingOrSelfPermission(context.getString(R.string.permissionCoarseLocation));
    if (checkVal != PackageManager.PERMISSION_GRANTED) {
      listener.onMissingPermission();
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

    Log.i(TAG, "location provider: " + provider);

    // the last known location of this provider
    location = locationManager.getLastKnownLocation(provider);
    // mylistener = new MyLocationListener();

    if (location == null) {
      locationManager.requestSingleUpdate(
          provider,
          new LocationListener() {
            @Override
            public void onLocationChanged(Location loc) {
              location = loc;
              if (autoEnabled) enableAuto();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
              // deprecated
            }

            @Override
            public void onProviderDisabled(String provider) {
              
            }
          },
          null);
      // leads to the settings because there is no last known location
      // Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
      // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      // context.startActivity(intent);
    } else {
      Log.i(
          TAG,
          "Current location: lat: "
              + location.getLatitude()
              + " / lon: "
              + location.getLongitude());
      // enableAuto();
      // mylistener.onLocationChanged(location);

      // location updates: at least 1 meter and 200millsecs change
      // locationManager.requestLocationUpdates(provider, 200, 1, mylistener);
      // String a = "" + location.getLatitude();
      // Toast.makeText(getApplicationContext(), a, 222).show();
    }
  }

  public Location getLocation() {
    return location;
  }

  public interface SunriseSunsetCallbacks {
    public void onSunrise();

    public void onSunset();

    public void onMissingPermission();
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
    if (location == null) {
      return;
    }
    thread = new Thread(runnable);
    thread.start();
  }

  private Runnable runnable =
      new Runnable() {
        private String status = null;

        @Override
        public void run() {
          while (true) {
            Location loc = locationManager.getLastKnownLocation(provider);
            if (loc != null) {
              location = loc;
            }

            ZonedDateTime dateTime = ZonedDateTime.now(); // date, time and timezone of calculation
            // double lat, lng = // geolocation
            SunTimes times =
                SunTimes.compute()
                    .on(dateTime) // set a date
                    .at(location.getLatitude(), location.getLongitude()) // set a location
                    .execute(); // get the results
            Log.i(TAG, "Sunrise: " + times.getRise() + " / " + "Sunset: " + times.getSet());

            if (times.getSet().toEpochSecond() < times.getRise().toEpochSecond()) {
              Log.i(TAG, "based on sun times show: bright screen");
              if (listener != null && !STATUS_SUNRISE.equals(status)) status = STATUS_SUNRISE;
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
              if (listener != null && !STATUS_SUNSET.equals(status)) status = STATUS_SUNSET;
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
