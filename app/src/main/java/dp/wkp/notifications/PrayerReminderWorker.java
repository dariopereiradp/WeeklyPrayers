package dp.wkp.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDate;

import dp.wkp.R;
import dp.wkp.activities.BaseActivity;
import dp.wkp.activities.MainActivity;
import dp.wkp.activities.PrayerDayActivity;
import dp.wkp.activities.SplashActivity;
import dp.wkp.db.prayercards.PrayerCard;
import dp.wkp.db.prayercards.PrayerCardDB;
import dp.wkp.fragments.PrayerFragment;
import dp.wkp.utils.DaysOfWeek;

/**
 * Worker to PrayerReminder notification
 */
public class PrayerReminderWorker extends Worker {
    public static final String CHANNEL_ID = "prayer_reminder";
    public static final int NOTIFICATION_ID = 777;
    private final Context context;

    public PrayerReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    /**
     * This method calculates the current week day and will show a notification for this specific day
     * only if the day has notification on and has not been set to "Prayed" this week.
     * There is no special reason to use (day + 1) * 10 on PendingIntent requestCode.
     * <p>
     * The call Main.getInstance() is used to know if the app is open or closed. If app is closed, it
     * will throw RuntimeException and the notification will open splash screen and then login. Else,
     * it will open PrayerDayActivity with MainActivity back stack.
     * After that, it will update alarm for the next day.
     *
     * @return
     */
    @NonNull
    @Override
    public Result doWork() {
        int day = LocalDate.now().getDayOfWeek().getValue();
        if (day == 7)
            day = 1;
        else
            day++;
        PrayerCard prayerCard = PrayerCardDB.getInstance(context).prayerCardsDao().getCardById(day);
        System.out.println(prayerCard);
        if (!prayerCard.isPrayed() && prayerCard.hasNotification()) {
            String dayOffWeek = DaysOfWeek.values()[day - 1].getDescription(context);
            String subject = prayerCard.getSubtitle();
            PendingIntent mainPendingIntent;
            try {
                MainActivity.getInstance();
                Intent prayerDay = new Intent(context, PrayerDayActivity.class);
                prayerDay.putExtra(PrayerFragment.CARD_ID, day);
                Intent backIntent = new Intent(context, MainActivity.class);
                backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mainPendingIntent = PendingIntent.getActivities(context, (day + 1) * 10, new Intent[]{backIntent, prayerDay}, PendingIntent.FLAG_ONE_SHOT); //Android 12: PendingIntent.FLAG_IMMUTABLE
            } catch (RuntimeException e) {
                Intent splash = new Intent(context, SplashActivity.class);
                splash.putExtra(BaseActivity.NOTIFICATION_TYPE, NotificationType.PRAYER_REMINDER);
                splash.putExtra(PrayerFragment.CARD_ID, day);
                mainPendingIntent = PendingIntent.getActivity(context, (day + 1) * 10, splash, PendingIntent.FLAG_ONE_SHOT); //Android 12: PendingIntent.FLAG_MUTABLE
            }

            Intent intentAction = new Intent(context, MarkDoneAction.class);
            intentAction.putExtra("id", prayerCard.getId());
            PendingIntent pIntentActionDone = PendingIntent.getBroadcast(context, 77777, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setContentTitle(subject)
                    .setContentText(dayOffWeek)
                    .setContentIntent(mainPendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(prayerCard.getText()))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .addAction(R.drawable.ic_done_24px, context.getString(R.string.praying), pIntentActionDone);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
        updateAlarm();
        return Result.success();
    }

    /**
     * Gets prayer reminder notification time and calls #updateAlarm from NotificationPrayerReminder
     */
    public void updateAlarm() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        int time = sharedPrefs.getInt(BaseActivity.TIME, 0);
        new NotificationPrayerReminder(context).updateAlarm(time, false);
    }
}
