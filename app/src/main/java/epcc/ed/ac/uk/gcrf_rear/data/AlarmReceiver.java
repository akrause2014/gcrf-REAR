package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

import epcc.ed.ac.uk.gcrf_rear.Logger;
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
            String dataURL = baseURL + "data/" + deviceId; // + "/sensor";
            String metaURL = baseURL + "metadata/" + deviceId;
            String excludeFile = settings.getString(context.getString(R.string.current_data_store_file), null);
            File datadir = new File(context.getExternalFilesDir(null), "rear");
            new UploadFile(registerURL, metaURL, dataURL, datadir, excludeFile, context).execute();
        }
    }

    public class UploadFile extends AsyncTask<Void, Void, Integer> {

        private final String registerURL;
        private final String url;
        private final String metaURL;
        private final File datadir;
        private final File metadir;
        private final String excludeFile;
        private final Context context;

        public UploadFile(String registerURL, String metaurl, String url, File datadir, String excludeFile, Context context) {
            this.registerURL = registerURL;
            this.metaURL = metaurl;
            this.url = url;
            this.datadir = datadir;
            this.context = context;
            this.excludeFile = excludeFile;
            this.metadir = new File(context.getExternalFilesDir(null), "rear_meta");
        }
        @Override
        protected Integer doInBackground(Void... voids) {
            int numFiles = 0;
            try {
                int status = DataUpload.isRegistered(registerURL);
                UploadDataActivity.UploadResult.Status uploadStatus = UploadDataActivity.UploadResult.Status.valueOf(status);
                if (uploadStatus != UploadDataActivity.UploadResult.Status.OK) {
                    Log.d("upload", "pre check failed: HTTP status = " + status);
                    Logger.log(context, "Upload failed: HTTP status = " + status + "\n");
                    return null;
                }
            }
            catch (Exception e) {
                Log.e("upload", "pre check failed", e);
                Logger.log(context, "Upload failed: error = " + e.getClass().getName() + ": " + e.getMessage() + "\n");
                return null;
            }

            Log.d("upload", "excluding file: " + excludeFile);
            for (File file : datadir.listFiles()) {
                File metafile = new File(metadir, file.getName());
                if (file.isFile() && !file.getName().equals(excludeFile)) {
                    DataUpload.Response response = DataUpload.uploadFile(url, file);
                    if (response.success) {
                        try {
                            int upload = Integer.valueOf(response.response);
                            if (metafile.isFile()) {
                                DataUpload.uploadFile(metaURL + "/" + upload, metafile);
                            }
                            else {
                                Log.d("data upload", "Failed to upload metadata: not a file ");
                            }
                        } catch (NumberFormatException e) {
                            // wrong response
                            Log.d("data upload", "Unexpected response: " + response.response);
                        }
                        numFiles++;
                        file.delete();
                    }
                }
            }
            if (numFiles > 0) {
                Log.d("upload", "Data upload complete: " + numFiles + " files");
            }
            return numFiles;
        }

        @Override
        protected void onPostExecute(Integer numFiles) {

            Logger.log(context, "Data upload complete: " + numFiles + " files\n");

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(context.getString(R.string.last_upload_date), System.currentTimeMillis());
            editor.commit();
        }
    }
}
