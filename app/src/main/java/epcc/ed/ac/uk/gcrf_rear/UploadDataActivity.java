package epcc.ed.ac.uk.gcrf_rear;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import epcc.ed.ac.uk.gcrf_rear.data.DataUpload;


public class UploadDataActivity extends AppCompatActivity {

    private int filesAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_data);
        File datadir = new File(getExternalFilesDir(null), "rear");
        for (File file : datadir.listFiles()) {
            if (file.isFile()) {
                filesAvailable++;
            }
        }
        TextView progressText = (TextView)findViewById(R.id.upload_progress_text);
        progressText.setText("Number of data files: " + filesAvailable);
    }

    public void uploadData(View view) {
        Log.d("upload", "data upload");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String baseURL = settings.getString(getString(R.string.pref_key_upload_url), getString(R.string.base_url));
        String deviceId = settings.getString(getString(R.string.pref_key_upload_device), null);
        if (deviceId == null || deviceId.isEmpty()) {
            TextView progressText = (TextView)findViewById(R.id.upload_progress_text);
            progressText.setText("Upload failed. Make sure the device is registered.");
            findViewById(R.id.upload_ok_button).setVisibility(View.INVISIBLE);
            findViewById(R.id.upload_cancel_button).setVisibility(View.INVISIBLE);
            findViewById(R.id.upload_close_button).setVisibility(View.VISIBLE);
        }
        else {
            String registerURL = baseURL + "register/" + deviceId;
            String dataURL = baseURL + "data/" + deviceId + "/sensor";
            Log.d("upload", "uploading data to " + dataURL);
            CheckBox btnDeleteData = (CheckBox) findViewById(R.id.delete_upload_checkbox);
            boolean deleteAfterUpload = btnDeleteData.isChecked();
            new UploadFile(registerURL, dataURL, deleteAfterUpload, filesAvailable).execute();
        }
    }

    public void uploadDeleteData(View view) {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Delete Data");
        alert.setMessage("Remove all data?");
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                File datadir = new File(UploadDataActivity.this.getExternalFilesDir(null), "rear");
                for (File file : datadir.listFiles()) {
                    if (file.isFile()) {
                        if (file.delete()) {
                            filesAvailable--;
                        }
                    }
                }
                TextView progressText = (TextView)findViewById(R.id.upload_progress_text);
                progressText.setText("Number of data files: " + filesAvailable);
            }
        });
        alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }

    public class UploadFile extends AsyncTask<Void, Integer, UploadResult>
    {
        private final String registerURL;
        private final String dataURL;
        private int filesAvailable;
        private final boolean deleteAfterUpload;

        public UploadFile(String registerURL, String dataURL, boolean deleteAfterUpload, int filesAvailable) {
            this.deleteAfterUpload = deleteAfterUpload;
            this.registerURL = registerURL;
            this.dataURL = dataURL;
            this.filesAvailable = filesAvailable;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.upload_ok_button).setVisibility(View.INVISIBLE);
            findViewById(R.id.upload_cancel_button).setVisibility(View.INVISIBLE);
            TextView progressText = (TextView)findViewById(R.id.upload_progress_text);
            progressText.setText("Uploading data ... Files: 0/" + filesAvailable);
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.upload_data_progress_bar);
            progressBar.setMax(filesAvailable);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            TextView progressText = (TextView)findViewById(R.id.upload_progress_text);
            progressText.setText("Uploading data ... Files: " + values[0] + "/" + filesAvailable);
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.upload_data_progress_bar);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(UploadResult result) {
            TextView progressText = (TextView)findViewById(R.id.upload_progress_text);
            findViewById(R.id.upload_close_button).setVisibility(View.VISIBLE);
            int numFiles = result.getNumFiles();
            switch (result.getStatus()) {
                case OK:
                    progressText.setText("Complete. Uploaded " + numFiles + "/" + filesAvailable + " files");
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(UploadDataActivity.this);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putLong(getString(R.string.last_upload_date), System.currentTimeMillis());
                    editor.commit();
                    break;
                case NOT_REGISTERED:
                    progressText.setText("Upload failed. Make sure the device is registered.");
                    break;
                case CONNECTION_FAILED:
                    progressText.setText("Upload failed. Check if the device is online.");
                    break;
                default:
                    progressText.setText("Upload failed. Try again later.");
                    break;
            }
        }

        @Override
        protected UploadResult doInBackground(Void... voids) {
            File datadir = new File(UploadDataActivity.this.getExternalFilesDir(null), "rear");
            int numFiles = 0;
            try {
                int status = DataUpload.isRegistered(registerURL);
                UploadResult.Status uploadStatus = UploadResult.Status.valueOf(status);
                if (uploadStatus != UploadResult.Status.OK) {
                    Log.d("upload", "pre check failed: status = " + status);
                    Logger.log(UploadDataActivity.this, "Upload failed: HTTP status = " + status + "\n");
                    return new UploadResult(numFiles, uploadStatus);
                }
            } catch (Exception e) {
                Log.e("upload", "pre check failed", e);
                Logger.log(UploadDataActivity.this, "Upload failed: error = " + e.getClass().getName() + ": " + e.getMessage() + "\n");
                return new UploadResult(numFiles, UploadResult.Status.valueOf(0));
            }
            for (File file : datadir.listFiles()) {
                if (file.isFile()) {
                    //                    Log.d("upload", "Reading file " + file.getName());
                    if (DataUpload.uploadFile(dataURL, file)) {
                        numFiles++;
                        publishProgress(numFiles);
                        if (deleteAfterUpload) {
                            file.delete();
                        }
                    }
                }
            }
            Log.d("upload", "Complete");
            return new UploadResult(numFiles);
        }

    }

    public void uploadClose(View view) {
        Log.d("upload", "cancel data upload");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public static class UploadResult
    {
        public enum Status {
            NOT_REGISTERED,
            SERVER_ERROR,
            CONNECTION_FAILED,
            UNKNOWN,
            OK;

            public static Status valueOf(int status) {
                switch (status) {
                    case 404: return NOT_REGISTERED;
                    case 200: return OK;
                    case 500: return SERVER_ERROR;
                    case 0: return CONNECTION_FAILED;
                    default: return UNKNOWN;
                }
            }

        }
        private final int numFiles;
        private final Status status;

        public UploadResult(int numFiles) {
            this(numFiles, Status.OK);
        }

        public UploadResult(int numFiles, Status status) {
            this.numFiles = numFiles;
            this.status = status;
        }

        public int getNumFiles() {
            return numFiles;
        }

        public Status getStatus() {
            return status;
        }

    }

}
