package dp.wkp.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import dp.wkp.R;
import dp.wkp.activities.ImageViewerActivity;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * This class is used to pick a image or load it from a url.
 * It uses Glide (to load the image in a ImageView) and uCrop to open an activity to let the user
 * crop the image.
 * It's also responsible to store the image file and treatment of errors, restoring  the old image
 * if something goes wrong.
 */
public class ImageUtils {

    public static final String IMAGE_VIEWER = "imageViewer";
    public static final int PICK_IMAGE = 100;

    private final String image_name;
    private final String sharedPreferenceKey;
    private final Activity activity;

    public ImageUtils(Activity activity, String image_name, String sharedPreferenceKey) {
        this.activity = activity;
        this.image_name = image_name;
        this.sharedPreferenceKey = sharedPreferenceKey;
    }

    public void openGallery() {
        activity.startActivityForResult(getChooserIntent(), PICK_IMAGE);
    }

    /**
     * Opens an intent to let the user choose an image.
     * If the profile image exists it will let the user choose between open a chooser intent
     * or view the image
     *
     * @return chooserIntent
     */
    public Intent getChooserIntent() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(getIntent, "View Profile Picture / Select new");

        if (existsProfilePicture()) {
            Intent viewIntent = new Intent(activity, ImageViewerActivity.class);
            viewIntent.putExtra(IMAGE_VIEWER, getProfilePicturePath());
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{viewIntent});
        }

        return chooserIntent;
    }

    public boolean existsProfilePicture() {
        return new File(getProfilePicturePath()).exists();
    }

    public String getProfilePicturePath() {
        return activity.getFilesDir().getAbsolutePath() + "/" + image_name;
    }

    public String getAuxProfilePicturePath() {
        return activity.getFilesDir().getAbsolutePath() + "/" + "aux";
    }

    /**
     * Aux picture ensures that if something goes wrong or if the user cancels uCrop, the app will
     * restore the old image.
     *
     * @throws IOException
     */
    public void copyAuxPicture() throws IOException {
        File old = new File(getProfilePicturePath());
        File newFile = new File(getAuxProfilePicturePath());
        Files.copy(old.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteAuxPicture() {
        File aux = new File(getAuxProfilePicturePath());
        aux.delete();
    }

    public void deleteProfilePicture() {
        File profile = new File(getProfilePicturePath());
        profile.delete();
    }

    /**
     * If uCrop cancels, the old image will be restored. The old image is saved in a aux file.
     * If aux doesn't exists, you may want to still delete the new one (for example, if there is none
     * image and you choose one but cancel the uCrop. It will restore the image to none only if you
     * set deleteNew to true).
     *
     * @param deleteNew
     * @throws IOException
     */
    public void undoChangePicture(boolean deleteNew) throws IOException {
        File newFile = new File(getProfilePicturePath());
        File old = new File(getAuxProfilePicturePath());
        if (old.exists())
            Files.move(old.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        else if (deleteNew)
            newFile.delete();
    }

    /**
     * Checks if the profile image file exists and if true, set the image on the ImageView profile
     *
     * @param profile
     */
    public void checkAndSetProfilePicture(ImageView profile) {
        if (existsProfilePicture())
            Glide.with(activity)
                    .load(getProfilePicturePath())
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.ic_addphoto_24dp)
                    .placeholder(R.drawable.ic_addphoto_24dp)
                    .into(profile);
    }

    public void uCrop(Uri sourceUri, Uri destinationUri, Fragment fragment) {
        UCrop.of(sourceUri, destinationUri)
                .withMaxResultSize(1080, 1080)
                .withOptions(getOptions())
                .start(activity, fragment);
    }

    public void uCrop(Uri sourceUri, Uri destinationUri) {
        UCrop.of(sourceUri, destinationUri)
                .withMaxResultSize(1080, 1080)
                .withOptions(getOptions())
                .start(activity);
    }

    public UCrop.Options getOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setFreeStyleCropEnabled(true);
        options.setMaxBitmapSize(512);
        options.setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary));
        options.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorAccent));
        return options;
    }

    /**
     * Depending of the result codes and request codes, this method will do different things,
     * If resultCode == RESULT_OK && requestCode == ImageUtils.PICK_IMAGE
     * - that means that the user picked and image from the chooser. It will copy the current image
     * (if exists) and call uCrop with the picked image
     * If resultCode == RESULT_OK && requestCode == ImageUtils.REQUEST_CROP
     * - that means that the user successfully cropped the image. The app will set the cropped
     * image to the ImageView, delete the aux (old) picture and store a boolean telling that the
     * image is set.
     * If resultCode == RESULT_CANCELLED && requestCode == ImageUtils.REQUEST_CROP
     * - that means that the user cancelled uCrop request, but the new image is already loaded.
     * So it will undo: delete the new image and restore old
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @param profile
     * @param fragment
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data, ImageView profile, @Nullable Fragment fragment) {
        if (resultCode == RESULT_OK && requestCode == ImageUtils.PICK_IMAGE) {
            Uri imageUri = data.getData();
            if (existsProfilePicture()) {
                try {
                    copyAuxPicture();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fragment == null)
                uCrop(imageUri, Uri.fromFile(new File(getProfilePicturePath())));
            else
                uCrop(imageUri, Uri.fromFile(new File(getProfilePicturePath())), fragment);

        } else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            checkAndSetProfilePicture(profile);
            deleteAuxPicture();
            updatePreferenceProfileImageSet(true);
            activity.recreate();
        } else if (resultCode == RESULT_CANCELED && requestCode == UCrop.REQUEST_CROP) {
            try {
                undoChangePicture(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updatePreferenceProfileImageSet(boolean state) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        editor.putBoolean(sharedPreferenceKey, state);
        editor.apply();
    }


    /**
     * Opens a confirmation dialog to delete profile image
     */
    public void delete() {
        if (existsProfilePicture()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(activity);
            alert.setTitle(R.string.delete_profile_image)
                    .setMessage(R.string.delete_profile_image_desc)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        deleteBoth();
                        activity.recreate();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                    }).show();
        }
    }

    /**
     * Delete profile picture and aux picture (if exists)
     */
    public void deleteBoth() {
        deleteAuxPicture();
        deleteProfilePicture();
        updatePreferenceProfileImageSet(false);
    }
}
