package dp.wkp.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import dp.wkp.R;
import dp.wkp.fragments.PrayerFragment;
import dp.wkp.notifications.NotificationType;

/**
 * SlashScreen activity. It also receives extras when it's opened by a notification. Then it will
 * call LoginActivity with extras, if necessary.
 */
public class SplashActivity extends BaseActivity {

    private static final long SPLASH_TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null)
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        }

        CardView pfh = findViewById(R.id.pfh);
        ImageView dp = findViewById(R.id.dp);
        TextView byDP = findViewById(R.id.byDP);

        Animation topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        Animation bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);
        Animation middleAnimation = AnimationUtils.loadAnimation(this, R.anim.middle_animation);

        pfh.setAnimation(topAnim);
        dp.setAnimation(bottomAnimation);
        byDP.setAnimation(middleAnimation);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = getIntent();
            Intent login = new Intent(SplashActivity.this, LoginActivity.class);
            if (intent.hasExtra(BaseActivity.NOTIFICATION_TYPE)) {
                NotificationType type = (NotificationType) intent.getExtras().get(BaseActivity.NOTIFICATION_TYPE);
                switch (type) {
                    case PRAYER_REMINDER:
                        int day = intent.getExtras().getInt(PrayerFragment.CARD_ID);
                        login = new Intent(SplashActivity.this, LoginActivity.class);
                        login.putExtra(PrayerFragment.CARD_ID, day);
                        break;
                    case DIARY_REMINDER:
                        int id = intent.getExtras().getInt(EditNoteActivity.NOTE_EXTRA_Key);
                        login = new Intent(SplashActivity.this, LoginActivity.class);
                        login.putExtra(EditNoteActivity.NOTE_EXTRA_Key, id);
                        break;
                }
            }
            startActivity(login);
            finish();
        }, SPLASH_TIME_OUT);
    }
}