package model;

import java.sql.Timestamp;

public class Document {
    public int fileId;
    public int ownerId;
    public Integer parentFolderId;
    public int createdBy;
    public int fileSize;
    public Timestamp dateCreated;
    public Timestamp dateModified;
    public String fileType;
    public String fileName;

    public Document(int fileId, int ownerId, Integer parentFolderId, int createdBy, int fileSize,
                  Timestamp dateCreated, Timestamp dateModified, String fileType, String fileName) {
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.parentFolderId = parentFolderId;
        this.createdBy = createdBy;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.fileType = fileType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
}