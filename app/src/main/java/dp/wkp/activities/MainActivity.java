package dp.wkp.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import dp.wkp.R;
import dp.wkp.backup.DriveServiceHelper;
import dp.wkp.backup.GoogleSignInFragment;
import dp.wkp.db.notes.Note;
import dp.wkp.db.notes.NotesDB;
import dp.wkp.db.prayercards.PrayerCard;
import dp.wkp.db.prayercards.PrayerCardDB;
import dp.wkp.db.prayercards.PrayerCardsDao;
import dp.wkp.fragments.AboutFragment;
import dp.wkp.fragments.DiaryFragment;
import dp.wkp.fragments.PrayerFragment;
import dp.wkp.fragments.SettingsFragment;
import dp.wkp.notifications.DiaryNoteReminderWorker;
import dp.wkp.notifications.NotificationPrayerReminder;
import dp.wkp.notifications.PrayerReminderWorker;
import dp.wkp.utils.ImageUtils;
import dp.wkp.utils.NavDrawerMenuStack;

/**
 * Activity that contains all fragments, creates notification channels, handle results, reload alarms
 * when restore occurs, etc... Also: controls the backStack, navigation drawer clicks and profile
 * picture.
 */
public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String PROFILE_PICTURE_NAME_YOU = "you.jpg";
    private static final String HAS_PROFILE_PICTURE_YOU = "profile_you";
    public static final String HAS_LOADED_URL = "url_ok";
    public static final String FRAGMENT_TO_LOAD = "fragment_to_load";
    private static final String LAST_DAY_RESET = "reset_date";

    private SharedPreferences settings;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private ImageUtils imageUtils;
    private Stack<Integer> menuIndexesClicked;
    private static MainActivity INSTANCE;

    public static MainActivity getInstance() {
        if (INSTANCE == null)
            INSTANCE = new MainActivity();
        return INSTANCE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        INSTANCE = this;
        super.onCreate(savedInstanceState);

        menuIndexesClicked = NavDrawerMenuStack.getINSTANCE().getMenuIndexesClicked();
        createNotificationChannel();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_base);
        navigationView = findViewById(R.id.navigationview_id);
        navigationView.setNavigationItemSelectedListener(this);
        initializeToolbar();
        toggleDrawer();
        setInitialFragment(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        imageUtils = new ImageUtils(this, PROFILE_PICTURE_NAME_YOU, HAS_PROFILE_PICTURE_YOU);
        setProfileImage();
        TextView name_view = navigationView.getHeaderView(0).findViewById(R.id.nav_header_name_id);
        TextView mail_view = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email_id);
        String name = settings.getString(PROFILE_NAME, getResources().getString(R.string.your_name));
        String mail = settings.getString(PROFILE_MAIL, getResources().getString(R.string.your_mail));
        name_view.setText(name);
        mail_view.setText(mail);

        checkAndResetPrayerDays();

        boolean loadAlarms = settings.getBoolean(BaseActivity.LOAD_ALARMS, false);
        if (loadAlarms) {
            reloadAlarms();
        }
    }

    /**
     * Initialize toolbar
     */
    private void initializeToolbar() {
        toolbar = findViewById(R.id.toolbar_id);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout_id);
    }

    /**
     * This method checks if a new week began. If the user opens the app and the last prayer reset
     * was last week, then it will do a prayer reset and update the last day reset value on the
     * SharedPreferences DB.
     * A prayer reset is iterating through the 7 prayer cards and marking each one with "not prayed" state.
     * This is useful to ensure that the notification system is weekly based, because if a card is marked
     * as "prayed", the notification for that day won't appear, even if it is turned on for that day.
     * The problem here is that if the user doesn't open the app and only clicks on the "Prayed" button
     * at the notification, next week notifications won't show app, because the app will not reset the values.
     * Once the user opens the app, it will reset, however.
     * To prevent this problem, a future update may change this behaviour adding it to a service (worker)
     * that runs every sunday (using WorkManager API)
     */
    private void checkAndResetPrayerDays() {
        long lastResetDay = settings.getLong(LAST_DAY_RESET, 0);
        LocalDate date = Instant.ofEpochMilli(lastResetDay).atZone(ZoneId.systemDefault()).toLocalDate();
        if (!inSameCalendarWeek(date)) {
            resetPrayerDays();
            settings.edit()
                    .putLong(LAST_DAY_RESET, LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    .apply();
        }
    }

    /**
     * @See #checkAndResetPrayerDays()
     */
    public void resetPrayerDays() {
        PrayerCardsDao dao = PrayerCardDB.getInstance(getApplicationContext()).prayerCardsDao();
        for (PrayerCard card : dao.getPrayerCards()) {
            card.setPrayed(false);
            dao.updatePrayerCard(card);
        }
    }

    /**
     * Checks if firtsDate is in the same week of today.
     *
     * @param firstDate
     * @return
     */
    public boolean inSameCalendarWeek(LocalDate firstDate) {
        LocalDate secondDate = LocalDate.now(ZoneId.systemDefault());
        // get a reference to the system of calendar weeks in US Locale (to guarantee that week starts on Sunday)
        WeekFields weekFields = WeekFields.of(Locale.US);
        // find out the calendar week for each of the dates
        int firstDatesCalendarWeek = firstDate.get(weekFields.weekOfWeekBasedYear());
        int secondDatesCalendarWeek = secondDate.get(weekFields.weekOfWeekBasedYear());
        /*
         * find out the week based year, too,
         * two dates might be both in a calendar week number 1 for example,
         * but in different years
         */
        int firstWeekBasedYear = firstDate.get(weekFields.weekBasedYear());
        int secondWeekBasedYear = secondDate.get(weekFields.weekBasedYear());
        // return if they are equal or not
        return firstDatesCalendarWeek == secondDatesCalendarWeek
                && firstWeekBasedYear == secondWeekBasedYear;
    }

    /**
     * If profile image doesn't exist, it will check if there is a valid url and if this url has not
     * been loaded. If this is true, then it will load the image from the url.
     * HAS_LOADED_URL will allow the user to delete the image. If this condition doesn't exists,
     * when the user deletes it's picture, the app will automatically load again from the url.
     */
    private void setProfileImage() {
        ImageView profileImage = navigationView.getHeaderView(0).findViewById(R.id.profile_picture);

        boolean profileImageIsSet = settings.getBoolean(HAS_PROFILE_PICTURE_YOU, false);
        boolean hasLoadedUrl = settings.getBoolean(HAS_LOADED_URL, false);

        if (!imageUtils.existsProfilePicture()) {
            String profile_url = settings.getString(PROFILE_IMAGE, "none");
            if (!profile_url.equals("none") && !hasLoadedUrl) {
                imageUtils.uCrop(Uri.parse(profile_url), Uri.fromFile(new File(imageUtils.getProfilePicturePath())));
                settings.edit().putBoolean(HAS_LOADED_URL, true).apply();
            }
        }
        if (imageUtils.existsProfilePicture()) {
            if (!profileImageIsSet) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(HAS_PROFILE_PICTURE_YOU, true);
                editor.apply();
            }
        }
        try {
            // this will check if there is an aux file and if true, then it will undo the change
            // I don't remember the reason for that, but I think it is just in case something goes
            //wrong ant the app stops, it will restore the old image.
            imageUtils.undoChangePicture(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!profileImageIsSet) {
            imageUtils.deleteAuxPicture();
            imageUtils.deleteProfilePicture();
        }
        profileImage.setOnClickListener(v -> imageUtils.openGallery());
        profileImage.setOnLongClickListener(v -> {
            imageUtils.delete();
            return true;
        });
        imageUtils.checkAndSetProfilePicture(profileImage);
    }

    public ImageUtils getImageUtils() {
        return imageUtils;
    }

    /**
     * If this method doesn't exists, the nav drawer icon won't be visible.
     * Creates an instance of the ActionBarDrawerToggle class:
     * 1) Handles opening and closing the navigation drawer
     * 2) Creates a hamburger icon in the toolbar
     * 3) Attaches listener to open/close drawer on icon clicked and rotates the icon
     */
    private void toggleDrawer() {
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    /**
     * Checks if the savedInstanceState is null - onCreate() is ran
     * If so, display fragment of navigation drawer menu at the initial position and
     * set checked status as true or set the position to the fragment that has been loaded.
     *
     * @param savedInstanceState savedInstanceState
     */
    private void setInitialFragment(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (savedInstanceState == null && !intent.hasExtra(FRAGMENT_TO_LOAD)) {
            navigationView.getMenu().getItem(0).setChecked(true);
            menuIndexesClicked.push(0);
            toolbar.setTitle(R.string.prayer_requests);
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_id, new PrayerFragment())
                    .commit();
        } else if (savedInstanceState == null && intent.hasExtra(FRAGMENT_TO_LOAD)) {
            String fragment = intent.getExtras().getString(FRAGMENT_TO_LOAD);
            if (DiaryFragment.DIARY_FRAGMENT.equals(fragment)) {
                navigationView.getMenu().getItem(1).setChecked(true);
                menuIndexesClicked.push(1);
                toolbar.setTitle(R.string.diary);
                getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_id, new DiaryFragment(), DiaryFragment.DIARY_FRAGMENT)
                        .commit();
            }
        }
    }

    /**
     * This method handles different things.
     * 1. If drawer is open, it will close drawer.
     * 2. If SearchView is open, it will close search view.
     * 3. Else, it will change the fragment to the fragment before or close the app if there is none
     * fragment opened before.
     * 4. It will change the name of the toolbar accordingly to the current fragment.
     * 5. Call a method to uncheck navigation icons
     *
     * @see #uncheckNavigationItems
     * <p>
     * FrameLayout.removeAllViews() - I needed to include that because in some cases the views of
     * different fragments were overlapping causing a weird effect.
     * <p>
     * To have a consistent behaviour of the backStack, I needed to crate an external stack to store
     * all the previous fragment indexes. Because when the activity was recreated (for example, screen
     * rotation) I was loosing the all the stack and the back button didn't work as expected anymore.
     */
    @Override
    public void onBackPressed() {
        //Checks if the navigation drawer is open -- If so, close it
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        // If drawer is already close -- Do not override original functionality
        else if (!closeSearchView()) {
            FrameLayout frameLayout = findViewById(R.id.framelayout_id);
            frameLayout.removeAllViews();
            super.onBackPressed();
            if (menuIndexesClicked.size() > 1) {
                menuIndexesClicked.pop();
                int index = menuIndexesClicked.peek();
                switch (index) {
                    case 0:
                        toolbar.setTitle(R.string.prayer_requests);
                        break;
                    case 1:
                        toolbar.setTitle(R.string.diary);
                        break;
                    case 2:
                        toolbar.setTitle(R.string.settings);
                        break;
                    case 3:
                        toolbar.setTitle(R.string.google_login);
                        break;
                    case 4:
                        toolbar.setTitle(R.string.about_fragment);
                        break;
                }
                uncheckNavigationItems(index);
            }
        }
    }

    /**
     * This method checks if search view was open or not. If it's open, back button needs to close it.
     * If it's not open, it will return false and back button will do normal behaviour.
     *
     * @return true if search view was closed or false if not
     */
    private boolean closeSearchView() {
        boolean close = false;
        if (menuIndexesClicked.peek() == 1) {
            DiaryFragment notesFragment = (DiaryFragment) getSupportFragmentManager().findFragmentByTag(DiaryFragment.DIARY_FRAGMENT);
            if (notesFragment != null) {
                close = notesFragment.closeSearchView();
            }
        }
        return close;
    }

    /**
     * This method will check the right position of the navigation menu and uncheck all the other positions.
     * The uncheck was really problematic. It will need two for loops to uncheck the first and the
     * second part of the menu (because Android consider that the second menu is a item (submenu) of
     * the first menu. It is very confuse). Also, this is necessary instead of the automatic uncheck
     * because I use two different sections (because of the categories) in the menu, but the behaviour
     * must be consistent, that is, if a check an item of the second section, I need to uncheck the
     * previous one, even if it is in the first section. The automatic behaviour didn't work as
     * expected, so I needed to create this.
     *
     * @param index - index that will be checked
     */
    private void uncheckNavigationItems(int index) {
        int size = navigationView.getMenu().size();
        for (int i = 0; i < size; i++) {
            navigationView.getMenu().getItem(i).setChecked(false);
        }
        int sizeSub = navigationView.getMenu().getItem(size - 1).getSubMenu().size();
        for (int i = 0; i < sizeSub; i++) {
            navigationView.getMenu().getItem(size - 1).getSubMenu().getItem(i).setChecked(false);
        }
        if (index < size - 1)
            navigationView.getMenu().getItem(index).setChecked(true);
        else {
            navigationView.getMenu().getItem(size - 1).getSubMenu().getItem(index - size + 1).setChecked(true);
        }
    }

    /**
     * 1. Checks if it can change to next fragment (that is, if backStack is empty or if the current
     * fragment is not the same that was clicked)
     * 2. If the condition is true, changes the fragment, add it do backStack and manually adds to
     * the stack of indexes. Also, changes the toolbar title and call a method to uncheck navigation icons.
     * 3. Closes navigation drawer.
     *
     * @param menuItem
     * @return
     * @see #uncheckNavigationItems
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_prayer_id) {
            if (menuIndexesClicked.isEmpty() || menuIndexesClicked.peek() != 0) {
                getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_id, new PrayerFragment()).addToBackStack(null)
                        .commit();
                menuIndexesClicked.push(0);
                uncheckNavigationItems(0);
                toolbar.setTitle(R.string.prayer_requests);
            }
            closeDrawer();
        } else if (itemId == R.id.nav_diary_id) {
            if (menuIndexesClicked.isEmpty() || menuIndexesClicked.peek() != 1) {
                getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_id, new DiaryFragment(), DiaryFragment.DIARY_FRAGMENT).addToBackStack(null)
                        .commit();
                menuIndexesClicked.push(1);
                uncheckNavigationItems(1);
                toolbar.setTitle(R.string.diary);
            }
            closeDrawer();
        } else if (itemId == R.id.nav_settings_id) {
            if (menuIndexesClicked.isEmpty() || menuIndexesClicked.peek() != 2) {
                getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_id, new SettingsFragment()).addToBackStack(null)
                        .commit();
                menuIndexesClicked.push(2);
                uncheckNavigationItems(2);
                toolbar.setTitle(R.string.settings);
            }
            closeDrawer();
        } else if (itemId == R.id.nav_google_id) {
            if (menuIndexesClicked.isEmpty() || menuIndexesClicked.peek() != 3) {
                getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_id, new GoogleSignInFragment(), "google").addToBackStack(null)
                        .commit();
                menuIndexesClicked.push(3);
                uncheckNavigationItems(3);
                toolbar.setTitle(R.string.google_login);
            }
            closeDrawer();
        } else if (itemId == R.id.nav_about_id) {
            if (menuIndexesClicked.isEmpty() || menuIndexesClicked.peek() != 4) {
                getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_id, new AboutFragment()).addToBackStack(null)
                        .commit();
                menuIndexesClicked.push(4);
                uncheckNavigationItems(4);
                toolbar.setTitle(R.string.about_fragment);
            }
            closeDrawer();
        }
        return true;
    }

    /**
     * Checks if the navigation drawer is open - if so, close it
     */
    private void closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /**
     * This is used in case the user is trying to do a backup or restore and needs to give the
     * required permission first.
     * If the request is not related to backup and restore, that means is a image related request.
     * The method will call onActivityResult from ImageUtils to handle it.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DriveServiceHelper.PERMISSION_REQUIRED_FOR_BACKUP && resultCode == -1) {
            GoogleSignInFragment google = (GoogleSignInFragment) getSupportFragmentManager().findFragmentByTag("google");
            if (google != null)
                google.doDirectBackup();
        } else if (requestCode == DriveServiceHelper.PERMISSION_REQUIRED_FOR_RESTORE && resultCode == -1) {
            GoogleSignInFragment google = (GoogleSignInFragment) getSupportFragmentManager().findFragmentByTag("google");
            if (google != null)
                google.doDirectRestore();
        } else {
            ImageView profileImage = navigationView.getHeaderView(0).findViewById(R.id.profile_picture);
            imageUtils.onActivityResult(requestCode, resultCode, data, profileImage, null);
        }
    }

    /**
     * This method will set all the alarms again. This is useful when restoring a backup.
     * 1. Reload Prayer reminder if is set on the SharedFragmentsDB
     * 2. Iterate through all notes and if note has notification, creates a new alarm for that note.
     * 3. Set LOAD_ALARMS to false, indicating that the restore is finished.
     *
     */
    private void reloadAlarms() {
        boolean notifications = settings.getBoolean(BaseActivity.NOTIFICATION, false);
        if (notifications) {
            int time = settings.getInt(BaseActivity.TIME, 0);
            new NotificationPrayerReminder(getApplicationContext()).updateAlarm(time, false);
        }

        for (Note note : NotesDB.getInstance(this).notesDao().getNotes()) {
            if (note.hasNotification()) {
                Data data = new Data.Builder().putInt("id", note.getId()).build();
                OneTimeWorkRequest notificationRequest = new OneTimeWorkRequest.Builder(DiaryNoteReminderWorker.class)
                        .setInitialDelay(note.getNoteDateTimeReminder() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .setInputData(data).build();
                WorkManager.getInstance(this).enqueueUniqueWork(String.valueOf(note.getId()), ExistingWorkPolicy.REPLACE, notificationRequest);
            }
        }

        settings.edit().putBoolean(BaseActivity.LOAD_ALARMS, false).apply();
        Toast.makeText(this, getString(R.string.restore_completed), Toast.LENGTH_SHORT).show();
    }

    /**
     * Since API26, it needs to create different notification channels for each type of notification.
     */
    private void createNotificationChannel() {
        int importance = NotificationManager.IMPORTANCE_HIGH;

        CharSequence prayer_name = getString(R.string.prayer_channel_name);
        String prayer_description = getString(R.string.prayer_channel_description);
        NotificationChannel prayer_channel = new NotificationChannel(PrayerReminderWorker.CHANNEL_ID, prayer_name, importance);
        prayer_channel.setDescription(prayer_description);

        CharSequence diary_name = getString(R.string.diary_channel_name);
        String diary_description = getString(R.string.diary_channel_description);
        NotificationChannel diary_channel = new NotificationChannel(DiaryNoteReminderWorker.CHANNEL_ID, diary_name, importance);
        diary_channel.setDescription(diary_description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(prayer_channel);
        notificationManager.createNotificationChannel(diary_channel);
    }
}