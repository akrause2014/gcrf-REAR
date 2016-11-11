package epcc.ed.ac.uk.gcrf_rear.data;

import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Created by akrause on 09/11/2016.
 */

public class DataPoint
{
    private float x;
    private float y;
    private float z;
    private long timestamp;
    private int sensorType;
    private String tableName;

    public DataPoint(long timestamp, float[] values, int sensorType) {
        this.timestamp = timestamp;
        this.x = values[0];
        this.y = values[1];
        this.z = values[2];
        this.sensorType = sensorType;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getSensorType() {
        return sensorType;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        String name = "";
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                name = "Accelerometer";
            case Sensor.TYPE_GYROSCOPE:
                name = "Gyroscope";
            case Sensor.TYPE_MAGNETIC_FIELD:
                name = "MagneticField";
        }
        return name + ": timestamp=" + timestamp + ", (" + x + ", " + y + ", " + z + ")";
    }
}

