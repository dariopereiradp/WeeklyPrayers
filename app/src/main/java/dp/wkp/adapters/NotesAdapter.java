package dp.wkp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkManager;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import dp.wkp.R;
import dp.wkp.activities.MainActivity;
import dp.wkp.callbacks.NoteEventListener;
import dp.wkp.db.notes.Note;
import dp.wkp.db.notes.NotesDB;
import dp.wkp.db.notes.NotesDao;
import dp.wkp.utils.DateConverter;
import dp.wkp.notifications.DiaryNoteNotification;

/**
 * Class to view notes on RecyclerView
 * Here and with layout xml file I can change the design the way I want.
 * (for example, in a future update, maybe I want to have a layout like Google Keep)
 */
public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteHolder> {
    private final Context context;
    private final ArrayList<Note> notes;
    private NoteEventListener listener;
    private boolean multiCheckMode = false;
    private final NotesDao dao;


    public NotesAdapter(Context context) {
        this.context = context;
        dao = NotesDB.getInstance(context).notesDao();
        this.notes = new ArrayList<>();
        notes.addAll(dao.getNotes());
    }


    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.note_layout, parent, false);
        return new NoteHolder(v);
    }

    /**
     * Method that initializes notes attributes (text, title, date, notification icon, etc)
     *
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull final NoteHolder holder, int position) {
        final Note note = getNote(position);
        if (note != null) {
            holder.getLayout().getBackground().setTint(note.getColor() == 0 ? context.getColor(R.color.note_backg) : note.getColor());
            holder.getNoteTitle().setText(note.getNoteTitle());
            if (note.getNum_enc() == 0) {
                holder.getNoteText().setText(note.getNoteText());
            } else {
                holder.getNoteText().setText(note.getNum_enc() + context.getString(R.string.x_encrypted));
                holder.getNoteText().setTextColor(context.getColor(R.color.red));
                holder.getLock().setImageResource(R.drawable.ic_enc_black_24dp);
            }
            holder.getNoteDate().setText(String.format("%s%s", context.getString(R.string.last_modified), DateConverter.dateFromLong(note.getNoteDate())));
            onNotificationStatusChange(holder, note);
            // init note click event
            holder.itemView.setOnClickListener(view -> listener.onNoteClick(note));
            holder.getCheckBox().setOnClickListener(view -> listener.onNoteClick(note));
            // init note long click
            holder.itemView.setOnLongClickListener(view -> {
                listener.onNoteLongClick(note);
                return false;
            });

            holder.getNotification().setOnClickListener(v -> {
                if (note.hasNotification()) {
                    Toast.makeText(context, context.getString(R.string.notification_off), Toast.LENGTH_SHORT).show();
                    note.setNotification(false);
                    NotesDB.getInstance(context).notesDao().updateNote(note);
                    WorkManager.getInstance(context).cancelUniqueWork(String.valueOf(note.getId()));
                    onNotificationStatusChange(holder, note);
                } else {
                    DatePickerDialog dpd = DatePickerDialog.newInstance(new DiaryNoteNotification(context, note, holder, NotesAdapter.this));
                    dpd.setMinDate(Calendar.getInstance());
                    dpd.show(MainActivity.getInstance().getSupportFragmentManager(), "DatePickerNote");
                }
            });
            // check checkBox if note selected
            if (multiCheckMode) {
                holder.getCheckBox().setVisibility(View.VISIBLE); // show checkBox if multiMode on
                holder.getCheckBox().setChecked(note.isChecked());
            } else holder.getCheckBox().setVisibility(View.GONE); // hide checkBox if multiMode off
        }
    }

    /**
     * Changes notification icon and reminder text
     *
     * @param holder
     * @param note
     */
    public void onNotificationStatusChange(NoteHolder holder, Note note) {
        if (!note.hasNotification()) {
            holder.reminderDate.setVisibility(View.GONE);
            holder.notification.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_notifications_off_24dp));
        } else {
            holder.notification.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_notifications_active_24px));
            holder.reminderDate.setVisibility(View.VISIBLE);
            holder.reminderDate.setText(String.format("%s%s", context.getString(R.string.reminder_set), DateConverter.dateFromLong(note.getNoteDateTimeReminder())));
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    /**
     * Gets note from position in descending order (because we want new notes to appear first)
     *
     * @param position
     * @return
     */
    public Note getNote(int position) {
        return notes.get(notes.size() - position - 1);
    }

    /**
     * List of all checked notes
     *
     * @return
     */
    public List<Note> getCheckedNotes() {
        List<Note> checkedNotes = new ArrayList<>();
        for (Note n : this.notes) {
            if (n.isChecked())
                checkedNotes.add(n);
        }
        return checkedNotes;
    }

    public void setListener(NoteEventListener listener) {
        this.listener = listener;
    }

    /**
     * MultiCheck mode enables check boxes to be checked.
     * If is false, them, uncheck all notes
     *
     * @param multiCheckMode
     */
    public void setMultiCheckMode(boolean multiCheckMode) {
        this.multiCheckMode = multiCheckMode;
        if (!multiCheckMode)
            for (Note note : this.notes) {
                note.setChecked(false);
            }
        notifyDataSetChanged();
    }

    /**
     * Modifies notes list. The list will contain only notes whose text or title matches text.
     * If text is empty, list will be reloaded with all notes from db.
     *
     * @param text
     */
    public void filter(String text) {
        notes.clear();
        if (!text.isEmpty()) {
            text = text.toLowerCase();
            for (Note note : dao.getNotes()) {
                if (note.getNoteText().toLowerCase().contains(text) || note.getNoteTitle().toLowerCase().contains(text))
                    notes.add(note);
            }
        } else {
            notes.addAll(dao.getNotes());
        }
        notifyDataSetChanged();
    }

    /**
     * Class that represents a single note graphically. Here and with layout xml file I can change
     * the design the way I want.
     */
    public static class NoteHolder extends RecyclerView.ViewHolder {
        private final LinearLayout layout;
        private final TextView noteTitle;
        private final TextView noteText;
        private final TextView noteDate;
        private final TextView reminderDate;
        private final CheckBox checkBox;
        private final ImageView lock;
        private final ImageView notification;

        public NoteHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.layout);
            noteTitle = itemView.findViewById(R.id.note_title);
            noteDate = itemView.findViewById(R.id.note_date);
            noteText = itemView.findViewById(R.id.note_text);
            checkBox = itemView.findViewById(R.id.checkBox);
            reminderDate = itemView.findViewById(R.id.reminder_date);
            lock = itemView.findViewById(R.id.enc);
            notification = itemView.findViewById(R.id.notification_note);
        }

        public LinearLayout getLayout() {
            return layout;
        }

        public TextView getNoteTitle() {
            return noteTitle;
        }

        public TextView getNoteText() {
            return noteText;
        }

        public TextView getNoteDate() {
            return noteDate;
        }

        public TextView getReminderDate() {
            return reminderDate;
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

        public ImageView getLock() {
            return lock;
        }

        public ImageView getNotification() {
            return notification;
        }
    }
}
