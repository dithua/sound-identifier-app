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

            fileName = getExternalFilesDir(null).getAbsolutePath() + "/" + simpleDateFormat.format(new Date()) + ".3gp";;
        } else {
            fileName = getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
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

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(fileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

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
