package epcc.ed.ac.uk.gcrf_rear.data;

import android.hardware.Sensor;

/**
 * Created by akrause on 09/11/2016.
 */

public class DataPoint
{
    private float x;
    private float y;
    private float z;
    private final long timestamp;
    private DataStore.SensorType sensorType;

    public DataPoint(long timestamp, float[] values, DataStore.SensorType sensorType) {
        this(timestamp, values[0], values[1], values[2], sensorType);
    }

    public DataPoint(long timestamp, float x, float y, float z, DataStore.SensorType sensorType) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
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

    public DataStore.SensorType getSensorType() {
        return sensorType;
    }

    @Override
    public String toString() {
        return sensorType + ": timestamp=" + timestamp + ", (" + x + ", " + y + ", " + z + ")";
    }

}

