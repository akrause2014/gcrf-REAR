package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

import epcc.ed.ac.uk.gcrf_rear.R;
import epcc.ed.ac.uk.gcrf_rear.UploadDataActivity;

/**
 * Created by akrause on 30/11/2016.
 */

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("upload alarm", "uploading data files");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String baseURL = settings.getString(context.getString(R.string.pref_key_upload_url), "");
        String deviceId = settings.getString(context.getString(R.string.pref_key_upload_device), null);
        if (baseURL != null && deviceId != null && !deviceId.isEmpty()) {
            String registerURL = baseURL + "register/" + deviceId;
            String dataURL = baseURL + deviceId + "/sensor";
            File datadir = new File(context.getExternalFilesDir(null), "rear");
            new UploadFile(registerURL, dataURL, datadir, context).execute();
        }
    }

    public class UploadFile extends AsyncTask<Void, Void, Void> {

        private final String registerURL;
        private final String url;
        private final File datadir;
        private final Context context;

        public UploadFile(String registerURL, String url, File datadir, Context context) {
            this.registerURL = registerURL;
            this.url = url;
            this.datadir = datadir;
            this.context = context;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            int numFiles = 0;
            int status = DataUpload.isRegistered(registerURL);
            UploadDataActivity.UploadResult.Status uploadStatus = UploadDataActivity.UploadResult.Status.valueOf(status);
            if (uploadStatus != UploadDataActivity.UploadResult.Status.OK) {
                Log.d("upload", "pre check failed: status = " + status);
                return null;
            }
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

        @Override
        protected void onPostExecute(Void aVoid) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(context.getString(R.string.last_upload_date), System.currentTimeMillis());
            editor.commit();
        }
    }
}
