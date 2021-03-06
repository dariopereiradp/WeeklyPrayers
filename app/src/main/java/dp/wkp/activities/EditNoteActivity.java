package dp.wkp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Date;

import dp.wkp.R;
import dp.wkp.db.notes.Note;
import dp.wkp.db.notes.NotesDB;
import dp.wkp.db.notes.NotesDao;
import dp.wkp.utils.AESCryptography;
import dp.wkp.utils.MaterialColor;
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;

/**
 * This activity is responsible to edit (and create) a note.
 * If it receives a noteId, it opens the respective note. Else, creates a new one.
 */
public class EditNoteActivity extends BaseActivity {
    private TextInputEditText title, inputNote;
    private NotesDao dao;
    private Note temp;
    private MaterialToolbar bottomToolbar;
    private int color;
    public static final String NOTE_EXTRA_Key = "note_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        MaterialToolbar toolbar = findViewById(R.id.edit_toolbar);
        toolbar.setTitle(R.string.edit_note);
        setSupportActionBar(toolbar);

        bottomToolbar = findViewById(R.id.end_toolbar);
        title = findViewById(R.id.title);
        inputNote = findViewById(R.id.input_note);
        dao = NotesDB.getInstance(this).notesDao();
        if (getIntent().getExtras() != null) {
            int id = getIntent().getExtras().getInt(NOTE_EXTRA_Key, 0);
            temp = dao.getNoteById(id);
            changeColor(temp.getColor());
            title.setText(temp.getNoteTitle());
            inputNote.setText(temp.getNoteText());
            if (temp.getNum_enc() != 0) {
                inputNote.setEnabled(false);
            }
        } else inputNote.setFocusable(true);

        color = temp == null ? getColor(R.color.note_backg) : temp.getColor();

        ExtendedFloatingActionButton enc = findViewById(R.id.enc);
        enc.setOnClickListener(v -> encriptar());
        ExtendedFloatingActionButton desc = findViewById(R.id.desc);
        desc.setOnClickListener(v -> desencriptar());
    }

    /**
     * Creates a dialog to enter a password and then encrypts or decrypts the message
     *
     * @param message   - message that will be displayed on the dialog
     * @param erro      - error message that will be displayed on the dialog
     * @param encriptar - true - encriptar, false - desencriptar
     */
    private void pass(final String message, final String erro, final boolean encriptar) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.password))
                .setMessage(message)
                .setView(input)
                .setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
                    String value = (input.getText()).toString();
                    AESCryptography.setKey(value);
                    try {
                        String s = "";
                        if (encriptar) {
                            AESCryptography.encrypt(inputNote.getText().toString());
                            s = AESCryptography.getEncryptedString();
                            temp.incrementar_enc();
                            inputNote.setEnabled(false);
                            Toast.makeText(this, getString(R.string.sucEnc), Toast.LENGTH_SHORT).show();
                        } else {
                            AESCryptography.decrypt(inputNote.getText().toString());
                            s = AESCryptography.getDecryptedString();
                            temp.decrementar_enc();
                            if (temp.getNum_enc() == 0) {
                                inputNote.setEnabled(true);
                            }
                            Toast.makeText(this, getString(R.string.sucDesc), Toast.LENGTH_SHORT).show();
                        }
                        if (!s.equals("")) {
                            inputNote.setText(s);
                            onSaveNote();
                        }

                    } catch (GeneralSecurityException | NullPointerException | UnsupportedEncodingException e) {
                        Toast.makeText(this, "" + erro, Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    } catch (IllegalArgumentException e1) {
                        Toast.makeText(this, "" + e1.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, whichButton) -> {
                })
                .show();
    }

    /**
     * Saves the note first (to prevent NullPointerException) and then opens the dialog the enter
     * password to encrypt
     */
    private void encriptar() {
        onSaveNote();
        if (temp == null || inputNote.getText().toString().trim().equals("")) {
            Toast.makeText(this, getString(R.string.empty_note), Toast.LENGTH_SHORT).show();
        } else {
            String message = getString(R.string.msgEnc);
            String erro = getString(R.string.errEnc);
            pass(message, erro, true);
        }
    }

    /**
     * Saves the note first (to prevent NullPointerException) and then opens the dialog the enter
     * password to decrypt.
     * Version change: it won't check anymore if num_enc == 0, because the user may enter an
     * encrypted note via copy&paste (for example, an encrypted note that was shared to another app)
     * and wants to decrypt it. Since num_enc is a variable stored in DB, this will only work with
     * notes created by the app.
     */
    private void desencriptar() {
        onSaveNote();
        if (temp != null) {
            String message = getString(R.string.msgDesc);
            String erro = getString(R.string.errDesc);
            pass(message, erro, false);
        } else
            Toast.makeText(this, getString(R.string.empty_note), Toast.LENGTH_SHORT).show();
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
     * Auto saves the note (if is not empty) when clicks back button.
     */
    @Override
    public void onBackPressed() {
        onSaveNote();
        finish(); // return to the MainActivity
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.save_note) {
            onSaveNote();
        } else if (id == R.id.action_share_note) {
            onShareNote();
        } else if (id == R.id.note_color) {
            onChangeColor();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Changes views color and color associated with the note
     * (however the color will be saved only when saving the note)
     *
     * @param color
     */
    private void changeColor(int color) {
        this.color = color;
        title.getBackground().setTint(color);
        inputNote.getBackground().setTint(color);
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
     * Opens a dialog to share the text returned by note.toStringShare(context)
     */
    private void onShareNote() {
        onSaveNote();
        if (temp != null) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, temp.toStringShare(this));
            startActivity(share);
        } else {
            Toast.makeText(this, getString(R.string.empty_note), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Try to save the note, if it is not empty.
     * If it's a new note, inserts on DB.
     * Else, update on DB (checking if there is a difference between the original and the current).
     * A difference can be on the title, text or color. If there's no difference, it won't save.
     */
    private void onSaveNote() {
        String titleText = title.getText().toString();
        String text = inputNote.getText().toString();
        if (!text.isEmpty() || !titleText.isEmpty()) {
            long date = new Date().getTime();
            if (temp == null) {
                temp = new Note(titleText, text, date, 0);
                temp.setColor(color);
                long id = dao.insertNote(temp);
                temp.setId((int) id);
                Toast.makeText(this, getString(R.string.notaSalva), Toast.LENGTH_SHORT).show();
            } else if (!(text.equals(temp.getNoteText()) && titleText.equals(temp.getNoteTitle()) && temp.getColor() == color)) {
                temp.setNoteTitle(titleText);
                temp.setNoteText(text);
                temp.setNoteDate(date);
                temp.setColor(color);
                dao.updateNote(temp);
                Toast.makeText(this, getString(R.string.notaSalva), Toast.LENGTH_SHORT).show();
            }
        } else
            Toast.makeText(this, getString(R.string.empty_note), Toast.LENGTH_SHORT).show();
    }
}
