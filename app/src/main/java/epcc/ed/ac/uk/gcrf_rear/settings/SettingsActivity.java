package epcc.ed.ac.uk.gcrf_rear.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.List;

import epcc.ed.ac.uk.gcrf_rear.R;

/**
 * Created by akrause on 07/12/2016.
 */

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(
                android.R.id.content, new SettingsFragment()).commit();
    }

}
