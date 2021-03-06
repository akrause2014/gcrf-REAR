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
import android.os.Environment;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

import epcc.ed.ac.uk.gcrf_rear.data.AlarmReceiver;
import epcc.ed.ac.uk.gcrf_rear.data.DataPoint;
import epcc.ed.ac.uk.gcrf_rear.data.DataStore;
import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;

/**
 * Created by akrause on 14/11/2016.
 */

public class REARApplication extends Application implements SensorEventListener {

    private DatabaseThread mDatabase;

    public static File getStorageDir(Context context) {
        File root = new File("/storage/extSdCard");
        if (!root.exists() || !root.canWrite()) {
            root = context.getExternalFilesDir(null);
        }
        File dir = new File(root, "gcrfREAR");
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    public static long getUsableSpace() {
        File root = new File("/storage/extSdCard");
        if (root.exists()) {
            return root.getUsableSpace();
        }
        else return 0L;
    }

    public static File getDataDir(Context context) {
        return new File(getStorageDir(context), "rear");
    }

    public static File getMetaDir(Context context) {
        return new File(getStorageDir(context), "rear_meta");
    }

    public static File getBackupDir(Context context) {
        return new File(getStorageDir(context), "rear_backup");
    }

    public static File getLocationDir(Context context) {
        return new File(getStorageDir(context), "rear_location");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        getDataDir(this).mkdir();
        getMetaDir(this).mkdir();
        getBackupDir(this).mkdir();
        getLocationDir(this).mkdir();

        mDatabase = new DatabaseThread();
        mDatabase.setContext(this);
        mDatabase.start();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String value = settings.getString(getString(R.string.pref_key_file_length), null);
        int defaultDataSize = getResources().getInteger(R.integer.default_data_size);
        int dataSize = defaultDataSize;
        if (value != null) {
            try {
                dataSize = Integer.valueOf(value);
                if (dataSize <= 0) dataSize = defaultDataSize;
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
