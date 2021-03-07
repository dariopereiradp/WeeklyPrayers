package dp.wkp.backup;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.gson.Gson;

import java.util.Collections;

import dp.wkp.R;
import dp.wkp.activities.BaseActivity;
import dp.wkp.utils.DateConverter;

/**
 * Fragment to allow Google Login, logout, disconnect, backup and restore
 */
public class GoogleSignInFragment extends Fragment implements View.OnClickListener {

    private static final String DRIVE_BACKUP_FOLDER_ID = "drive_folder_id";
    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private TextView mStatusTextView, lastBackupView;
    private ImageView profile;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "SignInActivity";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        if (account != null) {
            Drive googleDriveService = getGoogleAppDataDriveService(getContext(), account, "Weekly Prayers");
            mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_google_sign_in, container, false);
    }

    public static Drive getGoogleAppDataDriveService(Context context, GoogleSignInAccount account, String appName) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());
        return new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName(appName)
                .build();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStatusTextView = getActivity().findViewById(R.id.status);
        lastBackupView = getActivity().findViewById(R.id.detail);
        profile = getActivity().findViewById(R.id.user_image);
        SignInButton signInButton = getActivity().findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setOnClickListener(this);
        getActivity().findViewById(R.id.sign_out_button).setOnClickListener(this);
        getActivity().findViewById(R.id.disconnect_button).setOnClickListener(this);
        getActivity().findViewById(R.id.backup_button).setOnClickListener(this);
        getActivity().findViewById(R.id.restore_button).setOnClickListener(this);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if the user is already signed in and all required scopes are granted
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        if (account != null && GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_FILE))) {
            updateUI(account);
        } else {
            updateUI(null);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Updates profile image link to "none", so the user cannot import profile image from Google
     * account when signed out.
     */
    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(getActivity(), task -> {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(BaseActivity.PROFILE_IMAGE, "none").commit();
            getActivity().recreate();
        });
    }

    /**
     * Revoke will signed out and revoke permissions. User will need to give permissions again when
     * sign in.
     */
    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(getActivity(),
                task -> {
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(BaseActivity.PROFILE_IMAGE, "none").commit();
                    getActivity().recreate();
                });
    }

    /**
     * Updates image, name, buttons according to login/logout.
     *
     * @param account
     */
    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            mStatusTextView.setText(getString(R.string.signed_in_fmt, account.getDisplayName()));
            long date = PreferenceManager.getDefaultSharedPreferences(getContext()).getLong(BaseActivity.LAST_BACKUP_TIME, 0);
            if (date == 0)
                lastBackupView.setText(R.string.no_backup);
            else
                lastBackupView.setText(String.format("%s%s", getString(R.string.last_backup_on), DateConverter.dateFromLong(date)));
            getActivity().findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            getActivity().findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.backup_and_restore).setVisibility(View.VISIBLE);
            String image = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(BaseActivity.PROFILE_IMAGE, "none");
            Glide.with(getActivity())
                    .load(image)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.ic_user)
                    .placeholder(R.drawable.ic_user)
                    .into(profile);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            lastBackupView.setText(R.string.sign_in_to_backup_and_restore);
            getActivity().findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    /**
     * Gets user information from Google Account and stores the relevant on SharedPreferencesDB.
     *
     * @param completedTask
     * @param result
     */
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask, Intent result) {
        try {
            Context context = getContext();
            GoogleSignInAccount acct = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            String personName = acct.getDisplayName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();
            Uri personPhoto = acct.getPhotoUrl();

            SharedPreferences.Editor settings = PreferenceManager.getDefaultSharedPreferences(context).edit();
            settings.putString(BaseActivity.PROFILE_NAME, personName);
            settings.putString(BaseActivity.PROFILE_MAIL, personEmail);
            settings.putString(BaseActivity.PROFILE_IMAGE, personPhoto.toString());
            settings.putString(BaseActivity.PROFILE_ID, personId);
            settings.commit();

            getActivity().recreate();

            GoogleSignIn.getSignedInAccountFromIntent(result)
                    .addOnSuccessListener(googleAccount -> {
                        Log.d(TAG, "Signed in as " + googleAccount.getEmail());
                        Drive googleDriveService = getGoogleAppDataDriveService(context, googleAccount, "Weekly Prayers");
                        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                    })
                    .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task, data);
        }
    }

    public void doBackup() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.backup_message)
                .setMessage(R.string.backup_description)
                .setPositiveButton(R.string.yes, (dialog, which) -> doDirectBackup()).setNegativeButton(R.string.no, (dialog, which) -> {
        }).show();
    }

    /**
     * We need first to guarantee that Google Drive folder to backup exists. If not, create it.
     */
    public void doDirectBackup() {
        mDriveServiceHelper.createFolderIfNotExist("Weekly Prayers", null)
                .addOnSuccessListener(googleDriveFileHolder -> {
                    if (googleDriveFileHolder != null) {
                        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(getContext());
                        shp.edit().putString(DRIVE_BACKUP_FOLDER_ID, googleDriveFileHolder.getId()).apply();
                        new BackupHelper(getContext(), mDriveServiceHelper, googleDriveFileHolder.getId()).backup();
                        Gson gson = new Gson();
                        Log.i(TAG, "onSuccess of Folder creation: " + gson.toJson(googleDriveFileHolder));
                    }
                })
                .addOnFailureListener(e -> {
                    new AlertDialog.Builder(getContext()).setMessage(R.string.backup_error).show();
                    Log.i(TAG, "onFailure of Folder creation: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    public void doRestore() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.restore_message)
                .setMessage(R.string.restore_description)
                .setPositiveButton(R.string.yes, (dialog, which) -> doDirectRestore()).setNegativeButton(R.string.no, (dialog, which) -> {
        }).show();
    }

    public void doDirectRestore() {
        new BackupHelper(getContext(), mDriveServiceHelper, "").restore();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.sign_in_button) {
            signIn();
        } else if (id == R.id.sign_out_button) {
            signOut();
        } else if (id == R.id.disconnect_button) {
            revokeAccess();
        } else if (id == R.id.backup_button) {
            doBackup();
        } else if (id == R.id.restore_button) {
            doRestore();
        }
    }

}