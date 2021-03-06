package dp.wkp.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import dp.wkp.R;
import dp.wkp.activities.BaseActivity;
import dp.wkp.callbacks.PrayerCardEventListener;
import dp.wkp.db.prayercards.PrayerCard;
import dp.wkp.db.prayercards.PrayerCardDB;

/**
 * Class to view prayer cards on RecyclerView
 * Here and with layout xml file I can change the design the way I want.
 */
public class PrayerCardsAdapter extends RecyclerView.Adapter<PrayerCardsAdapter.PrayerCardHolder> {
    private final Context context;
    private final ArrayList<PrayerCard> cards;
    private PrayerCardEventListener listener;

    public PrayerCardsAdapter(Context context, ArrayList<PrayerCard> cards) {
        this.context = context;
        this.cards = cards;
    }

    @NonNull
    @Override
    public PrayerCardsAdapter.PrayerCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.prayer_card_layout, parent, false);
        return new PrayerCardHolder(v);
    }

    /**
     * Method that initializes prayer cards attributes (subject, text, notification icon, etc)
     *
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull final PrayerCardsAdapter.PrayerCardHolder holder, int position) {
        final PrayerCard prayerCard = getPrayerCard(position);
        if (prayerCard != null) {
            holder.getLayout().getBackground().setTint(prayerCard.getColor());
            holder.getNoteTitle().setText(prayerCard.getDay().getDescription(context));
            holder.getNoteSubtitle().setText(prayerCard.getSubtitle());
            holder.getNoteText().setText(prayerCard.getText());
            if (prayerCard.isPrayed())
                holder.getDone().setVisibility(View.VISIBLE);

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean notifications = sharedPrefs.getBoolean(BaseActivity.NOTIFICATION, false);
            if (!notifications)
                holder.getNotification().setVisibility(View.GONE);
            else if (prayerCard.hasNotification())
                holder.getNotification().setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_notifications_active_24px));

            // init prayerCard click event
            holder.itemView.setOnClickListener(view -> listener.OnCardClick(prayerCard));

            holder.getNotification().setOnClickListener(v -> {
                if (prayerCard.hasNotification()) {
                    Toast.makeText(context, context.getString(R.string.notification_off), Toast.LENGTH_SHORT).show();
                    prayerCard.setNotification(false);
                    holder.getNotification().setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_notifications_off_24dp));
                } else {
                    Toast.makeText(context, context.getString(R.string.notification_on), Toast.LENGTH_SHORT).show();
                    prayerCard.setNotification(true);
                    holder.getNotification().setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_notifications_active_24px));
                }
                PrayerCardDB.getInstance(context).prayerCardsDao().updatePrayerCard(prayerCard);
            });
        }
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    private PrayerCard getPrayerCard(int position) {
        return cards.get(position);
    }

    public void setListener(PrayerCardEventListener listener) {
        this.listener = listener;
    }

    /**
     * Class that represents a single card graphically. Here and with layout xml file I can change
     * the design the way I want.
     */
    public static class PrayerCardHolder extends RecyclerView.ViewHolder {
        private final LinearLayout layout;
        private final TextView noteTitle;
        private final TextView noteSubtitle;
        private final TextView noteText;
        private final ImageView done;
        private final ImageView notification;

        public PrayerCardHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.layout);
            noteTitle = itemView.findViewById(R.id.note_title);
            noteSubtitle = itemView.findViewById(R.id.note_subtitle);
            noteText = itemView.findViewById(R.id.note_text);
            done = itemView.findViewById(R.id.done);
            notification = itemView.findViewById(R.id.notification_note);
        }

        public LinearLayout getLayout() {
            return layout;
        }

        public TextView getNoteTitle() {
            return noteTitle;
        }

        public TextView getNoteSubtitle() {
            return noteSubtitle;
        }

        public TextView getNoteText() {
            return noteText;
        }

        public ImageView getDone() {
            return done;
        }

        public ImageView getNotification() {
            return notification;
        }
    }
}
