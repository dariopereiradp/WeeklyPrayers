package dp.wkp.notifications;

import android.content.Context;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import dp.wkp.R;
import dp.wkp.activities.MainActivity;
import dp.wkp.adapters.NotesAdapter;
import dp.wkp.db.notes.Note;
import dp.wkp.db.notes.NotesDB;

/**
 * This class calls TimePickerDialog when date is set. When time is set, it will enqueueUniqueWork
 * (with note.getId() as name) to set a reminder for the selected date.
 */
public class DiaryNoteNotification implements com.wdullaer.materialdatetimepicker.time.TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    private final Context context;
    private final Note note;
    private final NotesAdapter.NoteHolder holder;
    private final NotesAdapter adapter;
    private LocalDateTime calendar;

    public DiaryNoteNotification(Context context, Note note, NotesAdapter.NoteHolder holder, NotesAdapter adapter) {
        this.context = context;
        this.note = note;
        this.holder = holder;
        this.adapter = adapter;
        calendar = LocalDateTime.now(ZoneId.systemDefault());
    }

    /**
     * When date is set, opens a TimePickerDialog with min_time = LocalDateTime.now(), that is, it's
     * not possible to set past hours.
     *
     * @param view
     * @param year
     * @param monthOfYear
     * @param dayOfMonth
     */
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        TimePickerDialog tpd;
        if (Locale.getDefault().getLanguage().equals("pt"))
            tpd = TimePickerDialog.newInstance(this, true);
        else
            tpd = TimePickerDialog.newInstance(this, false);
        LocalDate date = LocalDate.of(year, monthOfYear + 1, dayOfMonth);
        if (LocalDate.now().isEqual(date)) {
            LocalTime now = LocalTime.now();
            int hour = now.getHour();
            int minute = now.getMinute();
            int second = now.getSecond();
            tpd.setMinTime(hour, minute, second);
        }
        calendar = calendar.withYear(year).withMonth(monthOfYear + 1).withDayOfMonth(dayOfMonth);
        tpd.show(MainActivity.getInstance().getSupportFragmentManager(), "TimePickerNote");
    }

    /**
     * When time is set, creates a work to send a notification to remind the note.
     *
     * @param view
     * @param hourOfDay
     * @param minute
     * @param second
     */
    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        calendar = calendar.withHour(hourOfDay).withMinute(minute).withSecond(second);
        long number = calendar.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        note.setNoteDateTimeReminder(number);
        note.setNotification(true);
        NotesDB.getInstance(context).notesDao().updateNote(note);
        adapter.onNotificationStatusChange(holder, note);

        Data data = new Data.Builder().putInt("id", note.getId()).build();
        OneTimeWorkRequest notificationRequest = new OneTimeWorkRequest.Builder(DiaryNoteReminderWorker.class)
                .setInitialDelay(number - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInputData(data).build();
        WorkManager.getInstance(context).enqueueUniqueWork(String.valueOf(note.getId()), ExistingWorkPolicy.REPLACE, notificationRequest);
        Toast.makeText(context, context.getString(R.string.reminder_set) + calendar.format(DateTimeFormatter.ofPattern(context.getString(R.string.dateTimePatternSimple))), Toast.LENGTH_SHORT).show();
    }
}
