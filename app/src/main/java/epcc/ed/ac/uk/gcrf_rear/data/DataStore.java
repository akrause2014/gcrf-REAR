package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by akrause on 11/11/2016.
 */

public class DataStore {

    private DataOutputStream mOutputStream;
    private String mFileName;
    private int mNumRows;

    public DataStore(Context context) throws IOException {
        mNumRows = 0;
        if (isExternalStorageWritable()) {
            mFileName = UUID.randomUUID().toString() + ".dat";
            Log.d("data store", "writing to file: " + mFileName);
            openFile(new File(context.getExternalFilesDir(null), "rear"), mFileName);
        }
        else {
            throw new IOException("Problem creating data store: External file storage is not writable");
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public DataOutputStream openFile(File dir, String name) throws IOException {
        mOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir, name))));
        return mOutputStream;
    }

    public void close() throws IOException {
        try {
            mOutputStream.flush();
            mOutputStream.close();
        }
        finally {
            Log.d("data store", "Closed file: " + mFileName + ". Wrote " + mNumRows + " records.");
            mNumRows = 0;
            mFileName = null;
        }
    }

    public void writeRecord(DataPoint dataPoint) throws IOException {
        mOutputStream.writeByte(dataPoint.getVersion());
        mOutputStream.writeByte(dataPoint.sensorAsByte());
        mOutputStream.writeLong(dataPoint.getTimestamp());
        mOutputStream.writeFloat(dataPoint.getX());
        mOutputStream.writeFloat(dataPoint.getY());
        mOutputStream.writeFloat(dataPoint.getZ());
        mNumRows++;
    }

}
