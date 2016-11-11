package epcc.ed.ac.uk.gcrf_rear.data;

import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by akrause on 09/11/2016.
 */

public class DatabaseThread extends Thread {

    public Handler mHandler;
    private Integer mDisplaySensor = Sensor.TYPE_ACCELEROMETER;
    private long mDisplayDelay = 10000000; // 10,000,000 nanoseconds = 0.1 seconds for display updates

    private SurfaceHolder surfaceHolder;
    private final Paint paintX, paintY, paintZ;
    private CircularBuffer<DataPoint> mDataPoints;
    private boolean mDisplayOn = false;

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

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new Handler() {
            private int numRows = 0;
            private long prevTs = -1;
            @Override
            public void handleMessage(Message msg) {
                numRows++;
                DataPoint dataPoint = (DataPoint)msg.obj;
                if (dataPoint != null) {
                    long ts = dataPoint.getTimestamp();
                    if (mDisplayOn && dataPoint.getSensorType() == mDisplaySensor) {
                        mDataPoints.add(dataPoint);
                        if ((ts - prevTs) > mDisplayDelay) {
                            drawGraph();
                        }
                    }
                    prevTs = ts;
                }
                if ((numRows % 100) == 0) {
                    Log.e("database", "Wrote " + numRows + " records. Value: " + dataPoint);
                }
            }
        };
        Looper.loop();
    }

    private void drawGraph()
    {
        if (surfaceHolder == null) return;
        Canvas canvas = surfaceHolder.lockCanvas();
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
//                    canvas.drawLine((prev.getY()*x_offset)/m + x_offset, stepSize * (i - 1) + y_offset, (dp.getY()*x_offset)/m + x_offset, stepSize * i + y_offset, paintY);
//                    canvas.drawLine((prev.getZ()*x_offset)/m + x_offset, stepSize * (i - 1) + y_offset, (dp.getY()*x_offset)/m + x_offset, stepSize * i + y_offset, paintZ);
            }
            i++;
            prev = dp;
        }
        surfaceHolder.unlockCanvasAndPost(canvas);

    }

    public void displaySensor(int sensorType) {
        mDisplaySensor = sensorType;
        mDataPoints.clear();
    }

    public void setDisplayOn(boolean displayOn) {
        mDisplayOn = displayOn;
    }
}
