package model;

import java.sql.Timestamp;

public class Comment {
    public int commentId;
    public int fileId;
    public int createdBy;
    public String content;
    public Timestamp timePosted;

    public Comment(int commentId, int fileId, int createdBy, String content, Timestamp timePosted) {
        this.commentId = commentId;
        this.fileId = fileId;
        this.createdBy = createdBy;
        this.content = content;
        this.timePosted = timePosted;
    }
}
