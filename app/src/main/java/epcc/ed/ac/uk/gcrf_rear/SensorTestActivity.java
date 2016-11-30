package epcc.ed.ac.uk.gcrf_rear;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import epcc.ed.ac.uk.gcrf_rear.view.DataSurfaceView;

public class SensorTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_test);
        DataSurfaceView dataView = (DataSurfaceView) findViewById(R.id.data_view_id);
        dataView.setDatabaseThread(((REARApplication) getApplication()).getDatabase());

        ToggleButton displayToggle = (ToggleButton) findViewById(R.id.displayToggleButton);
        displayToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    Sensor senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    if (senAccelerometer != null) {
                        sensorManager.registerListener(
                                (SensorEventListener) getApplication(),
                                senAccelerometer,
                                10000);
                        Log.d("test", "registered listener for accelerometer");
                    }
                }
                else {
                    SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    sensorManager.unregisterListener((SensorEventListener) getApplication());
                }
                ((REARApplication) getApplication()).getDatabase().setDisplayOn(isChecked);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDisplay();
    }

    private void stopDisplay() {
        ((REARApplication) getApplication()).getDatabase().setDisplayOn(false);
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener((SensorEventListener) getApplication());
    }


}
