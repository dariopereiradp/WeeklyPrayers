package dp.wkp.backup;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.google.common.io.Files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import dp.wkp.R;
import dp.wkp.activities.BaseActivity;
import dp.wkp.activities.MainActivity;
import dp.wkp.db.notes.NotesDB;
import dp.wkp.db.prayercards.PrayerCardDB;

import static android.content.ContentValues.TAG;

/**
 * This class gets all files to backup, creates a zip file with them and upload it to Google Drive.
 * It is also responsible to get the backup zip file, unzip it, copy and replace files in their
 * respective folders and then restarts the app to complete restore process.
 */
public class BackupHelper {

    private final Context context;
    private final DriveServiceHelper driveServiceHelper;
    private final String folderId;
    private static final int BUFFER_SIZE = 2048;
    private final String sharedPrefDB;
    private final String prayerDB;
    private final String notesDB;
    private final String profilePicture;
    private final String backupFolder;
    private final String backupFile = "dp.wkp.backup";
    private final String backupFileMime = "application/db";

    /**
     * Inicializes all files to backup, that are:
     * - SharedPreferences: "dp.wkp_preferences.xml"
     * - Databases: prayerDB, notesDB
     * - Images: you.jpg
     *
     * @param context
     * @param driveServiceHelper
     * @param folderId
     */
    public BackupHelper(Context context, DriveServiceHelper driveServiceHelper, String folderId) {
        this.context = context;
        this.driveServiceHelper = driveServiceHelper;
        this.folderId = folderId;
        String appFolder = context.getApplicationInfo().dataDir + File.separator;
        sharedPrefDB = appFolder + "shared_prefs" + File.separator + "dp.wkp_preferences.xml";
        backupFolder = appFolder + "backups";
        prayerDB = context.getDatabasePath(PrayerCardDB.DATABASE_NAME).toString();
        notesDB = context.getDatabasePath(NotesDB.DATABASE_NAME).toString();
        profilePicture = context.getFilesDir().getAbsolutePath() + File.separator + MainActivity.PROFILE_PICTURE_NAME_YOU;
    }

