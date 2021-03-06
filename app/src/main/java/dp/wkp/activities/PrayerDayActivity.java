package dp.wkp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import dp.wkp.R;
import dp.wkp.db.prayercards.PrayerCard;
import dp.wkp.db.prayercards.PrayerCardDB;
import dp.wkp.db.prayercards.PrayerCardsDao;
import dp.wkp.fragments.PrayerFragment;
import dp.wkp.utils.ListEditText;
import dp.wkp.utils.MaterialColor;
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;

/**
 * Activity to view and edit a prayer day (subject and requests)
 * It receives an id and opens respective prayer day.
 */
public class PrayerDayActivity extends BaseActivity {

    private TextInputEditText subject;
    private ListEditText requests;
    private MaterialToolbar bottomToolbar;
    private CheckBox checkBox;
    private PrayerCardsDao dao;
    private PrayerCard card;
    private int color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_day);

        dao = PrayerCardDB.getInstance(this).prayerCardsDao();
        int id = getIntent().getExtras().getInt(PrayerFragment.CARD_ID, 0);
        card = dao.getCardById(id);

        MaterialToolbar toolbar = findViewById(R.id.edit_toolbar);
        toolbar.setTitle(card.getDay().getDescription(this));
        setSupportActionBar(toolbar);

        bottomToolbar = findViewById(R.id.end_toolbar);
        subject = findViewById(R.id.title);
        requests = findViewById(R.id.input_note);
        checkBox = findViewById(R.id.checkbox);

        color = card.getColor();
        changeColor(color);

        subject.setText(card.getSubtitle());
        requests.setText(card.getText());

        if (card.isPrayed())
            checkBox.setChecked(true);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            card.setPrayed(checkBox.isChecked());
            dao.updatePrayerCard(card);
        });
    }

    /**
     * Menu with options to change color, share and save
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_note_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Auto saves (if there is any change) when clicks back button.
     */
    @Override
    public void onBackPressed() {
        onSave();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.save_note) {
            onSave();
        } else if (id == R.id.action_share_note) {
            onShare();
        } else if (id == R.id.note_color) {
            onChangeColor();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Changes views color and color associated with the day
     * (however the color will be saved only when saving the prayer note)
     *
     * @param color
     */
    private void changeColor(int color) {
        this.color = color;
        subject.getBackground().setTint(color);
        requests.getBackground().setTint(color);
        bottomToolbar.getBackground().setTint(color);
    }

    /**
     * Open ColorPickerDialog to let the user pick a color.
     */
    private void onChangeColor() {
        new ColorPickerDialog()
                .withColor(color)
                .withTitle(getString(R.string.choose_color))
                .withPresets(MaterialColor.PREDEFINED_COLORS)
                .withCornerRadiusPx(50)
                .withListener((pickerView, color) -> changeColor(color))
                .show(getSupportFragmentManager(), "colorPicker");
    }

    /**
     * Opens a dialog to share the text returned by card.toStringShare(context)
     */
    private void onShare() {
        onSave();
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, card.toStringShare(this));
        startActivity(share);
    }

    /**
     * Saves the prayer card if there is any difference (subject, text or color) and updates on DB.
     */
    private void onSave() {
        String subtitle = subject.getText().toString();
        String text = requests.getText().toString();
        if (!(text.equals(card.getText()) && subject.getText().toString().equals(card.getSubtitle()) && color == card.getColor())) {
            card.setSubtitle(subtitle);
            card.setText(text);
            card.setColor(color);
            dao.updatePrayerCard(card);
        }
    }
}