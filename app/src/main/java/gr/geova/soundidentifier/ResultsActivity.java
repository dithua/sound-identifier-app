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
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
    private final int channelCount = 1; // made channelCount final, because the audio channels are now only one.

    @Nullable
    private short[] getPCMData(final String fileName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ADTSDemultiplexer adts;

        try {
            adts = new ADTSDemultiplexer(new FileInputStream(fileName));

            //channelCount = adts.getChannelCount();
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

        final String fileName = getIntent().getStringExtra("FILENAME");

        final short[] shorts = getPCMData(fileName);

        if (shorts == null) {
            Log.e(LOG_TAG, "shorts is null");
            Toast.makeText(this, R.string.generic_error_message, Toast.LENGTH_LONG).show();
            return;
        }

        String json = fingerprint(shorts);

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

    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Android Studio warns that vibrator can be null
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        final int millisecondsToVibrate = 500;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millisecondsToVibrate, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millisecondsToVibrate);
        }
    }

    private void insertToDB(String songName) {
        SQLiteDatabase db = libraryHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LibraryHelper.COLUMN_SONG_NAME, songName);

        long result = db.insert(LibraryHelper.TABLE_NAME, null, values);

        if (result == -1) {
            Log.e(LOG_TAG, "DB insert() returned -1");
        }

        db.close();
    }

    @Override
    public void processFinish(String responseData) {
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

        if (responseData.isEmpty()) {
            noResultText.setText(R.string.no_result);
            noResultText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);

            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(responseData);

            String songName = jsonObject.getString(LibraryHelper.COLUMN_SONG_NAME);

            songText.setText(songName);

            songText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f); // TODO add support for different kinds of screens

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

        @Override
        protected String doInBackground(String... requestData) {
            final String IPAddress = sharedPreferences.getString("connection_settings_edit_text", "");
            final String port = "8080";

            final String URL = String.format("http://%s:%s/retrieve", IPAddress, port);

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
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                // todo probably add gzip?

                DataOutputStream ds = new DataOutputStream(httpURLConnection.getOutputStream());

                ds.writeBytes(requestData[0]);

                ds.flush();
                ds.close();
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
                e.printStackTrace();
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
        protected void onPostExecute(String jsonData) {
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
