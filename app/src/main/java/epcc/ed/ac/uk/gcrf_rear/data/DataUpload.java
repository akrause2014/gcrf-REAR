package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by akrause on 11/11/2016.
 */

public class DataUpload
{

    public static boolean exists(Context context, String name) {
        return new File(context.getExternalFilesDir(null), name).isFile();
    }

    public static DataInputStream open(Context context, String name) throws FileNotFoundException {
        return new DataInputStream(
                new BufferedInputStream(
                        new FileInputStream(
                                new File(context.getExternalFilesDir(null), name))));
    }

    public static DataPoint readRecord(DataInputStream inputStream) throws IOException {
        int sensorType = inputStream.read();
        float x = inputStream.readFloat();
        float y = inputStream.readFloat();
        float z = inputStream.readFloat();
        long timestamp = inputStream.readLong();
        return new DataPoint(timestamp, x, y, z, (byte)sensorType);
    }


}
