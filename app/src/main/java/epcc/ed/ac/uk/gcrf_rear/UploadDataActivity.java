package epcc.ed.ac.uk.gcrf_rear;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import epcc.ed.ac.uk.gcrf_rear.view.Settings;

public class UploadDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_data);
    }

    public void uploadData(View view) {
        Log.d("upload", "uploading data");
        CheckBox btnDeleteData = (CheckBox) findViewById(R.id.delete_upload_checkbox);
        boolean deleteAfterUpload = btnDeleteData.isChecked();
        new UploadFile(deleteAfterUpload).execute();
//        File datadir = new File(getExternalFilesDir(null), "rear");
//        for (File file : datadir.listFiles()) {
//            if (file.isFile()) {
//                Log.d("upload", "Reading file " + file.getName());
//                uploadFile(file);
//                try {
//                    DataInputStream inputStream = new DataInputStream(
//                            new BufferedInputStream(new FileInputStream(file)));
//                    while (true) {
//                        DataPoint dataPoint = DataUpload.readRecord(inputStream);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    public class UploadFile extends AsyncTask<Void, Void, Void>
    {
        private final boolean deleteAfterUpload;

        public UploadFile(boolean deleteAfterUpload) {
            this.deleteAfterUpload = deleteAfterUpload;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File datadir = new File(UploadDataActivity.this.getExternalFilesDir(null), "rear");
            for (File file : datadir.listFiles()) {
                if (file.isFile()) {
                    Log.d("upload", "Reading file " + file.getName());
                    if (uploadFile(file) && deleteAfterUpload) {
                        file.delete();
                    }
                }
            }
            return null;
        }

        private boolean uploadFile(File file) {
            try {
                URL url = new URL(Settings.mURL);
                HttpURLConnection con = (HttpURLConnection) (new URL(Settings.mURL)).openConnection();
                con.setRequestMethod("POST");
                con.setDoInput(false);
                con.setDoOutput(true);
                con.connect();
                OutputStream outputStream = con.getOutputStream();
                InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                byte[] buf = new byte[2048];
                int c = 0;
                while (c >= 0) {
                    c = inputStream.read(buf);
                    outputStream.write(buf, 0, c);
                }
                inputStream.close();
                outputStream.close();
                return true;
            }
            catch (MalformedURLException e) {
                Log.e("upload", "malformed URL", e);
            } catch (ProtocolException e) {
                Log.e("upload", "error", e);
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
