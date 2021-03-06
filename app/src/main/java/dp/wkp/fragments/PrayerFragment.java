package dp.wkp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dp.wkp.R;
import dp.wkp.activities.PrayerDayActivity;
import dp.wkp.adapters.PrayerCardsAdapter;
import dp.wkp.callbacks.PrayerCardEventListener;
import dp.wkp.db.prayercards.PrayerCard;
import dp.wkp.db.prayercards.PrayerCardDB;
import dp.wkp.db.prayercards.PrayerCardsDao;
import dp.wkp.utils.DaysOfWeek;

/**
 * A fragment to show 7 prayer cards (one for each day of the week)
 * It's not possible to manually add or remove prayer cards. It must have only 7 cards.
 * <p>
 * Long click is not yet supported, because I still didn't think about anything relevant to add.
 */
public class PrayerFragment extends Fragment implements PrayerCardEventListener {
    public static final String CARD_ID = "card_id";
    private RecyclerView recyclerView;
    private ArrayList<PrayerCard> cards;
    private PrayerCardsDao dao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_prayer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.prayer_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        dao = PrayerCardDB.getInstance(this.getContext()).prayerCardsDao();
    }

    @Override
    public void onResume() {
        loadCards();
        super.onResume();
    }

    /**
     * Gets all cards from DB and saves them in a ArrayList
     */
    private void loadCards() {
        this.cards = new ArrayList<>();
        List<PrayerCard> list = dao.getPrayerCards();
        this.cards.addAll(list);
        createCards();
        PrayerCardsAdapter adapter = new PrayerCardsAdapter(getContext(), cards);
        adapter.setListener(this);
        this.recyclerView.setAdapter(adapter);
    }

    @Override
    public void OnCardClick(PrayerCard card) {
        Intent cardActivity = new Intent(this.getActivity(), PrayerDayActivity.class);
        cardActivity.putExtra(CARD_ID, card.getId());
        startActivity(cardActivity);

    }

    /**
     * This method should be called after initializing ArrayList cards with all cards from DB.
     * If the list is still empty, then it will create 7 cards (one for each day) and stores on DB.
     */
    private void createCards() {
        if (cards.size() == 0) {
            for (DaysOfWeek day : DaysOfWeek.values()) {
                PrayerCard card = new PrayerCard(day);
                card.setColor(day.getColor(getContext()));
                long id = dao.insertPrayerCard(card);
                card.setId((int) id);
                cards.add(card);
            }
        }
    }
}