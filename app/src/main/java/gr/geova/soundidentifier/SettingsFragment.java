package gr.geova.soundidentifier;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        ListPreference recordingDuration = findPreference("duration_recording");
        if (recordingDuration.getValue() == null) {
            recordingDuration.setValueIndex(1); // 10 seconds, defined at values/arrays.xml file
        }
    }
}
