package gr.geova.soundidentifier;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MediaUtils {

    private MediaPlayer player;

    public MediaUtils() {
        player = new MediaPlayer();
    }

    /**
     * This method plays audio from a file.
     *
     * @param filePath the path to the audio file
     * @param blockUI  if set to true, it blocks the UI
     * @param LOG_TAG  a log tag
     */
    public void playMedia(String filePath, boolean blockUI, String LOG_TAG) {
        try {
            if (player.isPlaying()) {
                player.stop();
                player.reset();
            }
            player.setDataSource(filePath);
            player.prepare();
            player.start();

            if (blockUI) {
                while (player.isPlaying()) { }

                player.stop();
                player.reset();
                player.release();
            } else {
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.stop();
                        mp.reset();
//                      mp.release(); // see releaseResources()
                    }
                });
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "MediaPlayer's prepare() failed");
        }
    }

    public void releaseResources() {
        player.release();
    }
}