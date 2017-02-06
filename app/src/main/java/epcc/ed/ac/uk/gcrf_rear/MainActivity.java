package epcc.ed.ac.uk.gcrf_rear;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
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
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;
import epcc.ed.ac.uk.gcrf_rear.sensor.SensorListenerService;
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
            } catch (NumberFormatException e) {
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
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mDatabase = ((REARApplication) getApplication()).getDatabase();
        mDatabase.setSensorTextView((TextView) findViewById(R.id.sensor_text));
        final TextView sensorTextView = (TextView) findViewById(R.id.sensor_text);
        final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        final Sensor senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        final Sensor senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        final Sensor senMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    startService(new Intent(MainActivity.this, SensorListenerService.class));
                    mDatabase.setFileStoreOn(true);

                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        return;
                    }
                    registerLocationListener();
                } else {
                    Log.d("main", "unregistered listener");
                    stopService(new Intent(MainActivity.this, SensorListenerService.class));
                    mDatabase.setFileStoreOn(false);
                    mDatabase.close();
                    sensorTextView.setText("");
                    final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    locationManager.removeUpdates((LocationListener) getApplication());
                }
            }
        });

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        CheckBox accelCheckBox = (CheckBox) findViewById(R.id.track_accel_checkBox);
        if (senAccelerometer == null) {
            accelCheckBox.setVisibility(View.GONE);
        }
        else {
            boolean isChecked = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("AccelSensor", true);
            accelCheckBox.setChecked(isChecked);
            accelCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefs.edit().putBoolean(
                            getString(R.string.sensor_accel_check),
                            ((CheckBox) v).isChecked()).commit();
                }
            });
        }

        CheckBox gyroCheckBox = (CheckBox) findViewById(R.id.track_gyro_checkBox);
        if (senGyroscope == null) {
            gyroCheckBox.setVisibility(View.GONE);
        }
        else {
            boolean isChecked = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("GyroSensor", true);
            gyroCheckBox.setChecked(isChecked);
            gyroCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefs.edit().putBoolean(
                            getString(R.string.sensor_gyro_check),
                            ((CheckBox) v).isChecked()).commit();
                }
            });
        }

        final CheckBox magnetCheckBox = (CheckBox) findViewById(R.id.track_magnet_checkBox);
        if (senMagneticField == null) {
            magnetCheckBox.setVisibility(View.GONE);
        }
        else {
            boolean isChecked = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("MagnetSensor", true);
            magnetCheckBox.setChecked(isChecked);
            magnetCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefs.edit().putBoolean(
                            getString(R.string.sensor_magnet_check),
                            ((CheckBox) v).isChecked()).commit();
                }
            });
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        int rate = getRate();
        TextView freqText = (TextView)findViewById(R.id.main_frequency_text);
        freqText.setText("Frequency: " + rate + " Hertz");
//        startService(new Intent(this, SensorListenerService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void registerDevice(String password) {
        String url = getString(R.string.base_url) + "register";
        new RegisterDevice(url, password).execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch(requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    registerLocationListener();
                }
            }
        }
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
            case R.id.menu_view_log: {
                Log.d("menu", "View logs selected");
                Intent intent = new Intent(this, LogViewActivity.class);
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

    private void registerLocationListener() {
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) getApplication());
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) getApplication());
        Log.d("main", "registered listener for GPS");
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            Log.d("location",
                    "Lat/Lon: " + location.getLatitude() + "," + location.getLongitude()
                            + "\nAccuracy: " + location.getAccuracy()
                            + "\nAltitude: " + location.getAltitude());
        } else {
            ((TextView) findViewById(R.id.sensor_text)).setText("No GPS location available.\nLat/Lon: ---/---");
        }

    }

    public class RegisterDevice extends AsyncTask<Void, Void, String> {

        private final String url;
        private final String password;
        private ProgressDialog progress;
        private String message;

        public RegisterDevice(String url, String password) {
            this.url = url;
            this.password = password;
        }

        @Override
        protected String doInBackground(Void... params) {
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
                    message = "Device was registered successfully.";
                    return deviceID;
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
                message = "Device registration failed";
                Log.d("register", "failed to create URL", e);
            } catch (ProtocolException e) {
                message = "Device registration failed";
                Log.d("register", "failed to store preference", e);
            } catch (ConnectException e) {
                message = "Device registration failed: " + e.getMessage();
                Log.d("register", "failed to register", e);
            } catch (IOException e) {
                message = "Device registration failed";
                Log.d("register", "failed to register", e);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(MainActivity.this);
            progress.setMessage("Registering device");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String deviceID)
        {
            progress.hide();

            if (deviceID != null) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                final SharedPreferences.Editor editor = settings.edit();
                editor.putString(getString(R.string.pref_key_upload_device), deviceID);
                editor.commit();
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle("Register Device");
            alert.setMessage(message);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();

        }
    }

}
