package dp.wkp.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.Locale;

import dp.wkp.R;
import dp.wkp.activities.BaseActivity;
import dp.wkp.activities.MainActivity;
import dp.wkp.notifications.NotificationPrayerReminder;
import dp.wkp.notifications.TimePreference;

/**
 * Settings fragment with options: change name, import picture from Google account, language, theme,
 * prayer notification switch and time.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements TimePickerDialog.OnTimeSetListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preference profile_picture = findPreference("profile_picture");
        profile_picture.setOnPreferenceClickListener(preference -> {
            MainActivity.getInstance().getImageUtils().deleteBoth();
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(MainActivity.HAS_LOADED_URL, false).commit();
            getActivity().recreate();
            return true;
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        EditTextPreference name = getPreferenceManager().findPreference(BaseActivity.PROFILE_NAME);
        name.setOnPreferenceChangeListener(getRecreateListener());

        ListPreference languages = getPreferenceManager().findPreference(BaseActivity.LANGUAGE);
        languages.setOnPreferenceChangeListener(getRecreateListener());

        SwitchPreferenceCompat prayerReminders = getPreferenceManager().findPreference(BaseActivity.NOTIFICATION);
        prayerReminders.setOnPreferenceChangeListener(new NotificationPrayerReminder(getContext()));

        TimePreference time = getPreferenceManager().findPreference(BaseActivity.TIME);
        time.setOnPreferenceChangeListener(new NotificationPrayerReminder(getContext()));

        ListPreference themes = getPreferenceManager().findPreference(BaseActivity.THEME);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            themes.setEnabled(false);
        } else {
            themes.setOnPreferenceChangeListener((preference, newValue) -> {
                String value = (String) newValue;
                BaseActivity.setTheme(value);
                return true;
            });
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public Preference.OnPreferenceChangeListener getRecreateListener() {
        return (preference, newValue) -> {
            SettingsFragment.this.getActivity().recreate();
            return true;
        };
    }

    /**
     * This method is used to open TimePickerDialog with time stored on SharedPreferencesDB
     *
     * @param preference
     */
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof TimePreference) {
            boolean is24hour = Locale.getDefault().getLanguage().equals("pt");
            TimePickerDialog dialogFragment;
            Integer minutesAfterMidnight = ((TimePreference) preference).getTime();
            if (minutesAfterMidnight != null) {
                int hours = minutesAfterMidnight / 60;
                int minutes = minutesAfterMidnight % 60;
                dialogFragment = TimePickerDialog.newInstance(this, hours, minutes, is24hour);
            } else {
                dialogFragment = TimePickerDialog.newInstance(this, is24hour);
            }
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    /**
     * Stores selected time on preferences db
     *
     * @param view
     * @param hourOfDay
     * @param minute
     * @param second
     */
    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        int hours = view.getSelectedTime().getHour();
        int minutes = view.getSelectedTime().getMinute();
        int minutesAfterMidnight = (hours * 60) + minutes;

        TimePreference timePreference = getPreferenceManager().findPreference(BaseActivity.TIME);
        // This allows the client to ignore the user value.
        if (timePreference.callChangeListener(
                minutesAfterMidnight)) {
            timePreference.setTime(minutesAfterMidnight);
        }
    }
}