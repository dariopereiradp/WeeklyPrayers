package dp.wkp.backup;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dp.wkp.activities.MainActivity;

import static android.content.ContentValues.TAG;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
public class DriveServiceHelper {

    public static final int PERMISSION_REQUIRED_FOR_BACKUP = 555;
    public static final int PERMISSION_REQUIRED_FOR_RESTORE = 556;
    public static final String TYPE_GOOGLE_DRIVE_FOLDER = "application/vnd.google-apps.folder";
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    public Task<GoogleDriveFileHolder> createFolderIfNotExist(final String folderName, @Nullable final String parentFolderId) {
        return Tasks.call(mExecutor, () -> {
            FileList result = getFileListResult(folderName);
            if (result != null) {
                GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();

                if (result.getFiles().size() > 0) {
                    googleDriveFileHolder.setId(result.getFiles().get(0).getId());
                } else {
                    Log.d(TAG, "createFolderIfNotExist: not found");
                    List<String> root;
                    if (parentFolderId == null) {
                        root = Collections.singletonList("root");
                    } else {
                        root = Collections.singletonList(parentFolderId);
                    }
                    File metadata = new File()
                            .setParents(root)
                            .setMimeType(TYPE_GOOGLE_DRIVE_FOLDER)
                            .setName(folderName);

                    File googleFile = mDriveService.files().create(metadata).execute();
                    if (googleFile == null) {
                        throw new IOException("Null result when requesting file creation.");
                    }
                    googleDriveFileHolder.setId(googleFile.getId());
                }
                return googleDriveFileHolder;
            } else
                return null;
        });
    }

    public FileList getFileListResult(String folderName) {
        try {
            return mDriveService.files().list()
                    .setQ("mimeType = '" + TYPE_GOOGLE_DRIVE_FOLDER + "' and name = '" + folderName + "' and trashed=false")
                    .setSpaces("drive")
                    .execute();
        } catch (IOException e) {
            UserRecoverableAuthIOException e1 = (UserRecoverableAuthIOException) e;
            MainActivity.getInstance().startActivityForResult(e1.getIntent(), PERMISSION_REQUIRED_FOR_BACKUP);
            return null;
        }
    }

    public Task<GoogleDriveFileHolder> searchFile(String fileName, String mimeType) {
        return Tasks.call(mExecutor, () -> {
            try {
                FileList result = mDriveService.files().list()
                        .setQ("name = '" + fileName + "' and mimeType ='" + mimeType + "'")
                        .setSpaces("drive")
                        .setFields("files(id, name,size,createdTime,modifiedTime,starred)")
                        .execute();
                GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
                if (result.getFiles().size() > 0) {
                    googleDriveFileHolder.setId(result.getFiles().get(0).getId());
                }
                return googleDriveFileHolder;
            } catch (IOException e) {
                UserRecoverableAuthIOException e1 = (UserRecoverableAuthIOException) e;
                MainActivity.getInstance().startActivityForResult(e1.getIntent(), PERMISSION_REQUIRED_FOR_RESTORE);
                return null;
            }
        });
    }

    public Task<GoogleDriveFileHolder> uploadFile(final java.io.File localFile, final String mimeType, @Nullable final String folderId) {
        return Tasks.call(mExecutor, () -> {
            List<String> root;
            if (folderId == null) {
                root = Collections.singletonList("root");
            } else {
                root = Collections.singletonList(folderId);
            }
            File metadata = new File()
                    .setParents(root)
                    .setMimeType(mimeType)
                    .setName(localFile.getName());
            FileContent fileContent = new FileContent(mimeType, localFile);
            File fileMeta = mDriveService.files().create(metadata, fileContent).execute();
            GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
            googleDriveFileHolder.setId(fileMeta.getId());
            return googleDriveFileHolder;
        });
    }

    public Task<Void> downloadFile(java.io.File targetFile, String fileId) {
        return Tasks.call(mExecutor, () -> {
            OutputStream outputStream = new FileOutputStream(targetFile);
            mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            return null;
        });
    }

    public Task<Void> deleteFolderFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the metadata as a File object.
            if (fileId != null) {
                mDriveService.files().delete(fileId).execute();
            }
            return null;
        });
    }
}
