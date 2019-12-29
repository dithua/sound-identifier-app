package gr.geova.soundidentifier;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPOutputStream;

public class ResultsActivity extends AppCompatActivity implements AsyncResponse {

    static {
        System.loadLibrary("fingerprint-lib");
    }

    private native String fingerprint(short[] channel);

    private static final String LOG_TAG = "ResultsActivity";
    private static SharedPreferences sharedPreferences = null;

    private final LibraryHelper libraryHelper = LibraryHelper.getInstance(this);
    private boolean saveToDB;

    private TextView songText, noResultText;
    private SendAndReceiveData asyncTask = new SendAndReceiveData();
    private boolean ran = false; // TODO find something better

    /**
     * Get the raw PCM data from an AAC-encoded audio file.
     * @param filePath: the filepath of the audio file
     * @return the PCM data
     */
    @Nullable
    private short[] getPCMData(final String filePath) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ADTSDemultiplexer adts;

        try {
            adts = new ADTSDemultiplexer(new FileInputStream(filePath));
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            return null;
        }

        Decoder dec;
        try {
            dec = new Decoder(adts.getDecoderSpecificInfo());
        } catch (AACException e) {
            Log.e(LOG_TAG, e.getMessage());
            return null;
        }

        final SampleBuffer buf = new SampleBuffer();
        byte[] frame;
        while (true) {
            try {
                frame = adts.readNextFrame();
            } catch (Exception e) {
                break;
            }

            try {
                dec.decodeFrame(frame, buf);
            } catch (AACException e) {
                Log.e(LOG_TAG, e.getMessage());
                return null;
            }

            try {
                outputStream.write(buf.getData());
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                return null;
            }
        }

        byte[] outputStreamByteArray = outputStream.toByteArray();

