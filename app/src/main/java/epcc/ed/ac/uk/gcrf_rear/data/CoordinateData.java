package epcc.ed.ac.uk.gcrf_rear.data;

import android.provider.BaseColumns;

/**
 * Created by akrause on 08/11/2016.
 */

public class CoordinateData implements BaseColumns
{
    public static final String TIMESTAMP = "timestamp";
    public static final String VALUE_X = "value_x";
    public static final String VALUE_Y = "value_y";
    public static final String VALUE_Z = "value_z";
    public static final String ACCURACY = "accuracy";

    private CoordinateData() {
        // only static methods
    }
}
