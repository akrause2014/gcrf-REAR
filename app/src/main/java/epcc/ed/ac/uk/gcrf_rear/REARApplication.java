package epcc.ed.ac.uk.gcrf_rear;

import android.app.Application;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.File;

import epcc.ed.ac.uk.gcrf_rear.data.DataPoint;
import epcc.ed.ac.uk.gcrf_rear.data.DataStore;
import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;

/**
 * Created by akrause on 14/11/2016.
 */

public class REARApplication extends Application implements SensorEventListener, LocationListener {

    private DatabaseThread mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        File dir = new File(getExternalFilesDir(null), "rear");
        dir.mkdir();
        mDatabase = new DatabaseThread();
        mDatabase.setContext(this);
        mDatabase.start();
    }

    public DatabaseThread getDatabase() {
        return mDatabase;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Message msg = new Message();
        msg.arg1 = DatabaseThread.SENSOR_MSG;
        DataStore.SensorType sensorType = DataStore.SensorType.valueOf(sensorEvent.sensor.getType());
        long currentTime = System.currentTimeMillis();
        msg.obj = new DataPoint(currentTime, sensorEvent.values, sensorType);
        mDatabase.mHandler.sendMessage(msg);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (location.getAccuracy() < 10.0) {
            try {
                locationManager.removeUpdates(this);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 25, this);
            }
            catch (SecurityException e) {
                // check permissions
            }
            Message msg = new Message();
            msg.arg1 = DatabaseThread.LOCATION_MSG;
            msg.obj = location;
            mDatabase.mHandler.sendMessage(msg);
        }
        else {
            try {
                locationManager.removeUpdates(this);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, this);
            }
            catch (SecurityException e) {
                // check permissions
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

}
