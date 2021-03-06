package dp.wkp.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import dp.wkp.R;
import dp.wkp.fragments.DiaryFragment;
import dp.wkp.fragments.PrayerFragment;

/**
 * Activity that handles fingerprint (or similar) authentication. If it's not available on the device,
 * then it won't be available on the app. The user will be able to enter without authentication.
 * Use: #BiometricManager
 * However, data is not encrypted. The only purpose of authentication is let the user enters in the
 * next activity of the app.
 * <p>
 * This activity is also responsible to handle notification clicks. Every notification received while
 * app is closed will go to SplashScreen and then Login activity. This activity will process extra data and will continue
 * app cycle, opening respective activity/fragment with back stack if applicable.
 */
public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ImageView fingerprint_button = findViewById(R.id.fingerprint_button);

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                configureAuthentication(fingerprint_button);
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                TextView textView = findViewById(R.id.authenticate);
                textView.setText(R.string.authentication_not_available);
                fingerprint_button.setOnClickListener(v -> openActivity());
                break;
        }
    }

    /**
     * Configure BiometricPrompt
     *
     * @param fingerprint_button
     */
    private void configureAuthentication(ImageView fingerprint_button) {
        Executor executor = ContextCompat.getMainExecutor(this);

        final BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                openActivity();
            }

        });
        final BiometricPrompt.PromptInfo promptInfo;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.authenticate))
                    .setDescription(getString(R.string.auth_desc))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build();
        } else {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.authenticate))
                    .setDescription(getString(R.string.auth_desc))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build();
        }
        fingerprint_button.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
    }

    /**
     * Opens activity (or fragment) according to extra fields of intent.
     * This is useful when opening the app through notification click.
     * 1. If it's a prayer reminder notification, it will open PrayerDay activity
     * 2. If it's a note reminder notification, it will open EditNote activity.
     * 3. If it's a normal app opening, it will open Main Activity in PrayerFragment (that is, the
     * initial fragment)
     * <p>
     * The method uses Pending Intent with backIntent to create a back stack, so when the user clicks
     * the back button, it will have consistent behaviour. For example, if user clicks in a note
     * reminder notification, it will open EditNoteActivity. If the user then clicks on back button,
     * it will open MainActivity in DiaryFragment instead of closing the app. If he presses back again,
     * however, the app will close.
     */
    private void openActivity() {
        try {
            Intent intent = getIntent();
            Intent backIntent = new Intent(getBaseContext(), MainActivity.class);
            backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent mainPendingIntent;
            if (intent.hasExtra(PrayerFragment.CARD_ID)) {
                int day = intent.getExtras().getInt(PrayerFragment.CARD_ID);
                Intent prayerDay = new Intent(getBaseContext(), PrayerDayActivity.class);
                prayerDay.putExtra(PrayerFragment.CARD_ID, day);
                mainPendingIntent = PendingIntent.getActivities(getBaseContext(), (day + 1) * 10, new Intent[]{backIntent, prayerDay}, PendingIntent.FLAG_ONE_SHOT);
                mainPendingIntent.send();
            } else if (intent.hasExtra(EditNoteActivity.NOTE_EXTRA_Key)) {
                int id = intent.getExtras().getInt(EditNoteActivity.NOTE_EXTRA_Key);
                Intent noteIntent = new Intent(getBaseContext(), EditNoteActivity.class);
                noteIntent.putExtra(EditNoteActivity.NOTE_EXTRA_Key, id);
                backIntent.putExtra(MainActivity.FRAGMENT_TO_LOAD, DiaryFragment.DIARY_FRAGMENT);
                mainPendingIntent = PendingIntent.getActivities(getBaseContext(), id, new Intent[]{backIntent, noteIntent}, PendingIntent.FLAG_ONE_SHOT);
                mainPendingIntent.send();
            } else {
                Intent success = new Intent(LoginActivity.this, MainActivity.class);
                LoginActivity.this.startActivity(success);
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            Intent success = new Intent(LoginActivity.this, MainActivity.class);
            LoginActivity.this.startActivity(success);
        }
        Toast.makeText(getApplicationContext(), R.string.welcome, Toast.LENGTH_SHORT).show();
    }
}