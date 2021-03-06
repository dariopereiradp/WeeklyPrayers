package dp.wkp.notifications;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import dp.wkp.db.prayercards.PrayerCard;
import dp.wkp.db.prayercards.PrayerCardDB;
import dp.wkp.db.prayercards.PrayerCardsDao;

/**
 * Marks the prayer card as "Prayed" and cancels (that is, dismiss) notification
 */
public class MarkDoneAction extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("id", 0);
        PrayerCardsDao dao = PrayerCardDB.getInstance(context).prayerCardsDao();
        PrayerCard prayerCard = dao.getCardById(id);
        prayerCard.setPrayed(true);
        dao.updatePrayerCard(prayerCard);

        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(PrayerReminderWorker.NOTIFICATION_ID);
    }
}
