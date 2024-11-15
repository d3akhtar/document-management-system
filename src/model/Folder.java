package model;

import java.sql.Timestamp;

public class Folder {
    public int folderId;
    public int ownerId;
    public Integer parentFolderId;
    public int createdBy;
    public Timestamp dateCreated;
    public Timestamp dateModified;
    public String folderType;
    public String folderName;

    public Folder(int folderId, int ownerId, Integer parentFolderId, int createdBy,
                  Timestamp dateCreated, Timestamp dateModified, String folderType, String folderName) {
        this.folderId = folderId;
        this.ownerId = ownerId;
        this.parentFolderId = parentFolderId;
        this.createdBy = createdBy;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.folderType = folderType;
        this.folderName = folderName;
    }
}
