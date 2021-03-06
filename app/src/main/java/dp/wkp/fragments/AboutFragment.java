package dp.wkp.fragments;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.api.client.util.Charsets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import dp.wkp.R;

/**
 * Fragment that shows a HTML file (in english or portuguese, according to selected language) that
 * describes the app
 */
public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            InputStream functionalitiesHTML;
            if (Locale.getDefault().getLanguage().equals("pt"))
                functionalitiesHTML = getResources().getAssets().open("html/functionalities_pt.html");
            else
                functionalitiesHTML = getResources().getAssets().open("html/functionalities.html");
            String text;
            try (InputStreamReader reader = new InputStreamReader(functionalitiesHTML, Charsets.UTF_8)) {
                text = CharStreams.toString(reader);
                TextView tv = view.findViewById(R.id.textView);
                tv.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
