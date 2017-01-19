package epcc.ed.ac.uk.gcrf_rear.sensor;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import epcc.ed.ac.uk.gcrf_rear.R;
import epcc.ed.ac.uk.gcrf_rear.REARApplication;
import epcc.ed.ac.uk.gcrf_rear.data.DataPoint;
import epcc.ed.ac.uk.gcrf_rear.data.DataStore;
import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;

/**
 * Created by akrause on 18/01/2017.
 */

public class SensorListenerService extends Service implements SensorEventListener{

    private SensorManager mSensorManager = null;
    private PowerManager.WakeLock mWakeLock = null;

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("receiver", "onReceive("+intent+")");

            if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    Log.d(SensorListenerService.class.getName(), "re-registering listener");
                    mSensorManager.unregisterListener(SensorListenerService.this);
                    registerListener();
                }
            };

            new Handler().postDelayed(runnable, 500);
        }
    };

    private void registerListener() {
        Sensor accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) {
            mSensorManager.registerListener(this, accel, getSamplingPeriod());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("sensor listener", "on create");
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        PowerManager manager =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SensorListenerService.class.getName());

        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
        mSensorManager.unregisterListener(SensorListenerService.this);
        mWakeLock.release();
        stopForeground(true);
        Log.d("sensor listener", "unregistered listener");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startForeground(111, new Notification());
        registerListener();
        Log.d("sensor listener", "registered listener");
        mWakeLock.acquire();

        return START_STICKY;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Message msg = new Message();
        msg.arg1 = DatabaseThread.SENSOR_MSG;
        DataStore.SensorType sensorType = DataStore.SensorType.valueOf(sensorEvent.sensor.getType());
        msg.obj = new DataPoint(sensorEvent.timestamp, sensorEvent.values, sensorType);
        ((REARApplication)getApplication()).getDatabase().mHandler.sendMessage(msg);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private static final int DEFAULT_SAMPLING_RATE = 50; // Hertz

    private int getRate() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String value = settings.getString(getResources().getString(R.string.pref_key_frequency), null);
        int rate = DEFAULT_SAMPLING_RATE;
        if (value != null) {
            try {
                rate = Integer.valueOf(value);
                if (rate <= 0) rate = DEFAULT_SAMPLING_RATE;
            }
            catch (NumberFormatException e) {
                // ignore
            }
        }
        return rate;
    }

    private int getSamplingPeriod() {
        int rate = getRate();
        return 1000000 / rate;
    }





}
