package dp.wkp.utils;

import android.content.Context;
import android.util.AttributeSet;

/**
 * TextInput that automatically adds a bullet point every time the user clicks "Enter", to simulate
 * a bullet point list.
 */
public class ListEditText extends com.google.android.material.textfield.TextInputEditText {
    private static final String ASTERISK = "⬤";

    public ListEditText(Context context) {
        this(context, null);
    }

    public ListEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        getPaint();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (lengthAfter > lengthBefore) {
            if (text.toString().length() == 1) {
                text = ASTERISK + " " + text;
                setText(text);
                setSelection(getText().length());
            }
            if (text.toString().endsWith("\n")) {
                text = text.toString().replace("\n", "\n" + ASTERISK + " ");
                text = text.toString().replace(ASTERISK + " " + ASTERISK, ASTERISK);
                setText(text);
                setSelection(getText().length());
            }
        }
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

}
