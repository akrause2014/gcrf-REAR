package epcc.ed.ac.uk.gcrf_rear;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

import epcc.ed.ac.uk.gcrf_rear.data.DataPoint;
import epcc.ed.ac.uk.gcrf_rear.data.DataUpload;

public class UploadDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_data);
    }

    public void uploadData(View view) {
        Log.d("upload", "uploading data");
        int i=0;
        while (DataUpload.exists(this, "data-" + i + ".dat")) {
            String name = "data-" + i + ".dat";
            try {
                DataInputStream inputStream = DataUpload.open(this, name);
                try {
                    while (true) {
                        DataPoint dataPoint = DataUpload.readRecord(inputStream);
                    }
                }
                catch (EOFException e) {
                    // end of file
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    public void uploadClose(View view) {
        Log.d("upload", "cancel data upload");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
