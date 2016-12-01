package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.Context;
import android.hardware.Sensor;
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

    public enum SensorType
    {
        ACCELEROMETER(1),
        GYROSCOPE(2),
        MAGNETIC_FIELD(3),
        LOCATION(4);

        private int type;
        SensorType(int type) {
            this.type = type;
        }
        public static SensorType valueOf(int androidSensorType) {
            switch (androidSensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    return ACCELEROMETER;
                case Sensor.TYPE_GYROSCOPE:
                    return GYROSCOPE;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    return MAGNETIC_FIELD;
            }
            return null;
        }
        public int getType() {
            return type;
        }

        public static byte getAndroidSensorType(int androidSensorType) {
            SensorType t = valueOf(androidSensorType);
            if (t != null) {
                return (byte)t.getType();
            }
            else {
                return -1;
            }
        }
    }

    private final static int VERSION = 1;

    private DataOutputStream mOutputStream;
    private String mFileName;
    private int mNumRows;

    static {

    }

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
        mOutputStream.writeByte(VERSION);
        mOutputStream.writeByte(dataPoint.getSensorType().getType());
        mOutputStream.writeLong(dataPoint.getTimestamp());
        mOutputStream.writeFloat(dataPoint.getX());
        mOutputStream.writeFloat(dataPoint.getY());
        mOutputStream.writeFloat(dataPoint.getZ());
        mNumRows++;
    }

    public void writeLocation(Location location) throws IOException {
        mOutputStream.writeByte(VERSION);
        mOutputStream.writeByte(SensorType.LOCATION.getType());
        mOutputStream.writeLong(location.getTime());
        mOutputStream.writeDouble(location.getLatitude());
        mOutputStream.writeDouble(location.getLongitude());
        mOutputStream.writeDouble(location.getAltitude());
        mOutputStream.writeFloat(location.getAccuracy());
        mNumRows++;
    }

}
