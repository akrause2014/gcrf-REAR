package epcc.ed.ac.uk.gcrf_rear;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by akrause on 17/01/2017.
 */

public class Logger {

    public static Writer getLogWriter(Context context, boolean append) throws IOException
    {
        String fileName = context.getString(R.string.log_file);
        File dir = context.getExternalFilesDir(null);
        return new BufferedWriter(new FileWriter(new File(dir, fileName), append));
    }

    public static BufferedReader getLogReader(Context context) throws FileNotFoundException {
        String fileName = context.getString(R.string.log_file);
        File dir = context.getExternalFilesDir(null);
        return new BufferedReader(new FileReader(new File(dir, fileName)));
    }

    public static boolean deleteLogs(Context context) {
        String fileName = context.getString(R.string.log_file);
        File dir = context.getExternalFilesDir(null);
        return new File(dir, fileName).delete();
    }

    public static Writer getLogWriter(Context context) throws IOException
    {
        return getLogWriter(context, true);
    }

    public static void error(Context context, String message, Throwable e) {
        StringWriter writer = new StringWriter();
        PrintWriter p = new PrintWriter(writer);
        p.append(message);
        e.printStackTrace(p);
        p.close();
        log(context, writer.toString());
    }

    public static void log(Context context, String message) {
        try {
            Writer writer = Logger.getLogWriter(context);
            writer.write(message);
            writer.close();
        } catch (IOException e) {
            Log.d("logger", "Failed to write to log file", e);
        }
    }

}
