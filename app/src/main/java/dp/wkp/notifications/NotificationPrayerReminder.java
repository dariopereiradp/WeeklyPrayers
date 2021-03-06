package dp.wkp.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import dp.wkp.R;
import dp.wkp.activities.BaseActivity;

/**
 * Class to set the work to a prayer reminder notification
 */
public class NotificationPrayerReminder implements Preference.OnPreferenceChangeListener {

    public static final String PRAYER_REMINDER_TAG = "prayer_reminder";
    private final SharedPreferences sharedPrefs;
    private final Context context;

    public NotificationPrayerReminder(Context context) {
        this.context = context;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    /**
     * Updates notification work with new time (replaces old or, if doesn't exists, create new one)
     *
     * @param time
     * @param toast
     */
    public void updateAlarm(int time, boolean toast) {
        int hours = time / 60;
        int minutes = time % 60;
        Calendar calNow = Calendar.getInstance();
        Calendar calendar = (Calendar) calNow.clone();
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        String day = context.getString(R.string.today);
        if (calendar.compareTo(calNow) <= 0) {
            calendar.add(Calendar.DATE, 1);
            day = context.getString(R.string.tomorrow);
        }
        OneTimeWorkRequest notificationRequest = new OneTimeWorkRequest.Builder(PrayerReminderWorker.class)
                .setInitialDelay(calendar.getTimeInMillis() - System.currentTimeMillis(), TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance(context).enqueueUniqueWork(PRAYER_REMINDER_TAG, ExistingWorkPolicy.REPLACE, notificationRequest);
        if (toast) {
            String description = context.getString(R.string.next_reminder, day) + new SimpleDateFormat(context.getString(R.string.timePattern)).format(calendar.getTime());
            Toast.makeText(context, description, Toast.LENGTH_LONG).show();
        }
    }

    public void cancelNotification() {
        WorkManager.getInstance(context).cancelUniqueWork(PRAYER_REMINDER_TAG);
    }

    /**
     * Handles both ON & OFF notification change and change time preference
     *
     * @param preference
     * @param o
     * @return
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        boolean notifications;
        if (preference instanceof SwitchPreferenceCompat && preference.getKey().equals(BaseActivity.NOTIFICATION)) {
            notifications = (boolean) o;
            if (!notifications)
                cancelNotification();
            else
                updateAlarm(sharedPrefs.getInt(BaseActivity.TIME, 0), true);
        } else if (preference instanceof TimePreference)
            updateAlarm((Integer) o, true);
        return true;
    }
}
