package model;

import java.sql.Blob;
import java.sql.Timestamp;

public class Version {

    public int versionId;
    public Integer fileId;
    public Integer ownerId;
    public int versionNumber;
    public Timestamp dateModified;
    public Blob content;

    public Version(int versionId, Integer fileId, Integer ownerId, int versionNumber, Timestamp dateModified, Blob content) {
        this.versionId = versionId;
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.versionNumber = versionNumber;
        this.dateModified = dateModified;
        this.content = content;
    }
}
