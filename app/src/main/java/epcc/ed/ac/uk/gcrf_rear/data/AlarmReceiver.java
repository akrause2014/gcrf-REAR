package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;

import epcc.ed.ac.uk.gcrf_rear.SettingsActivity;

/**
 * Created by akrause on 30/11/2016.
 */

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("upload", "uploading data files");
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        String baseURL = settings.getString(SettingsActivity.DATA_URL, SettingsActivity.DEFAULT_DATA_URL);
        String deviceId = settings.getString(SettingsActivity.DEVICE_ID, null);
        if (baseURL != null && deviceId != null) {
            String url = baseURL + deviceId + "/sensor";
            File datadir = new File(context.getExternalFilesDir(null), "rear");
            new UploadFile(url, datadir).execute();
        }
    }

    public class UploadFile extends AsyncTask<Void, Void, Void> {

        private final String url;
        private final File datadir;

        public UploadFile(String url, File datadir) {
            this.url = url;
            this.datadir = datadir;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            int numFiles = 0;
            for (File file : datadir.listFiles()) {
                if (file.isFile()) {
                    if (DataUpload.uploadFile(url, file)) {
                        numFiles++;
                        file.delete();
                    }
                }
            }
            Log.d("upload", "Data upload complete: " + numFiles + " files");
            return null;
        }

    }
}
