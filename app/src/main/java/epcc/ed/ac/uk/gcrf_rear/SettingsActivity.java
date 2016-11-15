package epcc.ed.ac.uk.gcrf_rear;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import epcc.ed.ac.uk.gcrf_rear.data.DataPoint;
import epcc.ed.ac.uk.gcrf_rear.data.DataUpload;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public void saveSettings(View view) {

    }

    public void cancelSettings(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
