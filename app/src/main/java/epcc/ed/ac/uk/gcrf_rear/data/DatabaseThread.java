package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.TextView;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import epcc.ed.ac.uk.gcrf_rear.R;

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
        Log.d("database", "set data size to: " + dataSize);

        if (dataSize > 0) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
            String value = settings.getString(mContext.getString(R.string.pref_key_frequency), null);
            int defaultRate = mContext.getResources().getInteger(R.integer.default_frequency);
            int freq = defaultRate;
            if (value != null) {
                try {
                    freq = Integer.valueOf(value);
                    if (freq <= 0) freq = defaultRate;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            mFileStoreLength = dataSize * 60 * freq;
            Log.d("database", "max number of records in file store: " + mFileStoreLength);
        }
    }

    public DataStore getCurrentStore() {
        return mCurrentStore;
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new Handler() {
            private long startTime = -1;
            @Override
            public void handleMessage(Message msg) {
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
                        startTime = timestamp;
                        Log.d("upload", "New data store: "+ startTime);
                    }
                    else if (mCurrentStore.getNumRows() >= mFileStoreLength) {
                        startTime = timestamp;
                        mCurrentStore.close();
                        mCurrentStore = new DataStore(mContext);
                        Log.d("upload", "Closed data store: " + startTime);
                    }
                    return mCurrentStore;
                }
                return null;
            }
            private void handleLocationMessage(Message msg) {
//                Log.d("database", "Received location message");
//                Location location = (Location) msg.obj;
//                if (location != null) {
//                    long timeInMillisUTC = location.getTime();
//                }
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
                Log.d("upload", "Closed data store: " + mCurrentStore.getTimestamp());
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
