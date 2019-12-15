package gr.geova.soundidentifier;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ListFilesActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ListFilesActivity";

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

        ListView resultsListView = findViewById(R.id.resultsListView);

        final File directory = setDirectory();

        List<String> fileNameList = getFileNames(directory.listFiles());

        if (fileNameList == null) {
            return;
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, fileNameList.toArray());

        resultsListView.setAdapter(adapter);

        resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String fileName = directory.getAbsolutePath() + File.separator + adapter.getItem(position);
                Log.i(LOG_TAG, "Filename is " + fileName);

                // TODO insert Dialog (YES, NO) to playback track before sending it into the other activity

                Intent i = new Intent(ListFilesActivity.this, ResultsActivity.class);
                i.putExtra("FILENAME", fileName);

                startActivity(i);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(ListFilesActivity.this, SettingsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.my_library) {
            Intent i = new Intent(ListFilesActivity.this, MyLibraryActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
