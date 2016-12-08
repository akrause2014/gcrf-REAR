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
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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
import epcc.ed.ac.uk.gcrf_rear.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private DatabaseThread mDatabase;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDatabase = ((REARApplication)getApplication()).getDatabase();
        mDatabase.setSensorTextView((TextView)findViewById(R.id.sensor_text));
        final TextView sensorTextView = (TextView) findViewById(R.id.sensor_text);
        final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        final Sensor senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        final Sensor senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        final Sensor senMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int rate = getRate();
                final int samplingPeriod = 1000000 / rate;
                if (isChecked) {
                    Log.d("main", "sampling period: " + samplingPeriod + " microseconds");
                    mDatabase.setFileStoreOn(true);
                    if (senAccelerometer != null && ((CheckBox) findViewById(R.id.track_accel_checkBox)).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), senAccelerometer, samplingPeriod);
                        Log.d("main", "registered listener for accelerometer");
                    }
                    if (senGyroscope != null && ((CheckBox) findViewById(R.id.track_gyro_checkBox)).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), senGyroscope, samplingPeriod);
                        Log.d("main", "registered listener for gyroscope");
                    }
                    if (senMagneticField != null && ((CheckBox) findViewById(R.id.track_magnet_checkBox)).isChecked()) {
                        sensorManager.registerListener((SensorEventListener) getApplication(), senMagneticField, samplingPeriod);
                        Log.d("main", "registered listener for magnetic field");
                    }
                    try {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) getApplication());
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) getApplication());
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
                    Log.d("main", "unregistered listener");
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
        if (senAccelerometer == null) {
            accelCheckBox.setVisibility(View.GONE);
        }
        else {
            accelCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Sensor sensor = senAccelerometer;
                    boolean isChecked = ((CheckBox) v).isChecked();
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(
                            "AccelSensor", isChecked).commit();
                    if (sensor != null && toggle.isChecked()) {
                        if (isChecked) {
                            int samplingPeriod = getSamplingPeriod();
                            sensorManager.registerListener((SensorEventListener) getApplication(), sensor, samplingPeriod);
                            Log.d("main", "registered listener for accelerometer");
                        } else {
                            sensorManager.unregisterListener((SensorEventListener) getApplication(), sensor);
                        }
                    }
                }
            });
            boolean isChecked = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("AccelSensor", true);
            accelCheckBox.setChecked(isChecked);
        }

        CheckBox gyroCheckBox = (CheckBox) findViewById(R.id.track_gyro_checkBox);
        if (senGyroscope == null) {
            gyroCheckBox.setVisibility(View.GONE);
        }
        else {
            gyroCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Sensor sensor = senGyroscope;
                    boolean isChecked = ((CheckBox) v).isChecked();
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(
                            "GyroSensor", isChecked).commit();
                    if (sensor != null && toggle.isChecked()) {
                        if (((CheckBox) v).isChecked()) {
                            int samplingPeriod = getSamplingPeriod();
                            sensorManager.registerListener((SensorEventListener) getApplication(), sensor, samplingPeriod);
                            Log.d("main", "registered listener for gyroscope");
                        } else {
                            sensorManager.unregisterListener((SensorEventListener) getApplication(), sensor);
                        }
                    }
                }
            });
            boolean isChecked = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("GyroSensor", true);
            gyroCheckBox.setChecked(isChecked);
        }
        final CheckBox magnetCheckBox = (CheckBox) findViewById(R.id.track_magnet_checkBox);
        if (senMagneticField == null) {
            magnetCheckBox.setVisibility(View.GONE);
        }
        else {
            magnetCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Sensor sensor = senMagneticField;
                    boolean isChecked = ((CheckBox) v).isChecked();
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(
                            "MagnetSensor", toggle.isChecked()).commit();
                    if (sensor != null && isChecked) {
                        if (((CheckBox) v).isChecked()) {
                            int samplingPeriod = getSamplingPeriod();
                            sensorManager.registerListener((SensorEventListener) getApplication(), sensor, samplingPeriod);
                            Log.d("main", "registered listener for magnetic field");
                        } else {
                            sensorManager.unregisterListener((SensorEventListener) getApplication(), sensor);
                        }
                    }
                }
            });
            boolean isChecked = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("MagnetSensor", true);
            magnetCheckBox.setChecked(isChecked);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        int rate = getRate();
        TextView freqText = (TextView)findViewById(R.id.main_frequency_text);
        freqText.setText("Frequency: " + rate + " Hertz");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void registerDevice(String password) {
        String url = getString(R.string.register_url);
        new RegisterDevice(url, password).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_display: {
                final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
                toggle.setChecked(false);
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

    public class RegisterDevice extends AsyncTask<Void, Void, String> {

        private final String url;
        private final String password;

        public RegisterDevice(String url, String password) {
            this.url = url;
            this.password = password;
        }

        @Override
        protected String doInBackground(Void... params) {
            String message = "";
            try {
                URL url = new URL(this.url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.addRequestProperty("Authorization", "Basic " +
                        Base64.encodeToString((getString(R.string.register_user) + ":" + password).getBytes(), Base64.NO_WRAP));
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
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    final SharedPreferences.Editor editor = settings.edit();
                    editor.putString(getString(R.string.pref_key_upload_device), deviceID);
                    runOnUiThread(new Runnable() {
                                               public void run() {
                                                   editor.commit();
                                               }
                                           });

                    message = "Device was registered successfully.";
                } else {
                    message = "Device registration failed: ";
                    switch (status) {
                        case 401:
                            message += "Unauthorised";
                            break;
                        default:
                            message += "Status " + status;
                            break;
                    }
                }

            } catch (MalformedURLException e) {
                Log.d("register", "failed to create URL", e);
            } catch (ProtocolException e) {
                Log.d("register", "failed to store preference", e);
            } catch (IOException e) {
                Log.d("register", "failed to register", e);
            }
            final String msg = message;
            runOnUiThread(new Runnable() {
                public void run() {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setTitle("Register Device");
                    alert.setMessage(msg);
                    AlertDialog alertDialog = alert.create();
                    alertDialog.show();
                }
            });
            return null;
        }
    }

}
