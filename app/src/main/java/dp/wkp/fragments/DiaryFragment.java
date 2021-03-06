package dp.wkp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import dp.wkp.R;
import dp.wkp.activities.EditNoteActivity;
import dp.wkp.adapters.NotesAdapter;
import dp.wkp.callbacks.MainActionModeCallback;
import dp.wkp.callbacks.NoteEventListener;
import dp.wkp.db.notes.Note;
import dp.wkp.db.notes.NotesDB;
import dp.wkp.db.notes.NotesDao;

import static dp.wkp.activities.EditNoteActivity.NOTE_EXTRA_Key;

/**
 * A fragment to show all notes stored on DB and to handle click, long click and swipe to delete.
 * It also implements a search view
 */
public class DiaryFragment extends Fragment implements NoteEventListener {

    public static final String DIARY_FRAGMENT = "diary_fragment";
    private RecyclerView recyclerView;
    private ArrayList<Note> notes;
    private NotesAdapter adapter;
    private NotesDao dao;
    private MainActionModeCallback actionModeCallback;
    private int checkedCount = 0;
    private FloatingActionButton fab;
    private SearchView searchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.notes_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(view1 -> onAddNewNote());

        dao = NotesDB.getInstance(this.getContext()).notesDao();
    }

    /**
     * When there are no notes, show a message
     */
    private void showEmptyView() {
        if (notes.size() == 0) {
            this.recyclerView.setVisibility(View.GONE);
            this.getActivity().findViewById(R.id.empty_notes_view).setVisibility(View.VISIBLE);
        } else {
            this.recyclerView.setVisibility(View.VISIBLE);
            this.getActivity().findViewById(R.id.empty_notes_view).setVisibility(View.GONE);
        }
    }

    private void onAddNewNote() {
        startActivity(new Intent(this.getActivity(), EditNoteActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotes();
    }

    /**
     * Closes search view if it is open and returns true if it was open or false if not.
     *
     * @return
     */
    public boolean closeSearchView() {
        if (!searchView.isIconified()) {
            searchView.onActionViewCollapsed();
            return true;
        }
        return false;
    }

    @Override
    public void onNoteClick(Note note) {
        Intent edit = new Intent(this.getActivity(), EditNoteActivity.class);
        edit.putExtra(NOTE_EXTRA_Key, note.getId());
        closeSearchView();
        startActivity(edit);
    }

    /**
     * Loads all notes from DB do ArrayList
     */
    private void loadNotes() {
        this.notes = new ArrayList<>();
        List<Note> list = dao.getNotes();
        this.notes.addAll(list);
        this.adapter = new NotesAdapter(this.getContext());
        this.adapter.setListener(this);
        this.recyclerView.setAdapter(adapter);
        showEmptyView();
        swipeToDeleteHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * Long click checks note and changes view to show menus
     *
     * @param note : note
     */
    @Override
    public void onNoteLongClick(Note note) {
        note.setChecked(true);
        checkedCount = 1;
        adapter.setMultiCheckMode(true);

        adapter.setListener(new NoteEventListener() {
            @Override
            public void onNoteClick(Note note) {
                note.setChecked(!note.isChecked());
                if (note.isChecked())
                    checkedCount++;
                else checkedCount--;

                actionModeCallback.changeShareItemVisible(checkedCount <= 1);

                if (checkedCount == 0) {
                    //  finish multi select mode wen checked count = 0
                    actionModeCallback.getAction().finish();
                    onActionModeFinished();
                }

                actionModeCallback.setCount(checkedCount + "/" + notes.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNoteLongClick(Note note) {

            }
        });

        actionModeCallback = new MainActionModeCallback() {
            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_delete_notes)
                    onDeleteMultiNotes();
                else if (menuItem.getItemId() == R.id.action_share_note)
                    onShareNote();

                actionMode.finish();
                onActionModeFinished();
                return false;
            }

        };
        // start action mode
        this.getActivity().startActionMode(actionModeCallback);
        // hide fab button
        fab.setVisibility(View.GONE);
        actionModeCallback.setCount(checkedCount + "/" + notes.size());
    }

    public void onActionModeFinished() {
        adapter.setMultiCheckMode(false); // uncheck the notes
        adapter.setListener(this); // set back the old listener
        fab.setVisibility(View.VISIBLE);
    }

    /**
     * Search view
     *
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void onShareNote() {
        Note note = adapter.getCheckedNotes().get(0);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, note.toStringShare(getContext()));
        startActivity(share);
    }

    /**
     * Asks for confirmation and delete all checked notes. If note has notification, delete work.
     */
    private void onDeleteMultiNotes() {
        List<Note> checkedNotes = adapter.getCheckedNotes();
        if (checkedNotes.size() != 0) {
            new AlertDialog.Builder(DiaryFragment.this.getContext())
                    .setMessage(getString(R.string.delete) + "?")
                    .setPositiveButton(getString(R.string.delete), (dialogInterface, i) -> {
                        for (Note note : checkedNotes) {
                            if (note.hasNotification()) {
                                WorkManager.getInstance(getContext()).cancelUniqueWork(String.valueOf(note.getId()));
                            }
                            dao.deleteNote(note);
                        }
                        loadNotes();
                        Toast.makeText(this.getContext(), checkedNotes.size() + " " + getString(R.string.deleteSuccess), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    })
                    .setCancelable(false)
                    .show();
        } else
            Toast.makeText(this.getContext(), getString(R.string.no_selected_notes), Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles swipe left to delete
     */
    private final ItemTouchHelper swipeToDeleteHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    if (notes != null) {
                        Note swipedNote = adapter.getNote(viewHolder.getAdapterPosition());
                        if (swipedNote != null) {
                            swipeToDelete(swipedNote, viewHolder);
                        }
                    }
                }
            });

    /**
     * Asks for confirmation and delete. If note has notification, delete notification worker.
     */
    private void swipeToDelete(final Note swipedNote, final RecyclerView.ViewHolder viewHolder) {
        new AlertDialog.Builder(DiaryFragment.this.getContext())
                .setMessage(getString(R.string.delete) + "?")
                .setPositiveButton(getString(R.string.delete), (dialogInterface, i) -> {
                    if (swipedNote.hasNotification()) {
                        WorkManager.getInstance(getContext()).cancelUniqueWork(String.valueOf(swipedNote.getId()));
                    }
                    dao.deleteNote(swipedNote);
                    notes.remove(swipedNote);
                    loadNotes();
                    Toast.makeText(DiaryFragment.this.getContext(), getString(R.string.deleteSuccess1), Toast.LENGTH_SHORT).show();

                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> recyclerView.getAdapter().notifyItemChanged(viewHolder.getAdapterPosition()))
                .setCancelable(false)
                .show();
    }
}
