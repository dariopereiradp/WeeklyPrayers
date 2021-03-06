package dp.wkp.utils;

import android.content.Context;

import androidx.core.content.ContextCompat;

import dp.wkp.R;

/**
 * Enum storing all 7 days of the week with localized string name and default card color.
 */
public enum DaysOfWeek {
    SUNDAY(R.string.sunday, R.color.sunday),
    MONDAY(R.string.monday, R.color.monday),
    TUESDAY(R.string.tuesday, R.color.tuesday),
    WEDNESDAY(R.string.wednesday, R.color.wednesday),
    THURSDAY(R.string.thursday, R.color.thursday),
    FRIDAY(R.string.friday, R.color.friday),
    SATURDAY(R.string.saturday, R.color.saturday);

    private final int resourceId;
    private final int colorId;

    DaysOfWeek(int resourceId, int colorId) {
        this.resourceId = resourceId;
        this.colorId = colorId;
    }


    public String getDescription(Context context) {
        return context.getResources().getString(resourceId);
    }

    public int getColor(Context context) {
        return ContextCompat.getColor(context, colorId);
    }
}
