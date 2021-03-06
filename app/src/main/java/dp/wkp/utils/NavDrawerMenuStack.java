package dp.wkp.utils;

import java.util.Stack;

/**
 * Used to keep the fragment stack even if the activity is recreated.<br>
 * This class is useful to keep a consistent behaviour of the back button.
 */
public class NavDrawerMenuStack {

    private static NavDrawerMenuStack INSTANCE;
    private final Stack<Integer> menuIndexesClicked = new Stack<>();

    public static NavDrawerMenuStack getINSTANCE() {
        if (INSTANCE == null)
            INSTANCE = new NavDrawerMenuStack();
        return INSTANCE;
    }

    public Stack<Integer> getMenuIndexesClicked() {
        return menuIndexesClicked;
    }
}
