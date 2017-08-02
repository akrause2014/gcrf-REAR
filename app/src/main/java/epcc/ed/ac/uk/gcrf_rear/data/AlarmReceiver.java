package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import epcc.ed.ac.uk.gcrf_rear.Logger;
import epcc.ed.ac.uk.gcrf_rear.R;
import epcc.ed.ac.uk.gcrf_rear.REARApplication;
import epcc.ed.ac.uk.gcrf_rear.UploadDataActivity;

/**
 * Created by akrause on 30/11/2016.
 */

public class AlarmReceiver extends BroadcastReceiver
{

    DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-ww");

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.d("upload alarm", "uploading data files");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String baseURL = settings.getString(context.getString(R.string.pref_key_upload_url), "");
        String deviceId = settings.getString(context.getString(R.string.pref_key_upload_device), null);
        boolean keepBackup = settings.getBoolean(context.getString(R.string.pref_key_backup_active), false);
        if (baseURL != null && deviceId != null && !deviceId.isEmpty()) {
            String registerURL = baseURL + "register/" + deviceId;
            String dataURL = baseURL + "data/" + deviceId; // + "/sensor";
            String metaURL = baseURL + "metadata/" + deviceId;
            String locationURL = baseURL + "location/" + deviceId;
            String excludeFile = settings.getString(context.getString(R.string.current_data_store_file), null);
            new UploadFile(registerURL, metaURL, dataURL, locationURL, excludeFile, keepBackup, context).execute();
        }

        // delete the oldest week if there is not enough space on the SD card
        if (REARApplication.getUsableSpace() < 1000000000l) { // less than 1 GB
            final File[] weekDirs = REARApplication.getBackupDir(context).listFiles();
            if (weekDirs != null && weekDirs.length > 0) {
                File min = weekDirs[0];
                for (File f : weekDirs) {
                    if (f.isDirectory() && f.getName().compareTo(min.getName()) < 0) {
                        min = f;
                    }
                }
//                Log.d("alarm receiver", "Deleting backup directory: " + min.getAbsolutePath());
                deleteRecursive(min);
            }
        }

    }

    public class UploadFile extends AsyncTask<Void, Void, Integer> {

        private final String registerURL;
        private final String url;
        private final String metaURL;
        private final String locationURL;
        private final File datadir;
        private final File metadir;
        private final File backupdir;
        private final File locationdir;
        private final String excludeFile;
        private final boolean keepBackup;
        private final Context context;

        public UploadFile(String registerURL, String metaurl, String url, String locationurl, String excludeFile, boolean keepBackup, Context context) {
            this.registerURL = registerURL;
            this.metaURL = metaurl;
            this.url = url;
            this.locationURL = locationurl;
            this.datadir = REARApplication.getDataDir(context);
            this.context = context;
            this.excludeFile = excludeFile;
            this.metadir = REARApplication.getMetaDir(context);
            this.backupdir = REARApplication.getBackupDir(context);
            this.locationdir = REARApplication.getLocationDir(context);
            this.keepBackup = keepBackup;
        }
        @Override
        protected Integer doInBackground(Void... voids) {
            int numFiles = 0;
            try {
                int status = DataUpload.isRegistered(registerURL);
                UploadDataActivity.UploadResult.Status uploadStatus = UploadDataActivity.UploadResult.Status.valueOf(status);
                if (uploadStatus != UploadDataActivity.UploadResult.Status.OK) {
                    Log.d("upload", "pre check failed: HTTP status = " + status);
//                    Logger.log(context, "Upload failed: HTTP status = " + status + "\n");
                    return null;
                }
            }
            catch (Exception e) {
                Log.e("upload", "pre check failed", e);
//                Logger.log(context, "Upload failed: error = " + e.getClass().getName() + ": " + e.getMessage() + "\n");
                return null;
            }

            File weekDir = new File(backupdir, DATE_FORMAT.format(new Date()));

            if (keepBackup && !weekDir.exists()) {
                weekDir.mkdir();
            }
//            Log.d("upload", "excluding file: " + excludeFile);
            for (File file : datadir.listFiles()) {
                if (file.isFile() && !file.getName().equals(excludeFile)) {
                    DataUpload.Response response = DataUpload.uploadFile(url, file);
                    if (response.success) {
                        try {
                            int upload = Integer.valueOf(response.response);
                            File metafile = new File(metadir, file.getName());
                            if (metafile.isFile()) {
//                                DataUpload.uploadFile(metaURL + "/" + upload, metafile);
                                metafile.delete();

                            }
//                            else {
//                                Log.d("data upload", "Failed to upload metadata: not a file ");
//                            }
                        } catch (NumberFormatException e) {
                            // wrong response
//                            Log.d("data upload", "Unexpected response: " + response.response);
                        }
                        numFiles++;
//                        file.delete();

                        if (keepBackup) {
                            file.renameTo(new File(weekDir, file.getName()));
                        }
                        else {
                            file.delete();
                        }
//                        metafile.renameTo(new File(backupdir, file.getName() + ".meta"));
                    }
                }
            }

            for (File file : locationdir.listFiles()) {
                if (file.isFile()) {
                    Log.d("upload", "Uploading location file: " + file);
                    DataUpload.Response response = DataUpload.uploadFile(locationURL, file);
                    if (response.success) {
                        file.delete();
                    }
                }
            }
            return numFiles;
        }

        @Override
        protected void onPostExecute(Integer numFiles) {
            if (numFiles != null && numFiles > 0) {
                Log.d("upload", "Data upload complete: " + numFiles + " files");
//                Logger.log(context, "Data upload complete: " + numFiles + " files\n");
            }

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(context.getString(R.string.last_upload_date), System.currentTimeMillis());
            editor.commit();
        }
    }
}
