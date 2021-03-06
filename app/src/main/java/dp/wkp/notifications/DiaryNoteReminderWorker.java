package dp.wkp.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import dp.wkp.R;
import dp.wkp.activities.BaseActivity;
import dp.wkp.activities.EditNoteActivity;
import dp.wkp.activities.MainActivity;
import dp.wkp.activities.SplashActivity;
import dp.wkp.db.notes.Note;
import dp.wkp.db.notes.NotesDB;
import dp.wkp.db.notes.NotesDao;
import dp.wkp.fragments.DiaryFragment;

/**
 * Work that sends a notification reminding the note
 */
public class DiaryNoteReminderWorker extends Worker {

    public static final String CHANNEL_ID = "diary_reminder";
    public static final int NOTIFICATION_ID = 888;
    private final Context context;

    public DiaryNoteReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    /**
     * Gets note from DB given the note id from extras. Then, sends a notification.
     * After that, updates note on DB, setting notification to false.
     * The call Main.getInstance() is used to know if the app is open or closed. If app is closed, it
     * will throw RuntimeException and the notification will open splash screen and then login. Else,
     * it will open EditNoteActivity with back stack DiaryFragment.
     *
     * @return
     */
    @NonNull
    @Override
    public Result doWork() {
        int id = getInputData().getInt("id", 0);
        NotesDao notesDao = NotesDB.getInstance(context).notesDao();
        Note note = notesDao.getNoteById(id);
        if (note.hasNotification()) {
            String subject = note.getNoteTitle();
            PendingIntent mainPendingIntent;
            try {
                MainActivity.getInstance();
                Intent noteIntent = new Intent(context, EditNoteActivity.class);
                noteIntent.putExtra(EditNoteActivity.NOTE_EXTRA_Key, note.getId());
                Intent backIntent = new Intent(context, MainActivity.class);
                backIntent.putExtra(MainActivity.FRAGMENT_TO_LOAD, DiaryFragment.DIARY_FRAGMENT);
                backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mainPendingIntent = PendingIntent.getActivities(context, note.getId(), new Intent[]{backIntent, noteIntent}, PendingIntent.FLAG_ONE_SHOT);//Android 12: PendingIntent.FLAG_IMMUTABLE
            } catch (RuntimeException e) {
                Intent splash = new Intent(context, SplashActivity.class);
                splash.putExtra(BaseActivity.NOTIFICATION_TYPE, NotificationType.DIARY_REMINDER);
                splash.putExtra(EditNoteActivity.NOTE_EXTRA_Key, note.getId());
                mainPendingIntent = PendingIntent.getActivity(context, note.getId(), splash, PendingIntent.FLAG_ONE_SHOT); //Android 12 PendingIntent.FLAG_MUTABLE
            }
            String text = note.getNum_enc() == 0 ? note.getNoteText() : String.valueOf(note.getNum_enc()).concat(context.getString(R.string.x_encrypted));

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setContentTitle(subject)
                    .setContentText(text)
                    .setContentIntent(mainPendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            note.setNotification(false);
            notesDao.updateNote(note);
        }
        return Result.success();
    }
}
