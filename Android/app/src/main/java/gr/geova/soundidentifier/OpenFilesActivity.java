package gr.geova.soundidentifier;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class OpenFilesActivity extends AppCompatActivity {

    private static final String LOG_TAG = "OpenFilesActivity";
    private final MediaUtils mediaUtils = new MediaUtils();

    /**
     * Get the directory where the audio files are stored.
     *
     * @return Returns a File to the directory
     */
    private File getDirectory() {
        File directory;

        if (getExternalFilesDir(null) != null) {
            directory = new File(getExternalFilesDir(null).getAbsolutePath());
        } else {
            directory = new File(getFilesDir().getAbsolutePath());
        }

        return directory;
    }

    /**
     * Get the file name of every file in a given directory.
     * @param directory a directory
     * @return Returns a list of strings which stores the name of every file in a given directory; null if something bad happened (dir not found, or an I/O error occurred)
     */
    private List<String> getFileNames(File directory) {
        File[] filesInDir = directory.listFiles();

        // filesInDir CAN be null
        if (filesInDir == null) {
            return null;
        }

        List<String> fileNameList = new ArrayList<>();

        for (File item : filesInDir) {
            if (item.isFile()) {
                fileNameList.add(item.getName());
            }
        }

        // The following code assumes that the files' name won't be changed from a file explorer app.
        // Otherwise, ParseException occurs but it's caught
        Collections.sort(fileNameList, new Comparator<String>() {
                    @Override
                    public int compare(String date1, String date2) {
                        try {
                            // reverse order
                            return -1 * AudioRecordActivity.simpleDateFormat.parse(date1).compareTo(AudioRecordActivity.simpleDateFormat.parse(date2));
                        } catch (ParseException e) {
                            e.printStackTrace();
                            return 0;
                        }
                    }
                }
        );

        return fileNameList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_files);

        final ListView fileListView = findViewById(R.id.filesListView);
        final File directory = getDirectory();

        final List<String> fileNameList = getFileNames(directory);

        // notify user that there's nothing to show
        if (fileNameList == null || fileNameList.isEmpty()) {
            AlertDialog alertDialog = new AlertDialog.Builder(OpenFilesActivity.this).create();
            alertDialog.setTitle(getResources().getString(R.string.notification));
            alertDialog.setMessage(getResources().getString(R.string.noRecord));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    });
            alertDialog.show();
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNameList);

        fileListView.setAdapter(adapter);

        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(OpenFilesActivity.this);
                builder.setTitle(R.string.play_delete_dialog_title);
                builder.setItems(R.array.OpenFileOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String filePath = directory.getAbsolutePath() + File.separator + adapter.getItem(position);

                        switch (which) {
                            case 0: // Play file
                                Toast.makeText(OpenFilesActivity.this, R.string.playing_started, Toast.LENGTH_SHORT).show();
                                mediaUtils.playMedia(filePath, false, LOG_TAG);
                                break;
                            case 1: // Delete file
                                boolean deleteResult = new File(filePath).delete();

                                if (deleteResult) {
                                    Toast.makeText(OpenFilesActivity.this, R.string.file_deleted, Toast.LENGTH_SHORT).show();

                                    // update UI after item removal
                                    adapter.remove(adapter.getItem(position));
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(OpenFilesActivity.this, R.string.file_not_deleted, Toast.LENGTH_SHORT).show();
                                }

                                break;
                        }
                    }
                }).show();

                return true;
            }
        });

        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String filePath = directory.getAbsolutePath() + File.separator + adapter.getItem(position);

                Log.i(LOG_TAG, "Filepath is " + filePath);

                // go to the next activity (show results on screen)
                Intent i = new Intent(OpenFilesActivity.this, ResultsActivity.class);
                i.putExtra("FILEPATH", filePath);

                startActivity(i);

                finish();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(LOG_TAG, "OnStop() called");
        mediaUtils.releaseResources();
    }
}