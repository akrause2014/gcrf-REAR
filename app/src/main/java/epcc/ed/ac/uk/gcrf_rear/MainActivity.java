package epcc.ed.ac.uk.gcrf_rear;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;
import epcc.ed.ac.uk.gcrf_rear.view.DataSurfaceView;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private DatabaseThread mDatabase;
    private DataSurfaceView mDataView;
    private LocationManager mLocationManager;
    private int mSamplingPeriod = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDatabase = ((REARApplication)getApplication()).getDatabase();
        mDataView = (DataSurfaceView) findViewById(R.id.data_view_id);
        mDataView.setDatabaseThread(mDatabase);
        SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        int rate = settings.getInt(SettingsActivity.FREQUENCY, 100);
        if (rate > 0) {
            mSamplingPeriod = 1000 / rate;
        }

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("main", "sampling period: " + mSamplingPeriod + "ms");
                    SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    Sensor senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    if (senAccelerometer != null) {
                        // Register accelerometer listener with sampling rate 100Hz
                        sensorManager.registerListener((SensorEventListener) getApplication(), senAccelerometer, mSamplingPeriod);
                        Log.d("main", "registered listener for accelerometer");
                    }
                    Sensor senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                    if (senGyroscope != null) {
                        // Register gyroscope listener with sampling rate 100Hz
                        sensorManager.registerListener((SensorEventListener) getApplication(), senGyroscope, mSamplingPeriod);
                        Log.d("main", "registered listener for gyroscope");
                    }
                    Sensor senMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                    if (senMagneticField != null) {
                        // Register magnetic field listener with sampling rate 100Hz
                        sensorManager.registerListener((SensorEventListener) getApplication(), senMagneticField, mSamplingPeriod);
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

    private void registerDevice(String password) {
        String message = "Device registration failed.";
        try
        {
            URL url = new URL("http://129.215.213.252:8080/gcrfREAR/webapi/gcrf-REAR/register");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.addRequestProperty("Authorization", "Basic " +
                    Base64.encodeToString(("gcrf-REAR:" + password).getBytes(), Base64.NO_WRAP));
            conn.connect();
            conn.getOutputStream().close();
            int status = conn.getResponseCode();
            Log.d("register", "Register returned status: " + status);
            if (status == 200) {
                InputStream inputStream = conn.getInputStream();
                byte[] buf = new byte[1024];
                StringBuilder builder = new StringBuilder();
                int l;
                while ((l = inputStream.read(buf)) != -1) {
                    builder.append(new String(buf, 0, l));
                }
                inputStream.close();
                Log.d("register", "registered with device id: " + builder.toString());
                String deviceID = builder.toString();
                SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(SettingsActivity.DEVICE_ID, deviceID);
                editor.commit();

                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Register Device");
                alert.setMessage("Device was registered successfully.");
                AlertDialog alertDialog = alert.create();
                alertDialog.show();
                return;
            }
            else {
                if (status == 401) {
                    message += " Unauthorised";
                }
            }

        } catch (MalformedURLException e) {
            Log.d("register", "failed to create URL", e);
        } catch (ProtocolException e) {
            Log.d("register", "failed to store preference", e);
        } catch (IOException e) {
            Log.d("register", "failed to register", e);
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Register Device");
        alert.setMessage(message);
        AlertDialog alertDialog = alert.create();
        alertDialog.show();

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
            case R.id.menu_register: {
                Log.d("menu", "Register device selected");

                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Register Device");
                alert.setMessage("Enter password:");

                final EditText input = new EditText(MainActivity.this);
                input.setTransformationMethod(new PasswordTransformationMethod());
                alert.setView(input);

                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String password = input.getEditableText().toString();
                        MainActivity.this.registerDevice(password);
                    }
                });
                alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
                AlertDialog alertDialog = alert.create();
                alertDialog.show();
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
