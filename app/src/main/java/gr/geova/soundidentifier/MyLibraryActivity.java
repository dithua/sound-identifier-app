package gr.geova.soundidentifier;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MyLibraryActivity extends AppCompatActivity {

    private final LibraryHelper libraryHelper = LibraryHelper.getInstance(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mylibrary);

        ListView resultsListView = findViewById(R.id.myLibraryListView);
        List<String> resultsList = fetchDataFromDB();

        if (resultsList == null) {
            AlertDialog alertDialog = new AlertDialog.Builder(MyLibraryActivity.this).create();
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
        } else {
            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, resultsList.toArray());
            resultsListView.setAdapter(adapter);
        }
    }

    private List<String> fetchDataFromDB() {
        SQLiteDatabase db = libraryHelper.getReadableDatabase();

        List<String> results = new ArrayList<>();
        String result;

        Resources resources = getResources();

        String audioTitle = resources.getString(R.string.audio_title);
        String date = resources.getString(R.string.date);

        Cursor cursor = db.query(LibraryHelper.TABLE_NAME, null, null, null, null, null, "_ID DESC");
        if (cursor.moveToFirst()) {
            do {
                result = String.format("%s: %s\n%s: %s", audioTitle, cursor.getString(cursor.getColumnIndex(LibraryHelper.COLUMN_SONG_NAME)),
                        date, cursor.getString(cursor.getColumnIndex(LibraryHelper.COLUMN_DATE)));

                results.add(result);
            } while (cursor.moveToNext());
        } else {
            return null;
        }

        cursor.close();
        db.close();

        return results;
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
            Intent i = new Intent(MyLibraryActivity.this, SettingsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.about) {
            Intent i = new Intent(MyLibraryActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
