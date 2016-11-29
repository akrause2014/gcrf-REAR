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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;

public class MainActivity extends AppCompatActivity {

    private DatabaseThread mDatabase;
    private static final int DEFAULT_SAMPLING_RATE = 100; // default is 100 Hertz


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDatabase = ((REARApplication)getApplication()).getDatabase();
        mDatabase.setSensorTextView((TextView)findViewById(R.id.sensor_text));
        SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        int rate = settings.getInt(SettingsActivity.FREQUENCY, DEFAULT_SAMPLING_RATE);
        if (rate <= 0) {
            rate = DEFAULT_SAMPLING_RATE;
        }
        final int samplingPeriod = 1000000 / rate;
        TextView freqText = (TextView)findViewById(R.id.main_frequency_text);
        freqText.setText("Frequency: " + rate + " Hertz");
        final TextView sensorTextView = (TextView) findViewById(R.id.sensor_text);
        final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        final Sensor senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        final Sensor senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        final Sensor senMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (senAccelerometer == null) {
            findViewById(R.id.track_accel_checkBox).setVisibility(View.GONE);
        }
        if (senGyroscope == null) {
            findViewById(R.id.track_gyro_checkBox).setVisibility(View.GONE);
        }
        if (senMagneticField == null) {
            findViewById(R.id.track_magnet_checkBox).setVisibility(View.GONE);
        }
        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("main", "sampling period: " + samplingPeriod + " microseconds");
                    mDatabase.setFileStoreOn(true);
                    if (senAccelerometer != null && ((CheckBox) findViewById(R.id.track_accel_checkBox)).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), senAccelerometer, samplingPeriod);
                        Log.d("main", "registered listener for accelerometer");
                    }
                    if (senGyroscope != null && ((CheckBox) findViewById(R.id.track_accel_checkBox)).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), senGyroscope, samplingPeriod);
                        Log.d("main", "registered listener for gyroscope");
                    }
                    if (senMagneticField != null && ((CheckBox) findViewById(R.id.track_accel_checkBox)).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), senMagneticField, samplingPeriod);
                        Log.d("main", "registered listener for magnetic field");
                    }
                    try {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) getApplication());
                        Log.d("main", "registered listener for GPS");
                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            Log.d("location",
                                    "Lat/Lon: " + location.getLatitude() + "," + location.getLongitude()
                                            + "\nAccuracy: " + location.getAccuracy()
                                            + "\nAltitude: " + location.getAltitude());
                        }
                        else {
                            sensorTextView.setText("No GPS location available.\nLat/Lon: ---/---");
                        }
                    }
                    catch (SecurityException e) {
                        // check permissions
                        Log.e("main", "failed to register location listener", e);
                    }

                } else {
                    sensorManager.unregisterListener((SensorEventListener) getApplication());
                    try {
                        locationManager.removeUpdates((LocationListener) getApplication());
                    }
                    catch (SecurityException e) {
                        // check permissions
                    }
                    mDatabase.setFileStoreOn(false);
                    mDatabase.close();
                    sensorTextView.setText("");
                }
            }
        });
        CheckBox accelCheckBox = (CheckBox) findViewById(R.id.track_accel_checkBox);
        accelCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sensor sensor = senAccelerometer;
                if (sensor != null && toggle.isChecked()) {
                    if (((CheckBox) v).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), sensor, samplingPeriod);
                        Log.d("main", "registered listener for accelerometer");
                    } else {
                        sensorManager.unregisterListener((SensorEventListener) getApplication(), sensor);
                    }
                }
            }
        });
        CheckBox gyroCheckBox = (CheckBox) findViewById(R.id.track_gyro_checkBox);
        gyroCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sensor sensor = senGyroscope;
                if (sensor != null && toggle.isChecked()) {
                    if (((CheckBox) v).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), sensor, samplingPeriod);
                        Log.d("main", "registered listener for accelerometer");
                    } else {
                        sensorManager.unregisterListener((SensorEventListener) getApplication(), sensor);
                    }
                }
            }
        });
        final CheckBox magnetCheckBox = (CheckBox) findViewById(R.id.track_gyro_checkBox);
        magnetCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sensor sensor = senMagneticField;
                if (sensor != null && toggle.isChecked()) {
                    if (((CheckBox) v).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), sensor, samplingPeriod);
                        Log.d("main", "registered listener for accelerometer");
                    } else {
                        sensorManager.unregisterListener((SensorEventListener) getApplication(), sensor);
                    }
                }
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
            case R.id.menu_display: {
                Log.d("menu", "Test display selected");
                Intent intent = new Intent(this, SensorTestActivity.class);
                this.startActivity(intent);
                return true;
            }
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

}
