package gr.geova.soundidentifier;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        ListPreference recordingDuration = findPreference("duration_recording");
        if (recordingDuration.getValue() == null) {
            recordingDuration.setValueIndex(1); // 10 seconds, defined at values/arrays.xml file
        }

        EditTextPreference editTextPreference = findPreference("connection_settings_edit_text");
        if (editTextPreference.getText() == null) {
            editTextPreference.setText("");
        }

        Preference clearLibrary = findPreference("clear_my_library");
        clearLibrary.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                LibraryHelper libraryHelper = LibraryHelper.getInstance(getContext());
                                SQLiteDatabase db = libraryHelper.getWritableDatabase();
                                libraryHelper.deleteRows(db);
                                db.close();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }

                    }
                };

                Resources resources = getResources();

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                alertDialogBuilder.setTitle(resources.getString(R.string.warning)).
                        setMessage(resources.getString(R.string.clear_my_library_dialog)).
                        setPositiveButton(resources.getString(R.string.yes), dialogClickListener).
                        setNegativeButton(resources.getString(R.string.no), dialogClickListener).show();

                return true;
            }
        });
    }
}
