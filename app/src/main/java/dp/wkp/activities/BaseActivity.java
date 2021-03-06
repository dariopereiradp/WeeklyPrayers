package dp.wkp.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.util.Locale;

/**
 * Activity that stores some important constants. However, the main functionality is to change the language and the app theme.
 * All other activities must extend BaseActivity.
 * It overrides attachBaseContext(Context newBase) method to ensure the same language and theme in all activities from the app.
 */
public class BaseActivity extends AppCompatActivity {

    public static final String LANGUAGE = "language";
    public static final String NOTIFICATION = "notifications";
    public static final String TIME = "timePreference";
    public static final String THEME = "theme";
    public static final String PROFILE_IMAGE = "image";
    public static final String PROFILE_NAME = "name";
    public static final String PROFILE_MAIL = "email";
    public static final String PROFILE_ID = "personId";
    public static final String NOTIFICATION_TYPE = "notification_type";
    public static final String LOAD_ALARMS = "loadAlarms";
    public static final String LAST_BACKUP_TIME = "lastBackupTime";

    /**
     * Changes the app theme
     *
     * @param theme
     */
    public static void setTheme(String theme) {
        switch (theme) {
            case "default":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }

    /**
     * Changes the language of the app to a new language given on localeString param.
     * In fact, it only changes the locale. The language will change if there is a language
     * to this locale.
     * It is used when the user chooses a specific language.
     *
     * @param context
     * @param localeString
     * @param conf
     * @return
     */
    public Context setAppLocale(Context context, String localeString, Configuration conf) {
        Locale locale = new Locale(localeString.toLowerCase());
        Locale.setDefault(locale);
        conf.setLocale(locale);
        context = context.createConfigurationContext(conf);
        return context;
    }

    /**
     * During activity creation, initialize language and theme that were configured by the user.
     *
     * @param newBase
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(newBase);
        String lang = settings.getString(LANGUAGE, "default");
        Resources res = newBase.getResources();
        res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        if (lang.equals("default"))
            super.attachBaseContext(setAppLocale(newBase, conf));
        else
            super.attachBaseContext(setAppLocale(newBase, lang, conf));
        String theme = settings.getString(THEME, "default");
        setTheme(theme);
    }

    /**
     * Changes app language to the system default language. If the default language is not portuguese, the app will
     * change to the app default language, that is English.
     *
     * @param context
     * @param conf
     * @return
     */
    public Context setAppLocale(Context context, Configuration conf) {
        String lang = Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();
        if ("pt".equals(lang)) {
            return setAppLocale(context, lang, conf);
        } else {
            return setAppLocale(context, "en", conf);
        }
    }
}
