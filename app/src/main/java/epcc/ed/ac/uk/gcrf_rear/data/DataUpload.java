package epcc.ed.ac.uk.gcrf_rear.data;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by akrause on 11/11/2016.
 */

public class DataUpload
{

    public static boolean exists(Context context, String name) {
        return new File(context.getExternalFilesDir(null), name).isFile();
    }

    public static DataInputStream open(Context context, String name) throws FileNotFoundException {
        return new DataInputStream(
                new BufferedInputStream(
                        new FileInputStream(
                                new File(context.getExternalFilesDir(null), name))));
    }

    public static DataInputStream open(File file) throws FileNotFoundException {
        return new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)));
    }
//    public static DataPoint readRecord(DataInputStream inputStream) throws IOException {
//        int sensorType = inputStream.read();
//        float x = inputStream.readFloat();
//        float y = inputStream.readFloat();
//        float z = inputStream.readFloat();
//        long timestamp = inputStream.readLong();
//        return new DataPoint(timestamp, x, y, z, (byte)sensorType);
//    }

    public static int isRegistered(String target) {
        try {
            URL url = new URL(target);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(false);
            con.connect();
            int status = con.getResponseCode();
            return status;
        }
        catch (MalformedURLException e) {
            Log.e("upload", "malformed URL", e);
        } catch (ProtocolException e) {
            Log.e("upload", "error", e);
        } catch (ConnectException e) {
            Log.e("upload", "no connection", e);
        } catch (IOException e) {
            Log.e("upload", "error", e);
        }
        return 0;
    }

    public static boolean uploadFile(String dataURL, File file) {
        try {
            URL url = new URL(dataURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/octet-stream");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();
            OutputStream outputStream = con.getOutputStream();
            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            byte[] buf = new byte[2048];
            int c;
            while ((c = inputStream.read(buf)) >= 0) {
                outputStream.write(buf, 0, c);
            }
            inputStream.close();
            outputStream.close();
            int status = con.getResponseCode();
            if (status/100 == 2) {
                InputStream is = con.getInputStream();
                if (is != null) {
                    while (is.read(buf) != -1) {
                        // Log.d("upload input", new String(buf));
                    }
                }
                InputStream es = con.getErrorStream();
                if (es != null) {
                    while ((c = es.read(buf)) != -1) {
                        Log.d("upload error", new String(buf, 0, c));
                    }
                }
            }
            else {
                InputStream es = con.getErrorStream();
                if (es != null) {
                    while ((c = es.read(buf)) != -1) {
                        Log.d("upload error", new String(buf, 0, c));
                    }
                }
                Log.d("upload", "server returned status " + status);
                return false;
            }
            return true;
        }
        catch (MalformedURLException e) {
            Log.e("upload", "malformed URL", e);
        } catch (ProtocolException e) {
            Log.e("upload", "error", e);
        } catch (ConnectException e) {
            Log.e("upload", "no connection", e);
        } catch (IOException e) {
            Log.e("upload", "error", e);
        }
        return false;
    }


}
