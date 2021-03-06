package dp.wkp.db.notes;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import dp.wkp.R;
import dp.wkp.utils.DateConverter;

/**
 * Class that represents a note
 */

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id; // default value
    @ColumnInfo(name = "title")
    private String noteTitle;
    @ColumnInfo(name = "text")
    private String noteText;
    @ColumnInfo(name = "date")
    private long noteDate;
    @ColumnInfo(name = "notification")
    private boolean notification;
    @ColumnInfo(name = "dateTime_reminder")
    private long noteDateTimeReminder;
    @ColumnInfo(name = "color")
    private int color;

    @Ignore // we don't want to store this value on database so ignore it
    private boolean checked = false;

    @ColumnInfo(name = "n_enc")
    private int num_enc;

    @Ignore
    public Note() {
    }

    public Note(String noteTitle, String noteText, long noteDate, int num_enc) {
        this.noteTitle = noteTitle;
        this.noteText = noteText;
        this.noteDate = noteDate;
        this.num_enc = num_enc;
        this.color = 0;
    }

    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public long getNoteDate() {
        return noteDate;
    }

    public void setNoteDate(long noteDate) {
        this.noteDate = noteDate;
    }

    public int getId() {
        return id;
    }

    public int getNum_enc(){
        return num_enc;
    }

    public void setNum_enc(int num_enc){
        if(num_enc < 0)
            num_enc=0;
        this.num_enc = num_enc;
    }

    public void incrementar_enc(){
        num_enc++;
    }

    public void decrementar_enc(){
        setNum_enc(num_enc - 1);
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean hasNotification() {
        return notification;
    }

    public void setNotification(boolean notification) {
        this.notification = notification;
    }

    public long getNoteDateTimeReminder() {
        return noteDateTimeReminder;
    }

    public void setNoteDateTimeReminder(long noteDateTimeReminder) {
        this.noteDateTimeReminder = noteDateTimeReminder;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Gets a string representing this note to share text to other apps
     *
     * @param context
     * @return
     */
    public String toStringShare(Context context) {
        return noteTitle + "\n\n" + getNoteText() + "\n\n" + context.getString(R.string.createOn) +
                DateConverter.dateFromLong(getNoteDate()) + "\n" + context.getString(R.string.by) +
                context.getString(R.string.app_name);
    }
}
