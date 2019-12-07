package gr.geova.soundidentifier;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

// Portions of this code are from https://developer.android.com/guide/topics/media/mediarecorder. Licensed under Apache 2.0 (https://source.android.com/license).

public class AudioRecordActivity extends AppCompatActivity {

    static {
        System.loadLibrary("fingerprint-lib");
    }
    private native void fingerprint(double[] doubles);

    private static final String LOG_TAG = "AudioRecordActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;
    private SharedPreferences sharedPreferences = null;
    private MediaRecorder mediaRecorder = null;
    private CountDownTimer countDownTimer = null;
    private boolean recordingFinished = false;

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
        }
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            Log.i(LOG_TAG, "Timer stopped");
        }
    }

    private void setFileName() {
        final boolean keepRecordedFiles = sharedPreferences.getBoolean("keep_recorded_files_switch", false);
        if (keepRecordedFiles) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE-MM_d-yy_HH:mm:ss", Locale.US);

            if (getExternalFilesDir(null) != null) {
                fileName = getExternalFilesDir(null).getAbsolutePath();
            } else {
                fileName = getFilesDir().getAbsolutePath();
            }

            fileName += "/" + simpleDateFormat.format(new Date()) + ".aac";//".3gp";
        } else {

            if (getExternalCacheDir() != null) {
                fileName = getExternalCacheDir().getAbsolutePath();
            } else {
                fileName = getCacheDir().getAbsolutePath();
            }

            //fileName += "/audiorecordtest.3gp";
            fileName += "/audiorecordtest.aac";
        }
        Log.i(LOG_TAG, "filename is : " + fileName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO ) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionToRecordAccepted = true;
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        setFileName();

        Button stopRecordingButton = findViewById(R.id.stop_recording_button);
        stopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recordingFinished) {
                    onRecord(false);
                    cancelTimer();
                    finish(); // exit activity
                }
            }
        });
    }
    public static double[] toDoubleArray(byte[] byteArray){
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for(int i=0;i<doubles.length;i++){
            doubles[i] = ByteBuffer.wrap(byteArray, i*times, times).getDouble();
        }
        return doubles;
    }
    @Override
    public void onStart() {
        super.onStart();

        if (permissionToRecordAccepted) {
            onRecord(true);

            final ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setProgress(0);

            final int recordingDuration = Integer.parseInt(sharedPreferences.getString("duration_recording", "10 seconds"))*1_000; // converted to milliseconds
            Log.i(LOG_TAG, "Recording duration in seconds: " + recordingDuration/1_000);

            countDownTimer = new CountDownTimer(recordingDuration, 1_000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long elapsedTime = recordingDuration - millisUntilFinished;
                    int total = (int) (((double) elapsedTime/(double) recordingDuration)*100.0);
                    progressBar.setProgress(total);

                    Log.i(LOG_TAG, "Timer running...");
                }

                @Override
                public void onFinish() {
                    onRecord(false);

                    recordingFinished = true;

                    final boolean playback_track = sharedPreferences.getBoolean("playback_track", false);

                    if (playback_track) {
                        final MediaPlayer player = new MediaPlayer();
                        try {
                            player.setDataSource(fileName);
                            player.prepare();
                            player.start();
                            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    player.stop();
                                    player.reset();
                                    player.release();
                                    finish(); // exit activity ???
                                }
                            });
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "prepare() failed");
                        }
                    } else {

                        // Step 1
                        // TODO it works! But it has to be in another place!
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ADTSDemultiplexer adts = null;
                        try {
                            adts = new ADTSDemultiplexer(new FileInputStream(fileName));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Decoder dec = null;
                        try {
                            dec = new Decoder(adts.getDecoderSpecificInfo());
                        } catch (AACException e) {
                            e.printStackTrace();
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
                                e.printStackTrace();
                            }
                            try {
                                outputStream.write(buf.getData());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        byte[] outputStreamByteArray = outputStream.toByteArray();
                        //short[] shorts = new short[outputStreamByteArray.length / 2];
                        double[] s = toDoubleArray(outputStreamByteArray);
                        //ByteBuffer.wrap(outputStreamByteArray).order(ByteOrder.BIG_ENDIAN).asDoubleBuffer().get(s);
                        /*ByteBuffer.wrap(outputStreamByteArray).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);

                        if (adts.getChannelCount() == 2) {
                            short[][] channels = new short[2][shorts.length/2];

                            int j = 0, k = 0;
                            for (int i = 0; i < shorts.length; i++) {
                                if (i % 2 == 0) {
                                    channels[0][j++] = shorts[i];
                                } else {
                                    channels[1][k++] = shorts[i];
                                }
                            }
                        }*/

                        // Step 2 -- unnecessary
                        /*try {
                            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
                            InputStream is = new FileInputStream(fileName);
                            int n = 0;
                            byte[] buffer = new byte[1048576];  // pow(2,20)
                            while (n != -1) {
                                n = is.read(buffer);
                                if (n > 0) {
                                    messageDigest.update(buffer, 0, n);
                                }
                            }
                            String SHA1 = new BigInteger(1, messageDigest.digest()).toString(16);
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        fingerprint(s);



                        finish(); // exit activity
                    }
                }
            }.start();
        } else {
            finish();
        }
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }
//TODO add default value 10 seconds!!! SOS
    private void startRecording() {
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioChannels(2); // it isn't guaranteed that the audio channels will be 2 !!!
        mediaRecorder.setAudioEncodingBitRate(16*44100); // TODO review this!
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setOutputFile(fileName);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mediaRecorder.start();
        Log.i(LOG_TAG, "MediaRecorder started");
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            Log.i(LOG_TAG, "MediaRecorder released");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
            Log.i(LOG_TAG, "MediaRecorder released");
        }
    }

}
