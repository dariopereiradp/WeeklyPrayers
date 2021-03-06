package dp.wkp.db.prayercards;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import dp.wkp.R;
import dp.wkp.utils.DaysOfWeek;

/**
 * Class that represents a prayer day
 */
@Entity(tableName = "prayer_cards")
public class PrayerCard {

    @PrimaryKey(autoGenerate = true)
    private int id; // default value
    @ColumnInfo(name = "day")
    private DaysOfWeek day;
    @ColumnInfo(name = "subtitle")
    private String subtitle;
    @ColumnInfo(name = "text")
    private String text;
    @ColumnInfo(name = "prayed")
    private boolean prayed;
    @ColumnInfo(name = "notification")
    private boolean notification;
    @ColumnInfo(name = "color")
    private int color;


    public PrayerCard(DaysOfWeek day) {
        this.day = day;
        subtitle = "";
        text = "";
        prayed = false;
        notification = true;
        this.color = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public DaysOfWeek getDay() {
        return day;
    }

    public void setDay(DaysOfWeek day) {
        this.day = day;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isPrayed() {
        return prayed;
    }

    public void setPrayed(boolean prayed) {
        this.prayed = prayed;
    }

    public boolean hasNotification() {
        return notification;
    }

    public void setNotification(boolean notification) {
        this.notification = notification;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @NonNull
    @Override
    public String toString() {
        return "PrayerCard{" +
                "id=" + id +
                ", day=" + day +
                ", subtitle='" + subtitle + '\'' +
                ", text='" + text + '\'' +
                '}';
    }

    /**
     * Gets a string representing this card to share text to other apps
     *
     * @param context
     * @return
     */
    public String toStringShare(Context context) {
        return context.getString(R.string.prayer_card) + " - " + day.getDescription(context)
                + "\n\n" + subtitle + "\n" + text + "\n\n" + context.getString(R.string.by) +
                context.getString(R.string.app_name);
    }
}
