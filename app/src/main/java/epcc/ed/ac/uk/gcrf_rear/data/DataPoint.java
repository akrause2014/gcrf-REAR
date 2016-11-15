package epcc.ed.ac.uk.gcrf_rear.data;

import android.hardware.Sensor;

/**
 * Created by akrause on 09/11/2016.
 */

public class DataPoint
{
    private final int version = 1;
    private float x;
    private float y;
    private float z;
    private final long timestamp;
    private int sensorType;

    public DataPoint(long timestamp, float[] values, int sensorType) {
        this(timestamp, values[0], values[1], values[2], sensorType);
    }

    public DataPoint(long timestamp, float x, float y, float z, byte sensorAsByte) {
        this(timestamp, x, y, z, -1);
        switch (sensorAsByte) {
            case 1: sensorType = Sensor.TYPE_ACCELEROMETER; break;
            case 2: sensorType = Sensor.TYPE_GYROSCOPE; break;
            case 3: sensorType = Sensor.TYPE_MAGNETIC_FIELD; break;
        }
    }

    public DataPoint(long timestamp, float x, float y, float z, int sensorType) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.sensorType = sensorType;
    }

    public int getVersion() {
        return version;
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

    public byte sensorAsByte() {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return 1;
            case Sensor.TYPE_GYROSCOPE:
                return 2;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return 3;
            default:
                return -1;
        }
    }

    @Override
    public String toString() {
        String name = "";
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                name = "Accelerometer";
                break;
            case Sensor.TYPE_GYROSCOPE:
                name = "Gyroscope";
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                name = "MagneticField";
                break;
        }
        return name + ": timestamp=" + timestamp + ", (" + x + ", " + y + ", " + z + ")";
    }

}

