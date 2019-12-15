package gr.geova.soundidentifier;

import android.Manifest;
import android.content.Intent;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Portions of this code are from https://developer.android.com/guide/topics/media/mediarecorder. Licensed under Apache 2.0 (https://source.android.com/license).

public class AudioRecordActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;
    private SharedPreferences sharedPreferences = null;
    private MediaRecorder mediaRecorder = null;
    private CountDownTimer countDownTimer = null;
    private boolean recordingFinished = false;
    private int recordingDuration;

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
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE dd-MM-yyyy HH:mm:ss", Locale.US);

            if (getExternalFilesDir(null) != null) {
                Log.i(LOG_TAG, "getExternalFilesDir!");
                fileName = getExternalFilesDir(null).getAbsolutePath();
            } else {
                Log.i(LOG_TAG, "getFilesDir!");
                fileName = getFilesDir().getAbsolutePath();
            }

            fileName += "/" + simpleDateFormat.format(new Date()) + ".aac";
        } else {

            if (getExternalCacheDir() != null) {
                Log.i(LOG_TAG, "getExternalCacheDir!");
                fileName = getExternalCacheDir().getAbsolutePath();
            } else {
                Log.i(LOG_TAG, "getCacheDir!");
                fileName = getCacheDir().getAbsolutePath();
            }

            fileName += "/audiorecordtest.aac";
        }

        Log.i(LOG_TAG, "filename is : " + fileName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
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
                    new File(fileName).delete();
                    cancelTimer();
                    finish(); // exit activity
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!permissionToRecordAccepted) {
            finish();
            return; // prevents the app from crashing!
        }

        recordingDuration = Integer.parseInt(sharedPreferences.getString("duration_recording", "10")) * 1_000; // in milliseconds
        Log.i(LOG_TAG, "Recording duration in seconds: " + recordingDuration / 1_000);

        onRecord(true);

        final ProgressBar progressBar = findViewById(R.id.progressBar);
        final int countDownInterval = 1_000;

        progressBar.setProgress(0);

        countDownTimer = new CountDownTimer(recordingDuration, countDownInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedTime = recordingDuration - millisUntilFinished;
                int total = (int) (((double) elapsedTime / (double) recordingDuration) * 100.0);
                progressBar.setProgress(total);

                Log.i(LOG_TAG, "Timer running...");
            }

            @Override
            public void onFinish() {
                onRecord(false);

                progressBar.setProgress(100);

                recordingFinished = true;

                final boolean playback_track = sharedPreferences.getBoolean("playback_track", false);

                // The following code deliberately blocks the UI.
                if (playback_track) {
                    final MediaPlayer player = new MediaPlayer();
                    try {
                        player.setDataSource(fileName);
                        player.prepare();
                        player.start();

                        while (player.isPlaying()) {}

                        player.stop();
                        player.reset();
                        player.release();

                        // the below callback didn't block the UI
                        /*player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                player.stop();
                                player.reset();
                                player.release();
                            }
                        });*/
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "MediaPlayer's prepare() failed");
                    }
                }

                Intent i = new Intent(AudioRecordActivity.this, ResultsActivity.class);
                i.putExtra("FILENAME", fileName);
                startActivity(i);
            }
        }.start();
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        // for dev purposes
        if (fileName == null) {
            Log.e(LOG_TAG, "You called startRecording() before setting up a filename!");
            return;
        }

        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioChannels(2); // it isn't guaranteed that the audio channels will be 2 !!!
        mediaRecorder.setAudioEncodingBitRate(16 * 44100);
        mediaRecorder.setMaxDuration(recordingDuration);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setOutputFile(fileName);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "MediaRecorder's prepare() failed");
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
