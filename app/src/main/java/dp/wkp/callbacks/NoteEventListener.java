package dp.wkp.callbacks;

import dp.wkp.db.notes.Note;

public interface NoteEventListener {
    /**
     * call when note clicked.
     *
     * @param note: note item
     */
    void onNoteClick(Note note);

    /**
     * call when long click to note.
     *
     * @param note : item
     */
    void onNoteLongClick(Note note);
}
