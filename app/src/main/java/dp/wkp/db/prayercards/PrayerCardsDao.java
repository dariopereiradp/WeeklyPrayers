package dp.wkp.db.prayercards;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PrayerCardsDao {
    /**
     * Insert and save day to Database
     *
     * @param day
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPrayerCard(PrayerCard day);

    /**
     * Update day
     *
     * @param day the card that will be update
     */
    @Update
    void updatePrayerCard(PrayerCard day);

    /**
     * List All Notes From Database
     *
     * @return list of PrayerCards
     */
    @Query("SELECT * FROM prayer_cards")
    List<PrayerCard> getPrayerCards();

    /**
     * @param cardId card id
     * @return Card
     */
    @Query("SELECT * FROM prayer_cards WHERE id = :cardId")
    PrayerCard getCardById(int cardId);

}
