package epcc.ed.ac.uk.gcrf_rear;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import epcc.ed.ac.uk.gcrf_rear.data.DataPoint;
import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;
import epcc.ed.ac.uk.gcrf_rear.view.DataSurfaceView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private DatabaseThread mDatabase;
    private DataSurfaceView mDataView;
    private SensorManager mSensorManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = new DatabaseThread();
        mDataView = (DataSurfaceView)findViewById(R.id.data_view_id);
        mDatabase.start();
        mDataView.setDatabaseThread(mDatabase);

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    Sensor senAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    if (senAccelerometer != null) {
                        // Register accelerometer listener with sampling rate 100Hz
                        mSensorManager.registerListener(MainActivity.this, senAccelerometer, 10);
                    }
                    Sensor senGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                    if (senGyroscope != null){
                        // Register gyroscope listener with sampling rate 100Hz
                        mSensorManager.registerListener(MainActivity.this, senGyroscope, 10);
                    }
                    Sensor senMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                    if (senMagneticField != null){
                        // Register magnetic field listener with sampling rate 100Hz
                        mSensorManager.registerListener(MainActivity.this, senMagneticField, 10);
                    }
                } else {
                    mSensorManager.unregisterListener(MainActivity.this);
                }
            }
        });

        ToggleButton displayToggle = (ToggleButton) findViewById(R.id.displayToggleButton);
        displayToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDatabase.setDisplayOn(isChecked);
            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Message msg = new Message();
            msg.obj = new DataPoint(sensorEvent.timestamp, sensorEvent.values, sensorEvent.sensor.getType());
            mDatabase.mHandler.sendEmptyMessage(1);
            mDatabase.mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_upload_data:
                Log.d("menu", "Upload data selected");
                return true;
            case R.id.menu_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
