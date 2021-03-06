package dp.wkp.callbacks;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import dp.wkp.R;

/**
 * Class to represent ActionMode when long click notes on DiaryFragment (menu on toolbar changes)
 */
public abstract class MainActionModeCallback implements ActionMode.Callback {
    private ActionMode action;
    private MenuItem countItem;
    private MenuItem shareItem;

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.main_action_mode, menu);
        this.action = actionMode;
        this.countItem = menu.findItem(R.id.action_checked_count);
        this.shareItem = menu.findItem(R.id.action_share_note);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {

    }

    public void setCount(String checkedCount) {
        if (countItem != null)
            this.countItem.setTitle(checkedCount);
    }

    /**
     * if checked count > 1 hide shareItem else show it
     *
     * @param b :visible
     */
    public void changeShareItemVisible(boolean b) {
        shareItem.setVisible(b);
    }

    public ActionMode getAction() {
        return action;
    }
}
