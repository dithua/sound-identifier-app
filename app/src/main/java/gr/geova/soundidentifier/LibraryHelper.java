package gr.geova.soundidentifier;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LibraryHelper extends SQLiteOpenHelper {

    private static LibraryHelper libraryHelperInstance = null;

    public static final String TABLE_NAME = "LIBRARY";

    public static final String _ID = "_ID";
    public static final String COLUMN_SONG_NAME = "song_name";
    public static final String COLUMN_DATE = "date";

    private static final String DB_NAME = "LIBRARY_DB";
    private static final int DATABASE_VERSION = 1;
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_SONG_NAME + " VARCHAR(150) NOT NULL, " + COLUMN_DATE + " REAL DEFAULT (datetime('now', 'localtime')));";

    public static synchronized LibraryHelper getInstance(Context context) {
        if (libraryHelperInstance == null) {
            libraryHelperInstance = new LibraryHelper(context);
        }

        return libraryHelperInstance;
    }

    private LibraryHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void deleteFromTable(SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }
}
