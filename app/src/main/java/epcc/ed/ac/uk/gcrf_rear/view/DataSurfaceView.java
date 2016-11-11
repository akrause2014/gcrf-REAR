package epcc.ed.ac.uk.gcrf_rear.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import epcc.ed.ac.uk.gcrf_rear.data.DatabaseThread;

/**
 * Created by akrause on 09/11/2016.
 */

public class DataSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private Paint paint = new Paint();
    private DatabaseThread mDatabaseThread;

    public DataSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);

        paint.setAntiAlias(true);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }


//    public void setDataPoints(CircularBuffer<DataPoint> points) {
//        mDataPoints = points;
//    }

    public void setDatabaseThread(DatabaseThread databaseThread) {
        mDatabaseThread = databaseThread;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.e("view", "created");
        mDatabaseThread.setSurfaceHolder(surfaceHolder);
//        mDiagramThread = new DiagramThread(getHolder(), mDataPoints);
//        mDiagramThread.setDrawing(true);
//        mDiagramThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {
        Log.e("view", "drawing graph");
        mDatabaseThread.setSurfaceHolder(surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mDatabaseThread.setSurfaceHolder(null);
//        mDiagramThread.setDrawing(false);
//        try {
//            mDiagramThread.join();
//        } catch (InterruptedException e) {
//            // ignore
//        }
    }

}
