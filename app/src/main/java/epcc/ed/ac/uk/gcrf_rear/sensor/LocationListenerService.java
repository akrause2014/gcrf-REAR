package epcc.ed.ac.uk.gcrf_rear.sensor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import epcc.ed.ac.uk.gcrf_rear.REARApplication;
import epcc.ed.ac.uk.gcrf_rear.data.DataStore;

/**
 * Created by akrause on 02/08/2017.
 */

public class LocationListenerService extends Service implements LocationListener {

    private Location mCurrentLocation = null;
    private File locationFile;

    private final static Map<String, Integer> PROVIDERS = new HashMap<>();

    static {
        PROVIDERS.put(LocationManager.GPS_PROVIDER, 1);
        PROVIDERS.put(LocationManager.NETWORK_PROVIDER, 2);
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: {
                    stopSelf();
                    break;
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterListener();
        broadcastLocation(mCurrentLocation);
        storeLocation(mCurrentLocation);
//        Log.d("location", "unregistered listener");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startForeground(111, new Notification());
        registerListener();
//        Log.d("location listener", "registered listener");
        handler.sendEmptyMessageDelayed(1, 5 * 60 * 1000);

        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("location", "Location: " + location);
        if (isBetterLocation(location, mCurrentLocation)) {
            broadcastLocation(location);
            final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            mCurrentLocation = location;
            if (location.getAccuracy() < 10.0) {
                storeLocation(location);
                try {
                    locationManager.removeUpdates(this);
                } catch (SecurityException e) {
                    // check permissions
                }
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void broadcastLocation(Location location) {
        Intent i = new Intent("LOCATION_UPDATED");
        String text;
        if (location == null) {
            text = "No location information.";
        } else {
            text = "Location: (" + location.getLatitude() + "," + location.getLongitude()
                    + ")\n  Accuracy: " + location.getAccuracy()
                    + "\n  Provider: " + location.getProvider();
        }
        i.putExtra("status", text);
        sendBroadcast(i);
    }

    private void registerListener() {
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
//        Log.d("main", "registered location listener");
        mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    private void unregisterListener() {
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(this);
    }

    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isNewer = timeDelta > 0;

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private void storeLocation(Location location) {
        if (location != null) {
//            Log.d("location",
//                    "Time: " + new Date(location.getTime())
//                            + ", Lat/Lon: (" + location.getLatitude() + "," + location.getLongitude()
//                            + "), Accuracy: " + location.getAccuracy()
//                            + ", Provider: " + location.getProvider());
//            broadcastLocation(location);

            if (locationFile == null) {
                locationFile =
                        new File(REARApplication.getLocationDir(this),
                                "location-" + location.getTime() + ".dat");
            }
            try {
                DataOutputStream output = new DataOutputStream(
                        new FileOutputStream(locationFile, true));
                output.writeLong(location.getTime());
                Integer provider = PROVIDERS.get(location.getProvider());
                int p = (provider != null) ? provider : 0;
                output.writeInt(p);
                output.writeDouble(location.getLatitude());
                output.writeDouble(location.getLongitude());
                output.writeFloat(location.getAccuracy());
                output.close();
            } catch (IOException e) {
                Log.e("location", "failed to write location", e);
            }
        }
    }

}
