package gr.geova.soundidentifier;

import android.Manifest;
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
import androidx.core.app.ActivityCompat;

import java.io.IOException;

// Portions of this code are from https://developer.android.com/guide/topics/media/mediarecorder. Licensed under Apache 2.0 (https://source.android.com/license).

public class AudioRecordActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private static String fileName = null;
    private MediaRecorder mediaRecorder = null;
    private CountDownTimer countDownTimer = null;

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            Log.i(LOG_TAG, "Timer stopped");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        /* TODO getExternalCacheDir() is only for TEMPORARY files!!!
            Add code that saves the file in the internal or external(?) storage (based on the SETTINGS!!!).*/
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";
        Log.e(LOG_TAG, "filename is : " + fileName);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        final boolean start = true;

        Button stopRecordingButton = findViewById(R.id.stop_recording_button);
        stopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecord(!start);
                cancelTimer();
                finish();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        onRecord(true);

        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setProgress(0);
        // record for 10.000 ms = 10 s TODO allow the user to change it in the Settings!!!
        countDownTimer = new CountDownTimer(10_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedTime = 10_000 - millisUntilFinished;
                int total = (int) (((double) elapsedTime/(double) 10_000)*100.0);
                progressBar.setProgress(total);

                Log.i(LOG_TAG, "Timer running...");
            }

            @Override
            public void onFinish() {
                onRecord(false);

                // ONLY for development purposes
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
                        }
                    });
                } catch (IOException e) {
                    Log.e(LOG_TAG, "prepare() failed");
                }

                finish();
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

    /*private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }*/

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
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

}
