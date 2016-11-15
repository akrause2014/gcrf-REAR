package epcc.ed.ac.uk.gcrf_rear;

import android.app.Application;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Message;
import android.util.Log;

import java.io.File;

import epcc.ed.ac.uk.gcrf_rear.data.DataPoint;
import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;

/**
 * Created by akrause on 14/11/2016.
 */

public class REARApplication extends Application implements SensorEventListener {

    private DatabaseThread mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("oncreate", "database: " + mDatabase);
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
//        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        Message msg = new Message();
        msg.arg1 = DatabaseThread.SENSOR_MSG;
        msg.obj = new DataPoint(sensorEvent.timestamp, sensorEvent.values, sensorEvent.sensor.getType());
        mDatabase.mHandler.sendMessage(msg);
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
