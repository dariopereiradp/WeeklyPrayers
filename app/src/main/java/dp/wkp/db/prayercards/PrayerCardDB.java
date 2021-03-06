package dp.wkp.db.prayercards;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import dp.wkp.db.utils.DaysConverter;

@Database(entities = PrayerCard.class, version = 1, exportSchema = false)
@TypeConverters(DaysConverter.class)
public abstract class PrayerCardDB extends RoomDatabase {
    public abstract PrayerCardsDao prayerCardsDao();

    public static final String DATABASE_NAME = "prayerCardsDb";
    private static PrayerCardDB instance;

    public static PrayerCardDB getInstance(Context context) {
        if (instance == null)
            instance = Room.databaseBuilder(context, PrayerCardDB.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .build();
        return instance;
    }
}