    /**
     * Gets a list of files and save them as a zip file on file zipFile.
     *
     * @param files
     * @param zipFile
     * @throws IOException
     */
    public static void zip(List<File> files, File zipFile) throws IOException {
        BufferedInputStream origin;
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            byte[] data = new byte[BUFFER_SIZE];
            for (File file : files) {
                if (file.exists()) {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    origin = new BufferedInputStream(fileInputStream, BUFFER_SIZE);
                    String filePath = file.getAbsolutePath();
                    try {
                        ZipEntry entry = new ZipEntry(filePath.substring(filePath.lastIndexOf("/") + 1));
                        out.putNextEntry(entry);
                        int count;
                        while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                            out.write(data, 0, count);
                        }
                    } finally {
                        origin.close();
                    }
                }
            }
        }
    }

    /**
     * Gets a zipFile and unzip all files to the given location folder.
     *
     * @param zipFile
     * @param location
     */
    public static void unzip(String zipFile, String location) {
        int size;
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            if (!location.endsWith(File.separator)) {
                location += File.separator;
            }
            File f = new File(location);
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE))) {
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = location + ze.getName();
                    File unzipFile = new File(path);
                    if (ze.isDirectory()) {
                        if (!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    } else {
                        // check for and create parent directories if they don't exist
                        File parentDir = unzipFile.getParentFile();
                        if (null != parentDir) {
                            if (!parentDir.isDirectory()) {
                                parentDir.mkdirs();
                            }
                        }
                        // unzip the file
                        FileOutputStream out = new FileOutputStream(unzipFile, false);
                        BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
                        try {
                            while ((size = zin.read(buffer, 0, BUFFER_SIZE)) != -1) {
                                fout.write(buffer, 0, size);
                            }
                            zin.closeEntry();
                        } finally {
                            fout.flush();
                            fout.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unzip exception", e);
        }
    }

    /**
     * Saves old backup file id (id is Google Drive file id) to SharedPreferencesDB.
     * We need to save it so that we can delete it when new backup is done. Because Google Drive
     * will store 2 files with same name as different files (different ids). So, we need to delete the
     * old one and keep only the new one.
     *
     * @param fileHolder
     */
    private void atualizarOldId(GoogleDriveFileHolder fileHolder) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("oldBackupId", fileHolder.getId()).commit();
    }

    /**
     * Before doing a backup, it puts LOAD_ALARMS with TRUE, and than, backups updated shared preferences
     * file. This ensures that when restore, this value will be true and alarms will be reloaded. At
     * the end of backup process, this value will be put to FALSE again.
     * <p>
     * This method will create a zip file containing all files to backup. Before uploading it, it will
     * search for an old backup file and it will store it on SharedPreferencesDB.
     * After that, it will upload it to Google Drive, and on success, it will delete the old backup
     * file, if exists.
     */
    public void backup() {
        File folder = new File(context.getExternalFilesDir(null) + File.separator + context.getResources().getString(R.string.app_name));
        boolean success = true;
        if (!folder.exists())
            success = folder.mkdirs();
        if (success) {
            closeDatabases();
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(BaseActivity.LOAD_ALARMS, true).commit();
            try {
                List<File> filesToBackup = new ArrayList<>();

                File prayerDBFile = new File(prayerDB);
                filesToBackup.add(prayerDBFile);
                File notesDBFile = new File(notesDB);
                filesToBackup.add(notesDBFile);
                File sharedPrefFile = new File(sharedPrefDB);
                filesToBackup.add(sharedPrefFile);
                File profilePictureFile = new File(profilePicture);
                filesToBackup.add(profilePictureFile);

                File backupFolderDirectory = new File(backupFolder);
                if (!backupFolderDirectory.exists())
                    backupFolderDirectory.mkdir();

                driveServiceHelper.searchFile(backupFile, backupFileMime)
                        .addOnSuccessListener(this::atualizarOldId)
                        .addOnFailureListener(Throwable::printStackTrace);

                File zipFile = new File(backupFolder + File.separator + backupFile);
                zip(filesToBackup, zipFile);
                driveServiceHelper.uploadFile(zipFile, backupFileMime, folderId)
                        .addOnSuccessListener(this::removeOldBackupFile)
                        .addOnFailureListener(Throwable::printStackTrace);
            } catch (Exception e) {
                Toast.makeText(context, context.getString(R.string.backup_error_toast), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes old backup file from Google Drive (if exists) to prevent duplicates.
     * After that, it will put LOAD_ALARMS to false again and it will update LAST_BACKUP_TIME and
     * recreates activity to show last backup time.
     * <p>
     * Updating the time only after a backup is done is the best approach I found, because upload process
     * may fail for many different reasons. So I don't want to give a wrong timestamp. I need to guarantee
     * that backup was successfully done before updating last time backup.
     * The problem is that when user restores a backup, last backup time will be wrong (it will be second to last)
     * <p>
     * In a future update, I may try to fix it by:
     * 1. Updating timestamp before creating zip file, but saving old timestamp in a variable.
     * 2. On failure, put old timestamp again.
     *
     * @param googleDriveFileHolder
     */
    private void removeOldBackupFile(GoogleDriveFileHolder googleDriveFileHolder) {
        String oldId = PreferenceManager.getDefaultSharedPreferences(context).getString("oldBackupId", null);
        if (oldId != null) {
            driveServiceHelper.deleteFolderFile(oldId);
        }
        Toast.makeText(context, context.getString(R.string.backup_completed), Toast.LENGTH_SHORT).show();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(BaseActivity.LOAD_ALARMS, false).commit();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(BaseActivity.LAST_BACKUP_TIME, System.currentTimeMillis()).commit();
        MainActivity.getInstance().recreate();
    }

    /**
     * Restore process
     * 1. CLose databases
     * 2. Creates backup folder (if don't exists)
     * 3. Search for backup file in Google Drive (search for name)
     * 4. Call #doRestore(googleDriveFileHolder)
     */
    public void restore() {
        try {
            closeDatabases();
            File backupFolderDirectory = new File(backupFolder);
            if (!backupFolderDirectory.exists()) {
                backupFolderDirectory.mkdir();
            }
            driveServiceHelper.searchFile(backupFile, backupFileMime)
                    .addOnSuccessListener(this::doRestore)
                    .addOnFailureListener(e -> {
                        new AlertDialog.Builder(context).setMessage(R.string.restore_error).show();
                        e.printStackTrace();
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads backup file from Google Drive to a local backup file and onSuccess, call
     * #copyFilesAndRestart()
     *
     * @param googleDriveFileHolder
     */
    private void doRestore(GoogleDriveFileHolder googleDriveFileHolder) {
        if (googleDriveFileHolder != null) {
            File zipFile = new File(backupFolder + "/dp.pfh.backup");
            driveServiceHelper.downloadFile(zipFile, googleDriveFileHolder.getId())
                    .addOnSuccessListener(fileHolder -> copyFilesAndRestart())
                    .addOnFailureListener(e -> {
                        new AlertDialog.Builder(context).setMessage(R.string.restore_error).show();
                        e.printStackTrace();
                    });
        }
    }

    /**
     * This method is used for restore purposes.
     * It will unzip backup file and check for the existence of each file that can be restored.
     * For each file, copy to original folder and delete the temporary unzipped file.
     * After that, restarts app.
     */
    private void copyFilesAndRestart() {
        try {
            File zipFile = new File(backupFolder + "/dp.pfh.backup");
            unzip(zipFile.getAbsolutePath(), backupFolder);
            File zipPrayerDB = new File(backupFolder + "/" + PrayerCardDB.DATABASE_NAME);
            File zipDiaryDB = new File(backupFolder + "/" + NotesDB.DATABASE_NAME);
            File zipSharedPref = new File(backupFolder + "/" + "dp.pfh_preferences.xml");
            File zipProfileImg = new File(backupFolder + "/" + "you.jpg");

            if (zipPrayerDB.exists()) {
                Files.copy(zipPrayerDB, new File(prayerDB));
                zipPrayerDB.delete();
            }
            if (zipDiaryDB.exists()) {
                Files.copy(zipDiaryDB, new File(notesDB));
                zipDiaryDB.delete();
            }
            if (zipSharedPref.exists()) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
                Files.copy(zipSharedPref, new File(sharedPrefDB));
                zipSharedPref.delete();
            }
            if (zipProfileImg.exists()) {
                Files.copy(zipProfileImg, new File(profilePicture));
                zipProfileImg.delete();
            }

            Intent i = context.getPackageManager().
                    getLaunchIntentForPackage(context.getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeDatabases() {
        PrayerCardDB.getInstance(context).close();
        NotesDB.getInstance(context).close();
    }
}
