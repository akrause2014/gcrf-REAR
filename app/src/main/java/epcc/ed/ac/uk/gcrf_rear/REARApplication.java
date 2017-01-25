package epcc.ed.ac.uk.gcrf_rear;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

import epcc.ed.ac.uk.gcrf_rear.data.AlarmReceiver;
import epcc.ed.ac.uk.gcrf_rear.data.DataPoint;
import epcc.ed.ac.uk.gcrf_rear.data.DataStore;
import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;

/**
 * Created by akrause on 14/11/2016.
 */

public class REARApplication extends Application implements SensorEventListener, LocationListener {

    private DatabaseThread mDatabase;
    private Location mCurrentLocation = null;
    private long mLocationUpdates;

    @Override
    public void onCreate() {
        super.onCreate();
        File dir = new File(getExternalFilesDir(null), "rear");
        dir.mkdir();
        File metadir = new File(getExternalFilesDir(null), "rear_meta");
        metadir.mkdir();
        mDatabase = new DatabaseThread();
        mDatabase.setContext(this);
        mDatabase.start();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String value = settings.getString(getString(R.string.pref_key_file_length), null);
        int dataSize = 1;
        if (value != null) {
            try {
                dataSize = Integer.valueOf(value);
                if (dataSize <= 0) dataSize = 1;
            }
            catch (NumberFormatException e) {
                // ignore
            }
        }
        mDatabase.setDataSize(dataSize);
        scheduleDataUpload();
    }

    public DatabaseThread getDatabase() {
        return mDatabase;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Message msg = new Message();
        msg.arg1 = DatabaseThread.SENSOR_MSG;
        DataStore.SensorType sensorType = DataStore.SensorType.valueOf(sensorEvent.sensor.getType());
        msg.obj = new DataPoint(sensorEvent.timestamp, sensorEvent.values, sensorType);
        mDatabase.mHandler.sendMessage(msg);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Log.d("location", "Location: " + location);
        if (isBetterLocation(location, mCurrentLocation)) {
            mCurrentLocation = location;
            if (location.getAccuracy() < 10.0) {
                try {
                    mLocationUpdates = 5000;
                    locationManager.removeUpdates(this);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mLocationUpdates, 25, this);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mLocationUpdates, 25, this);
                } catch (SecurityException e) {
                    // check permissions
                }
                Message msg = new Message();
                msg.arg1 = DatabaseThread.LOCATION_MSG;
                msg.obj = location;
                mDatabase.mHandler.sendMessage(msg);
            }
            else if (mLocationUpdates > 100) {
                try {
                    mLocationUpdates = 100;
                    locationManager.removeUpdates(this);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mLocationUpdates, 25, this);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mLocationUpdates, 25, this);
                } catch (SecurityException e) {
                    // check permissions
                }
            }
        }
    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    public void scheduleDataUpload()
    {
        String value = PreferenceManager.getDefaultSharedPreferences(this).getString(
               getString(R.string.pref_key_upload_period), null);
        int period = 60; // default is 60 minutes
        if (value != null) {
            try {
                period = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                // ignore and use default value
            }
        }
        scheduleDataUpload(period);
    }
    public void scheduleDataUpload(int p)
    {
        boolean isActive = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                getString(R.string.pref_key_upload_active), false);

        // cancel the alarm intent
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent);

        // data upload is off
        if (!isActive) {
            Log.d("application", "automatic data upload off");
            return;
        }

        // data upload is on - schedule the new data period
        int defaultPeriod = 60; // minutes
        int period = p;
        if (period <= 0) {
            period = defaultPeriod;
        }
        period = period*60*1000; // convert from minutes to milliseconds

        long time = SystemClock.elapsedRealtime(); // + period;

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, period,
                PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        Log.d("application", "set automatic upload delay to " + period/60000 + " minutes");
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
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

}
