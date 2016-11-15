package epcc.ed.ac.uk.gcrf_rear;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;
import epcc.ed.ac.uk.gcrf_rear.view.DataSurfaceView;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private DatabaseThread mDatabase;
    private DataSurfaceView mDataView;
    private LocationManager mLocationManager;
    private int mSamplingRate = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDatabase = ((REARApplication)getApplication()).getDatabase();
        mDataView = (DataSurfaceView) findViewById(R.id.data_view_id);
        mDataView.setDatabaseThread(mDatabase);

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    Sensor senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    if (senAccelerometer != null) {
                        // Register accelerometer listener with sampling rate 100Hz
                        sensorManager.registerListener((SensorEventListener) getApplication(), senAccelerometer, mSamplingRate);
                        Log.d("main", "registered listener for accelerometer");
                    }
                    Sensor senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                    if (senGyroscope != null) {
                        // Register gyroscope listener with sampling rate 100Hz
                        sensorManager.registerListener((SensorEventListener) getApplication(), senGyroscope, mSamplingRate);
                        Log.d("main", "registered listener for gyroscope");
                    }
                    Sensor senMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                    if (senMagneticField != null) {
                        // Register magnetic field listener with sampling rate 100Hz
                        sensorManager.registerListener((SensorEventListener) getApplication(), senMagneticField, mSamplingRate);
                        Log.d("main", "registered listener for magnetic field");
                    }
//                    mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//                    try {
//                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0l, 0f, MainActivity.this);
//                    }
//                    catch (SecurityException e) {
//                        // check permissions
//                    }

                } else {
                    SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    sensorManager.unregisterListener((SensorEventListener) getApplication());
                    mDatabase.close();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_upload_data: {
                Log.d("menu", "Upload data selected");
                Intent intent = new Intent(this, UploadDataActivity.class);
                this.startActivity(intent);
                return true;
            }
            case R.id.menu_settings: {
                Log.d("menu", "Settings selected");
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Message msg = new Message();
        msg.arg1 = DatabaseThread.LOCATION_MSG;
        msg.obj = location;
        mDatabase.mHandler.sendMessage(msg);
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
