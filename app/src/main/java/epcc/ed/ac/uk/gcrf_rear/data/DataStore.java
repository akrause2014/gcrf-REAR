package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.Context;
import android.location.Location;
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

    private final static int TYPE_SENSOR = 1;
    private final static int TYPE_LOCATION = 2;
    private final static int VERSION = 1;


    private DataOutputStream mOutputStream;
    private String mFileName;
    private int mNumRows;

    public DataStore(Context context) throws IOException {
        mNumRows = 0;
        if (isExternalStorageWritable()) {
            mFileName = UUID.randomUUID().toString() + ".dat";
            Log.d("data store", "opening file: " + mFileName);
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
        mOutputStream.writeByte(TYPE_SENSOR);
        mOutputStream.writeByte(VERSION);
        mOutputStream.writeByte(dataPoint.sensorAsByte());
        mOutputStream.writeLong(dataPoint.getTimestamp());
        mOutputStream.writeFloat(dataPoint.getX());
        mOutputStream.writeFloat(dataPoint.getY());
        mOutputStream.writeFloat(dataPoint.getZ());
        mNumRows++;
    }

    public void writeLocation(Location location) throws IOException {
        mOutputStream.writeByte(TYPE_LOCATION);
        mOutputStream.writeByte(VERSION);
        mOutputStream.writeLong(location.getTime());
        mOutputStream.writeDouble(location.getLatitude());
        mOutputStream.writeDouble(location.getLongitude());
        mOutputStream.writeDouble(location.getAltitude());
        mOutputStream.writeFloat(location.getAccuracy());
        mNumRows++;
    }

}
