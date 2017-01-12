package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by akrause on 12/01/2017.
 */

public class TimeSetReceiver extends BroadcastReceiver {

    public static final String SYSTEM_TIME = "SystemTime";
    public static final String ELAPSED_REAL_TIME = "ElapsedRealTime";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(SYSTEM_TIME, System.currentTimeMillis());
        editor.putLong(ELAPSED_REAL_TIME, SystemClock.elapsedRealtime());
        editor.commit();
        Log.d("application", "Time changed.");
    }
}

