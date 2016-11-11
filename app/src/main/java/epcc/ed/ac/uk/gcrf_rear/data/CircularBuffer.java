package epcc.ed.ac.uk.gcrf_rear.data;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by akrause on 09/11/2016.
 */

public class CircularBuffer<T>  {

    private LinkedBlockingDeque<T> mData;
    private int mCapacity;

    public CircularBuffer(int capacity) {
        mData = new LinkedBlockingDeque<T>(capacity);
        mCapacity = capacity;
    }

    public int getCapacity() {
        return mCapacity;
    }

    public synchronized void add(T obj) {
        if (!mData.offer(obj)) {
            mData.poll();
            mData.offer(obj);
        };
    }

    public synchronized T remove() {
        return mData.poll();
    }

    public synchronized void clear() {
        mData.clear();
    }

    public Iterator<T> iterator() {
        return mData.iterator();
    }

}
