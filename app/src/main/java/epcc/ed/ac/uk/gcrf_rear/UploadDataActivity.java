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
        String baseURL = settings.getString(getString(R.string.pref_key_upload_url), getString(R.string.default_url));
        String deviceId = settings.getString(getString(R.string.pref_key_upload_device), null);
        if (baseURL != null && deviceId != null) {
            String url = baseURL + deviceId + "/sensor";
            Log.d("upload", "uploading data to " + url);
            CheckBox btnDeleteData = (CheckBox) findViewById(R.id.delete_upload_checkbox);
            boolean deleteAfterUpload = btnDeleteData.isChecked();
            new UploadFile(url, deleteAfterUpload, filesAvailable).execute();
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

    public class UploadFile extends AsyncTask<Void, Integer, Integer>
    {
        private final boolean deleteAfterUpload;
        private final String dataURL;
        private int filesAvailable;

        public UploadFile(String url, boolean deleteAfterUpload, int filesAvailable) {
            this.deleteAfterUpload = deleteAfterUpload;
            this.dataURL = url;
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
        protected void onPostExecute(Integer numFiles) {
            TextView progressText = (TextView)findViewById(R.id.upload_progress_text);
            progressText.setText("Complete. Uploaded " + numFiles + "/" + filesAvailable + " files");
            findViewById(R.id.upload_close_button).setVisibility(View.VISIBLE);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(UploadDataActivity.this);
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(getString(R.string.last_upload_date), System.currentTimeMillis());
            editor.commit();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            File datadir = new File(UploadDataActivity.this.getExternalFilesDir(null), "rear");
            int numFiles = 0;
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
            return numFiles;
        }

    }

    public void uploadClose(View view) {
        Log.d("upload", "cancel data upload");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}
