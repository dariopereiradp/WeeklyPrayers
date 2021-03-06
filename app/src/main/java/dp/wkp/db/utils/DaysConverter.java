package dp.wkp.db.utils;

import androidx.room.TypeConverter;

import dp.wkp.utils.DaysOfWeek;

/**
 * Converts enum type DaysOfWeek to String, to store on DB and vice-versa.
 */
public class DaysConverter {


    @TypeConverter
    public static String toStringField(DaysOfWeek day) {
        return day.toString();
    }

    @TypeConverter
    public static DaysOfWeek toDaysOfWeek(String day) {
        return DaysOfWeek.valueOf(day);
    }
}