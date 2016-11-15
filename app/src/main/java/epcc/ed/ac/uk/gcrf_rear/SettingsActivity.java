package epcc.ed.ac.uk.gcrf_rear;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import epcc.ed.ac.uk.gcrf_rear.view.Settings;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "REARPreferences";
    public static final String DATA_URL = "dataURL";
    public static final String DEVICE_ID = "deviceID";
    public static final String DATA_SIZE = "dataSize";
    public static final String FREQUENCY = "frequency";
    private static final String DEFAULT_DATA_URL = "http://129.215.213.252:8080/gcrfREAR/webapi/gcrf-REAR/data/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        EditText urlInput = (EditText) findViewById(R.id.data_url_input);
        urlInput.setText(settings.getString(DATA_URL, DEFAULT_DATA_URL));
        EditText deviceIdInput = (EditText) findViewById(R.id.device_id_input);
        deviceIdInput.setText(settings.getString(DEVICE_ID, "3B9FC8C2E3724EA2B13EBB31B4598CD9"));
        EditText dataSizeInput = (EditText) findViewById(R.id.data_size);
        dataSizeInput.setText(String.valueOf(settings.getInt(DATA_SIZE, 6000)));
        EditText freqInput = (EditText) findViewById(R.id.frequency_input);
        freqInput.setText(String.valueOf(settings.getInt(FREQUENCY, 100)));
    }

    public void saveSettings(View view) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        EditText urlInput = (EditText) findViewById(R.id.data_url_input);
        editor.putString(DATA_URL, urlInput.getText().toString());
        EditText deviceIdInput = (EditText) findViewById(R.id.device_id_input);
        editor.putString(DEVICE_ID, deviceIdInput.getText().toString());
        EditText dataSizeInput = (EditText) findViewById(R.id.data_size);
        int dataSize = Integer.parseInt(dataSizeInput.getText().toString());
        ((REARApplication)getApplication()).getDatabase().setDataSize(dataSize);
        editor.putInt(DATA_SIZE, dataSize);
        EditText freqInput = (EditText) findViewById(R.id.frequency_input);
        editor.putInt(FREQUENCY, Integer.parseInt(freqInput.getText().toString()));
        editor.commit();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void cancelSettings(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
