package dp.wkp.utils;

import androidx.room.TypeConverter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

import dp.wkp.R;
import dp.wkp.activities.MainActivity;

/**
 * Just returns a String date given a long time
 */
public class DateConverter {

    /**
     * Just returns a String date given a long time
     *
     * @param time
     * @return
     */
    public static String dateFromLong(long time) {
        Locale local = Locale.getDefault();
        DateFormat format = new SimpleDateFormat(MainActivity.getInstance().getResources().getString(R.string.dateTimePatternSimple), local);
        return format.format(new Date(time));
    }

}