package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.TextView;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by akrause on 09/11/2016.
 */

public class DatabaseThread extends Thread {

    public static final int SENSOR_MSG = 1;
    public static final int LOCATION_MSG = 2;

    public Handler mHandler;
    public Context mContext;

    private DataStore mCurrentStore;
    private long mFileStoreLength = 60*1000000000l; // 1 minute in nanoseconds
    private boolean mFileStoreOn = false;

    private DataStore.SensorType mDisplaySensor = DataStore.SensorType.ACCELEROMETER;
    private SurfaceHolder surfaceHolder;
    private final Paint paintX, paintY, paintZ;
    private CircularBuffer<DataPoint> mDataPoints;
    private boolean mDisplayOn = false;
    private TextView mLocationTextView;

    public DatabaseThread() {

        paintX = new Paint();
        paintX.setColor(Color.BLUE);
        paintX.setStrokeWidth(4);
        paintY = new Paint();
        paintY.setColor(Color.RED);
        paintY.setStrokeWidth(4);
        paintZ = new Paint();
        paintZ.setColor(Color.GREEN);
        paintZ.setStrokeWidth(4);
        mDataPoints = new CircularBuffer<>(100);

    }

    public void setSurfaceHolder(SurfaceHolder holder) {
        surfaceHolder = holder;
    }

    public void setSensorTextView(TextView textView) {
        mLocationTextView = textView;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setDataSize(int dataSize) {
        if (dataSize > 0) {
            Log.d("database", "set data size to: " + dataSize);
            mFileStoreLength = dataSize*60l*1000000000l; // minutes
        }
    }

    public DataStore getCurrentStore() {
        return mCurrentStore;
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new Handler() {
            private int numRows = 0;
            private long startTime = Long.MAX_VALUE;
            @Override
            public void handleMessage(Message msg) {
                numRows++;
                switch (msg.arg1) {
                    case LOCATION_MSG:
                        handleLocationMessage(msg);
                        break;
                    case SENSOR_MSG:
                        handleSensorMessage(msg);
                        break;
                }
            }
            private DataStore getFileStore(long timestamp) throws IOException {
                if (mFileStoreOn) {
                    if (mCurrentStore == null) {
                        mCurrentStore = new DataStore(mContext);
                    } else if ((timestamp-startTime) > mFileStoreLength) {
                        numRows = 0;
                        startTime = timestamp;
                        mCurrentStore.close();
                        mCurrentStore = new DataStore(mContext);
                    }
                    return mCurrentStore;
                }
                return null;
            }
            private void handleLocationMessage(Message msg) {
//                Log.d("database", "Received location message");
                try {
                    Location location = (Location) msg.obj;
                    DataStore store = getFileStore(location.getTime()*1000000); // time is in milliseconds
                    if (store != null) {
                        store.writeLocation(location);
                        if (mLocationTextView != null) {
                            mLocationTextView.setText("GPS location available:\nLon/Lat: "
                                    + location.getLongitude() + "," + location.getLatitude());
                        }
                    }
                } catch (IOException e) {
                    Log.e("database", "error opening file store", e);
                }
            }
            private void handleSensorMessage(Message msg) {
                DataPoint dataPoint = (DataPoint)msg.obj;
//                if ((numRows % 100) == 0) {
//                    Log.d("database", "Received " + numRows + " records. Value: " + dataPoint);
//                }
                if (dataPoint != null) {
                    try {
                        DataStore store = getFileStore(dataPoint.getTimestamp());
                        if (store != null) {
                            store.writeRecord(dataPoint);
                        }
                    } catch (IOException e) {
                        Log.e("database", "error writing data", e);
                    }
                    if (mDisplayOn) {
                        if (dataPoint.getSensorType() == mDisplaySensor) {
                            mDataPoints.add(dataPoint);
                            drawGraph();
                        }
                    }
                }

            }
        };
        Looper.loop();
    }

    private void drawGraph()
    {
        SurfaceHolder sh = surfaceHolder;
        if (sh == null) return;
        Canvas canvas = sh.lockCanvas();
        canvas.drawColor(Color.WHITE);
        Paint paintLine = new Paint();
        paintLine.setColor(Color.BLACK);
        paintLine.setStrokeWidth(2);
        int radius = 4;
        int x_offset = canvas.getWidth() / 2;
        int y_offset = 50;
        canvas.drawLine(x_offset, 0, x_offset, canvas.getHeight(), paintLine);
        DataPoint prev = null;
        Iterator<DataPoint> iter = mDataPoints.iterator();
        List<DataPoint> dps = new LinkedList<>();
        float m = 1;
        while (iter.hasNext()) {
            DataPoint dp = iter.next();
            m = Math.max(Math.abs(dp.getX()), m);
            m = Math.max(Math.abs(dp.getY()), m);
            m = Math.max(Math.abs(dp.getZ()), m);
            dps.add(dp);
        }

        int stepSize = (canvas.getHeight() - 2 * y_offset) / dps.size();
        int i = 0;
        for (DataPoint dp : dps) {
            canvas.drawCircle((dp.getX()*x_offset)/m+x_offset, stepSize*i+y_offset, radius, paintX);
            canvas.drawCircle((dp.getY()*x_offset)/m+x_offset, stepSize*i+y_offset, radius, paintY);
            canvas.drawCircle((dp.getZ()*x_offset)/m+x_offset, stepSize*i+y_offset, radius, paintZ);
            if (prev != null) {
                canvas.drawLine((prev.getX()*x_offset)/m + x_offset, stepSize * (i - 1) + y_offset, (dp.getX()*x_offset)/m + x_offset, stepSize * i + y_offset, paintX);
                canvas.drawLine((prev.getY()*x_offset)/m + x_offset, stepSize * (i - 1) + y_offset, (dp.getY()*x_offset)/m + x_offset, stepSize * i + y_offset, paintY);
                canvas.drawLine((prev.getZ()*x_offset)/m + x_offset, stepSize * (i - 1) + y_offset, (dp.getZ()*x_offset)/m + x_offset, stepSize * i + y_offset, paintZ);
            }
            i++;
            prev = dp;
        }
        sh.unlockCanvasAndPost(canvas);

    }

    public void close() {
        try {
            if (mCurrentStore != null) {
                mCurrentStore.close();
            }
        } catch (IOException e) {
            Log.e("database", "error closing data file", e);
        }
        finally {
            mCurrentStore = null;
        }
    }

    public void setDisplaySensor(DataStore.SensorType sensorType) {
        mDisplaySensor = sensorType;
        mDataPoints.clear();
    }

    public void setDisplayOn(boolean displayOn) {
        mDisplayOn = displayOn;
    }

    public void setFileStoreOn(boolean fileStoreOn) {
        mFileStoreOn = fileStoreOn;
    }
}
