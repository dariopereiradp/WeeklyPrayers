package dp.wkp.backup;

/**
 * Simple class to represent a file from Google Drive.
 * We only need id. However, this class can be updated to store many other things like file name,
 * size, last modified data, etc...
 */
public class GoogleDriveFileHolder {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
