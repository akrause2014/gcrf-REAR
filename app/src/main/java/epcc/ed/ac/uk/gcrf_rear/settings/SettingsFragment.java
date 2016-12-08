package epcc.ed.ac.uk.gcrf_rear.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.widget.TextView;

import java.util.Map;

import epcc.ed.ac.uk.gcrf_rear.R;
import epcc.ed.ac.uk.gcrf_rear.REARApplication;

/**
 * Created by akrause on 08/12/2016.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener
{

    SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        //add xml
        addPreferencesFromResource(R.xml.preferences);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        for (String key: sharedPreferences.getAll().keySet()) {
            onSharedPreferenceChanged(sharedPreferences, key);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.startsWith("pref_key")) {
            Log.d("preferences", "preference changed: " + key);
            String value = sharedPreferences.getString(key, "");
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setSummary(value);
            }
            if (key.equals(getResources().getString(R.string.pref_key_upload_period))) {
                try {
                    int p = Integer.valueOf(value);
                    ((REARApplication) getActivity().getApplication()).scheduleDataUpload(p);
                } catch (NumberFormatException e) {

                }
            }
            if (key.equals(getResources().getString(R.string.pref_key_frequency))) {
                try {
                    int p = Integer.valueOf(value);
                } catch (NumberFormatException e) {

                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}
