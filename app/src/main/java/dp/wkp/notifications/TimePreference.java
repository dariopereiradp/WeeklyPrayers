package dp.wkp.notifications;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dp.wkp.R;

/**
 * It's used to store notification time on sharedPreferencesDB as an int and display it as an
 * hour string format.
 */
public class TimePreference extends DialogPreference {

    private int mTime;

    public TimePreference(Context context) {
        this(context, null);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public TimePreference(Context context, AttributeSet attrs,
                          int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public TimePreference(Context context, AttributeSet attrs,
                          int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public int getTime() {
        return mTime;
    }

    public void setTime(int time) {
        mTime = time;
        // Save to Shared Preferences
        persistInt(time);
        updateSummary(time);
    }

    public void updateSummary(int minutesAfterMidnight) {
        int hours = minutesAfterMidnight / 60;
        int minutes = minutesAfterMidnight % 60;
        LocalTime localTime = LocalTime.of(hours, minutes);
        this.setSummary(localTime.format(DateTimeFormatter.ofPattern(getContext().getString(R.string.timePattern))));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // Default value from attribute. Fallback value is set to 0.
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        setTime(getPersistedInt(mTime));
    }
}
