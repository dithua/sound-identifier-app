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
import java.util.ArrayList;
import java.util.List;

import static gr.geova.soundidentifier.MediaUtils.playMedia;

public class OpenFilesActivity extends AppCompatActivity {

    private static final String LOG_TAG = "OpenFilesActivity";

    private File setDirectory() {
        File directory;

        if (getExternalFilesDir(null) != null) {
            directory = new File(getExternalFilesDir(null).getAbsolutePath());
        } else {
            directory = new File(getFilesDir().getAbsolutePath());
        }

        return directory;
    }

    private List<String> getFileNames(File[] listOfFiles) {
        // listOfFiles CAN be null
        if (listOfFiles == null) {
            return null;
        }

        List<String> fileNameList = new ArrayList<>();

        for (File item : listOfFiles) {
            if (item.isFile()) {
                fileNameList.add(item.getName());
            }
        }

        return fileNameList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_files);

        final ListView fileListView = findViewById(R.id.filesListView);
        final File directory = setDirectory();

        final List<String> fileNameList = getFileNames(directory.listFiles());

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

        final ArrayAdapter<Object> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNameList.toArray());

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
                                playMedia(filePath, false, LOG_TAG);
                                break;
                            case 1: // Delete file
                                boolean deleteResult = new File(filePath).delete();

                                // TODO find way to update the UI (in case a file is deleted)

                                if (deleteResult) {
                                    Toast.makeText(OpenFilesActivity.this, R.string.file_deleted, Toast.LENGTH_SHORT).show();
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

                Intent i = new Intent(OpenFilesActivity.this, ResultsActivity.class);
                i.putExtra("FILEPATH", filePath);

                startActivity(i);

                //finish(); // TODO review
            }
        });
    }
}
