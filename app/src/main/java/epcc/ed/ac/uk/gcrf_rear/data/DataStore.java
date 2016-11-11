package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by akrause on 11/11/2016.
 */

public class DataStore {

    private DataOutputStream mOutputStream;
    private String mFileName;

    public DataStore(Context context, long count) throws IOException {
        if (isExternalStorageWritable()) {
            mFileName = "data-" + count + ".dat";
            Log.d("data store", "writing to file: " + mFileName);
            openFile(context.getExternalFilesDir(null), mFileName);
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
            Log.d("data store", "closed file: " + mFileName);
            mFileName = null;
        }
    }

    public void writeRecord(DataPoint dataPoint) throws IOException {
        mOutputStream.write(dataPoint.sensorAsByte());
        mOutputStream.writeFloat(dataPoint.getX());
        mOutputStream.writeFloat(dataPoint.getY());
        mOutputStream.writeFloat(dataPoint.getZ());
        mOutputStream.writeLong(dataPoint.getTimestamp());
    }

}
