package gr.geova.soundidentifier;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.WebView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

// https://www.bignerdranch.com/blog/open-source-licenses-and-android/
public class LicensesFragment extends DialogFragment {

    public static LicensesFragment newInstance() {
        return new LicensesFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        WebView webView = (WebView) LayoutInflater.from(getActivity()).inflate(R.layout.about_layout, null);
        webView.loadUrl("file:///android_asset/licenses.html");

        return new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert).
                setTitle(R.string.action_about).setView(webView).setPositiveButton("OK", null).create();
    }
}
