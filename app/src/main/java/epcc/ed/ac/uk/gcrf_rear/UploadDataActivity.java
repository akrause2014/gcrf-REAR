package epcc.ed.ac.uk.gcrf_rear;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


public class UploadDataActivity extends AppCompatActivity {

    private int filesAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_data);
        File datadir = new File(UploadDataActivity.this.getExternalFilesDir(null), "rear");
        for (File file : datadir.listFiles()) {
            if (file.isFile()) {
                filesAvailable++;
            }
        }
        TextView progressText = (TextView)findViewById(R.id.upload_progress_text);
        progressText.setText("Number of data files: " + filesAvailable);
    }

    public void uploadData(View view) {
        SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        String baseURL = settings.getString(SettingsActivity.DATA_URL, null);
        String deviceId = settings.getString(SettingsActivity.DEVICE_ID, null);
        if (baseURL != null && deviceId != null) {
            String url = baseURL + deviceId + "/sensor";
            Log.d("upload", "uploading data to " + url);
            CheckBox btnDeleteData = (CheckBox) findViewById(R.id.delete_upload_checkbox);
            boolean deleteAfterUpload = btnDeleteData.isChecked();
            new UploadFile(url, deleteAfterUpload, filesAvailable).execute();
        }
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
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            File datadir = new File(UploadDataActivity.this.getExternalFilesDir(null), "rear");
            int numFiles = 0;
            for (File file : datadir.listFiles()) {
                if (file.isFile()) {
                    Log.d("upload", "Reading file " + file.getName());
                    if (uploadFile(file)) {
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

        private boolean uploadFile(File file) {
            try {
                Log.d("upload", "opening connection");
                URL url = new URL(dataURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/octet-stream");
                con.setDoInput(true);
                con.setDoOutput(true);
                con.connect();
                OutputStream outputStream = con.getOutputStream();
                InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                byte[] buf = new byte[2048];
                int c;
                while ((c = inputStream.read(buf)) >= 0) {
                    outputStream.write(buf, 0, c);
                }
                inputStream.close();
                outputStream.close();
                Log.d("upload", "done");
                InputStream is = con.getInputStream();
                if (is != null) {
                    while (is.read(buf) != -1) {
//                        Log.d("upload input", new String(buf));
                    }
                }
                InputStream es = con.getErrorStream();
                if (es != null) {
                    while (es.read(buf) != -1) {
//                        Log.d("upload error", new String(buf));
                    }
                }
                return true;
            }
            catch (MalformedURLException e) {
                Log.e("upload", "malformed URL", e);
            } catch (ProtocolException e) {
                Log.e("upload", "error", e);
            } catch (ConnectException e) {
                Log.e("upload", "no connection", e);
            } catch (IOException e) {
                Log.e("upload", "error", e);
            }
            return false;
        }
    }

    public void uploadClose(View view) {
        Log.d("upload", "cancel data upload");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