        // convert byte[] to short[]
        short[] shorts = new short[outputStreamByteArray.length / 2];
        ByteBuffer.wrap(outputStreamByteArray).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);

        return shorts;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        saveToDB = sharedPreferences.getBoolean("save_to_db_switch", true);

        songText = findViewById(R.id.song_title);
        noResultText = findViewById(R.id.no_result);

        asyncTask.delegate = this;
    }

    @Override
    public void onStart() {
        super.onStart();

        final String filePath = getIntent().getStringExtra("FILEPATH");

        final short[] shorts = getPCMData(filePath);

        if (shorts == null) {
            Log.e(LOG_TAG, "shorts is null");
            Toast.makeText(this, R.string.generic_error_message, Toast.LENGTH_LONG).show();
            return;
        }

        String json = fingerprint(shorts); // call native method to get the fingerprints in JSON format

        if (json.isEmpty()) {
            Toast.makeText(this, R.string.generic_error_message, Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "json is empty");
        } else {
            if (!ran) {
                asyncTask.execute(json);
                ran = true;
            }
        }
    }

    /**
     * This method vibrates the device, if the device has a vibrator,
     * and the user has enabled vibration in the Settings.
     */
    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        boolean isVibrationAllowed = sharedPreferences.getBoolean("vibrate_device_switch", true);

        // Android Studio warns that vibrator can be null
        if (vibrator == null || !vibrator.hasVibrator() || !isVibrationAllowed) {
            return;
        }

        final int millisecondsToVibrate = 500;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millisecondsToVibrate, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millisecondsToVibrate);
        }
    }

    /**
     * This method inserts data into the SQLite Database of the device.
     * @param songName the name of the track, be it music or sound in general
     */
    private void insertToDB(final String songName) {
        SQLiteDatabase db = libraryHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LibraryHelper.COLUMN_SONG_NAME, songName);
        // date is added automatically (see LibraryHelper.java)

        long result = db.insert(LibraryHelper.TABLE_NAME, null, values);

        // in case the insertion failed
        if (result == -1) {
            Log.e(LOG_TAG, "DB insert() returned -1");
            Toast.makeText(this, R.string.database_insert_failed, Toast.LENGTH_SHORT).show();
        }

        db.close();
    }

    /**
     * Show the results on the screen.
     * @param responseData the data received from the server
     */
    @Override
    public void processFinish(final String responseData) {
        // print error messages in case something wrong happened with the connection to the server
        switch (responseData) {
            case ErrorCodes.MALFORMED_URL:
                Toast.makeText(this, R.string.malformed_ip_address, Toast.LENGTH_SHORT).show();
                return;
            case ErrorCodes.IO_EXCEPTION_SERVER:
                Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show();
                return;
            case ErrorCodes.IO_EXCEPTION_GENERIC:
                Toast.makeText(this, R.string.generic_error_message, Toast.LENGTH_SHORT).show();
                return;
        }

        vibrateDevice();

        // No result -- server didn't send a 200 status code
        if (responseData.isEmpty()) {
            noResultText.setText(R.string.no_result);
            noResultText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);

            return;
        }

        try {
            // parse server's response data
            JSONObject jsonObject = new JSONObject(responseData);

            String songName = jsonObject.getString(LibraryHelper.COLUMN_SONG_NAME);

            songText.setText(songName);

            songText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);

            if (saveToDB) {
                insertToDB(songName);
            }

        } catch (JSONException e) {
            Toast.makeText(this, R.string.generic_error_message, Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private static class SendAndReceiveData extends AsyncTask<String, Void, String> {
        AsyncResponse delegate;

        /**
         * This method establishes connection with the server,
         * sends and receives request and response data respectively.
         *
         * @param requestData the data to be sent to the server
         * @return Returns a String, server's response data (if it's empty, then server sent 404 Not Found).
         * It can also return a String from ErrorCodes.java, to indicate that something wrong happened with the connection with the server.
         */
        @Override
        protected String doInBackground(String... requestData) {
            final String IPAddress = sharedPreferences.getString("connection_settings_edit_text", "");
            final String port = "8080";

            final String URL = "http://" + IPAddress + ":" + port + "/retrieve";

            Log.i(LOG_TAG, URL);

            URL url;

            try {
                url = new URL(URL);
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "MALFORMED URL EXCEPTION!");
                return ErrorCodes.MALFORMED_URL;
            }

            // send request -- begin
            HttpURLConnection httpURLConnection;
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);

                httpURLConnection.setRequestProperty("Content-Type", "application/x-gzip");
                httpURLConnection.setRequestProperty("Content-Encoding", "gzip");

                httpURLConnection.setRequestProperty("Accept", "application/json");
                // it's my choice not to set "Accept-Encoding"=>"gzip", because the server sends a very, very small JSON anyway


                // compress JSON in gzip form
                GZIPOutputStream gzipStream = new GZIPOutputStream(httpURLConnection.getOutputStream());
                gzipStream.write(requestData[0].getBytes());
                gzipStream.close();

                // Sends the data uncompressed
                /*httpURLConnection.setRequestProperty("Content-Type", "application/json");

                DataOutputStream ds = new DataOutputStream(httpURLConnection.getOutputStream());
                ds.writeBytes(requestData[0]);
                ds.flush();
                ds.close();
                 */
            } catch (IOException e) {
                Log.e(LOG_TAG, "SERVER " + e.getMessage());
                return ErrorCodes.IO_EXCEPTION_SERVER;
            }
            // send request -- end

            // receive response -- begin
            try {
                if (httpURLConnection.getResponseCode() == 404) {
                    return "";
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "GENERIC " + e.getMessage());
                return ErrorCodes.IO_EXCEPTION_GENERIC;
            }

            StringBuilder response = new StringBuilder();
            try (InputStream in = new BufferedInputStream(httpURLConnection.getInputStream())) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                    String line;

                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "GENERIC " + e.getMessage());
                return ErrorCodes.IO_EXCEPTION_GENERIC;
            } finally {
                httpURLConnection.disconnect();
            }
            // receive response -- end

            final String responseData = response.toString();

            Log.i(LOG_TAG, "RESPONSE " + responseData);

            return responseData;
        }

        @Override
        protected void onPostExecute(final String jsonData) {
            delegate.processFinish(jsonData);
        }

    }

    @Override
    public void onBackPressed() {
        finish();
        Intent i = new Intent(ResultsActivity.this, MainActivity.class);
        startActivity(i);
        //TODO review finish()
        // read about stacks!
    }
}
