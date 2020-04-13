package gr.geova.soundidentifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static gr.geova.soundidentifier.MediaUtils.playMedia;

// Portions of this code are from https://developer.android.com/guide/topics/media/mediarecorder. Licensed under Apache 2.0 (https://source.android.com/license).

public class AudioRecordActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordActivity";

    private static String filePath = null;
    private SharedPreferences sharedPreferences = null;
    private MediaRecorder mediaRecorder = null;
    private CountDownTimer countDownTimer = null;
    private boolean recordingFinished = false;
    private int recordingDuration;

    /**
     * This method cancels the audio recording
     */
    private void cancelRecording() {
        if (!recordingFinished) {
            onRecord(false);
            new File(filePath).delete();
            cancelTimer();
            finish(); // exit activity
        }
    }

    /**
     * This method stops the countdown timer.
     */
    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            Log.i(LOG_TAG, "Timer stopped");
        }
    }

    /**
     * This method sets the filepath of the audio file.
     */
    private void setFilePath() {
        final boolean keepRecordedFiles = sharedPreferences.getBoolean("keep_recorded_files_switch", false);
        Log.i(LOG_TAG, "keepRecordedFiles: " + keepRecordedFiles);

        if (keepRecordedFiles) {
            // replaced HH:mm:ss with '.' because ':' must not be a part of a filename!
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE dd-MM-yyyy HH.mm.ss", Locale.US);

            if (getExternalFilesDir(null) != null) {
                Log.i(LOG_TAG, "getExternalFilesDir!");
                filePath = getExternalFilesDir(null).getAbsolutePath();
            } else {
                Log.i(LOG_TAG, "getFilesDir!");
                filePath = getFilesDir().getAbsolutePath();
            }

            filePath += File.separator + simpleDateFormat.format(new Date()) + ".aac";
        } else {

            if (getExternalCacheDir() != null) {
                Log.i(LOG_TAG, "getExternalCacheDir!");
                filePath = getExternalCacheDir().getAbsolutePath();
            } else {
                Log.i(LOG_TAG, "getCacheDir!");
                filePath = getCacheDir().getAbsolutePath();
            }

            filePath += File.separator + "temp_audio.aac";
        }

        Log.i(LOG_TAG, "filepath is : " + filePath);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        setFilePath();

        Button stopRecordingButton = findViewById(R.id.stop_recording_button);
        stopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRecording();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        final int oneSecondToMilliseconds = 1_000;

        // get recording duration from the Settings (in milliseconds)
        recordingDuration = Integer.parseInt(sharedPreferences.getString("duration_recording", "10")) * oneSecondToMilliseconds;
        Log.i(LOG_TAG, "Recording duration in seconds: " + recordingDuration / oneSecondToMilliseconds);

        onRecord(true); // start recording

        final ProgressBar progressBar = findViewById(R.id.progressBar);

        progressBar.setProgress(0);

        countDownTimer = new CountDownTimer(recordingDuration, oneSecondToMilliseconds) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedTime = recordingDuration - millisUntilFinished;
                int total = (int) (((double) elapsedTime / (double) recordingDuration) * 100.0);
                progressBar.setProgress(total);

                Log.i(LOG_TAG, "Timer running...");
            }

            @Override
            public void onFinish() {
                onRecord(false); // stop recording

                progressBar.setProgress(100);

                recordingFinished = true;

                final boolean playbackTrack = sharedPreferences.getBoolean("playback_track_switch", false);
                Log.i(LOG_TAG, "Playback Track: " + playbackTrack);

                if (playbackTrack) {
                    playMedia(filePath, true, LOG_TAG);
                }

                // go to the next activity (show results on screen)
                Intent i = new Intent(AudioRecordActivity.this, ResultsActivity.class);
                i.putExtra("FILEPATH", filePath);
                startActivity(i);
            }
        }.start();
    }

    /**
     * Starts or stops recording audio, according to the boolean parameter.
     * @param start If set to true, record, otherwise stop recording
     */
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    /**
     * This method creates a MediaRecorder object, and starts recording from the microphone.
     */
    private void startRecording() {
        // for dev purposes
        /*if (filePath == null) {
            Log.e(LOG_TAG, "You called startRecording() before setting up a filepath!");
            return;
        }*/

        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioChannels(1); // why have 2 channels when most Android devices have mono mics?
        mediaRecorder.setAudioEncodingBitRate(16 * 44100);
        mediaRecorder.setMaxDuration(recordingDuration);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setOutputFile(filePath);

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

    @Override
    public void onBackPressed() {
        cancelRecording();
    }

}
