package repository;

import java.io.Console;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;

import model.Comment;
import model.Document;
import model.Folder;
import model.FolderContent;
import model.Permission;
import model.Version;
import ui.MainFrame;
import ui.MainFrame.User;

public class DocumentRepository {
    
    private Connection connection;

    public DocumentRepository(Connection connection) {
        this.connection = connection;
    }

    public ArrayList<FolderContent> getFolderContentsForUser(int parentFolderId, int userId)
    {
        ArrayList<FolderContent> contents = new ArrayList<FolderContent>();
        String parentFolderExistsQuery = "SELECT * from parent_folder WHERE parent_folder_id " + (parentFolderId == 0 ? "IS NULL":"=" + Integer.toString(parentFolderId));
        String contentsQuery = 
        "SELECT d.file_id AS `Id`, d.file_name AS `Name`, d.file_type AS `Type`, d.date_created AS `Date Created`, d.date_modified AS `Date Modified`, d.file_size AS `Size`\n" + //
        "FROM document d\n" + //
        "JOIN PERMISSION p on p.user_id=" + Integer.toString(userId) + " AND p.file_id=d.file_id\n" + //
        "WHERE d.PARENT_FOLDER_ID " + (parentFolderId == 0 ? "IS NULL":"=" + Integer.toString(parentFolderId)) + "\r\n" +
        "UNION\n" + //
        "SELECT f.folder_id AS `Id`, f.folder_name AS `Name`, f.folder_type AS `Type`, f.date_created AS `Date Created`, f.date_modified AS `Date Modified`, f.folder_size AS `Size`\n" + //
        "FROM folder f\n" + //
        "JOIN PERMISSION p on p.user_id=" + Integer.toString(userId) + " AND p.folder_id=f.folder_id\n" + //
        "WHERE f.PARENT_FOLDER_ID " + (parentFolderId == 0 ? "IS NULL":"=" + Integer.toString(parentFolderId)) + "\r\n" + //
        "ORDER BY `Name` ASC";       

        try {
            PreparedStatement statement = connection.prepareStatement(parentFolderExistsQuery);
            ResultSet rs = statement.executeQuery(); 
            if (rs.next() || parentFolderId==0){
                statement = connection.prepareStatement(contentsQuery);
                rs = statement.executeQuery();
                while (rs.next()){
                    int id = rs.getInt("Id");
                    String name = rs.getString("Name");
                    String type = rs.getString("Type");
                    String dateCreated = rs.getString("Date Created");
                    String dateModified = rs.getString("Date Modified");
                    int size = rs.getInt("Size");
                    contents.add(new FolderContent(id, name, type, dateCreated, dateModified, size));
                }
            }
            else{
                return contents;
            }
        }  catch (Exception e){
            System.err.println("An error occured while retrieving folder contents for parent folder with id: " + parentFolderId);
            e.printStackTrace();
        }

        return contents;
    }

    public Version getLatestFileVersion(int fileId, int userId)
    {
        String versionQuery =
        "SELECT version_id,v.file_id,owner_id,version_number,date_modified,content FROM doc_version v   \n" + //
        "JOIN permission p on p.file_id=v.file_id AND p.user_id=" + Integer.toString(userId) + " \n" + //
        "WHERE v.file_id=" + Integer.toString(fileId);

        try {
            PreparedStatement statement = connection.prepareStatement(versionQuery);
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int versionId = rs.getInt("version_id");
                int ownerId = rs.getInt("owner_id");
                int versionNumber = rs.getInt("version_number");
                Timestamp dateModified = rs.getTimestamp("date_modified");
                Blob content = rs.getBlob("content");
                return new Version(versionId, fileId, ownerId, versionNumber, dateModified, content);
            }
            else {
                return null;
            }
        } catch (Exception e) {
            System.err.println("An error occured while retrieving latest version for file with id: " + fileId);
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Comment> getFileComments(int fileId)
    {
        ArrayList<Comment> comments = new ArrayList<Comment>();
        String commentsQuery =
        "SELECT * from doc_comment\r\n" + //
        "WHERE file_id=" + Integer.toString(fileId);
        try {
            PreparedStatement statement = connection.prepareStatement(commentsQuery);
            ResultSet rs = statement.executeQuery();
            while (rs.next()){
                int commentId = rs.getInt("comment_id");
                int createdBy = rs.getInt("created_by");
                String content = rs.getString("content");
                Timestamp timePosted = rs.getTimestamp("time_posted");
                comments.add(new Comment(commentId, fileId, createdBy, content, timePosted));
            }
        } catch (Exception e){
            System.err.println("An error occured while retrieving comments for file with id: " + fileId);
            e.printStackTrace();
        }

        return comments;
    }

    private boolean updateFileSize(int fileId, int newFileSize)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE document SET file_size=" + newFileSize + " WHERE file_id=" + fileId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while updating file size with id: " + fileId);
            e.printStackTrace();
            return false;
        }
    }

    // Add a new file version and update the file size
    public boolean addFileVersion(Version version, int newFileSize)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(version_id) FROM doc_version");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(version_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO doc_version VALUES (?,?,?,?,?,?)");
                statement.setInt(0, nextId);
                statement.setInt(1, version.fileId);
                statement.setInt(2, version.ownerId);
                statement.setInt(3, version.versionNumber);
                statement.setTimestamp(4, version.dateModified);
                statement.setBlob(5, version.content);
                return statement.executeUpdate() > 0 && updateFileSize(version.fileId, newFileSize);
            } else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occured while adding a version to file with id: " + version.fileId);
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertDocumentComment(Comment comment)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(comment_id) FROM doc_comment");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(comment_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO doc_comment VALUES (?,?,?,?)");
                statement.setInt(0, nextId);
                statement.setInt(1, comment.fileId);
                statement.setInt(2, comment.createdBy);
                statement.setString(3, comment.content);
                statement.setTimestamp(4, comment.timePosted);
                return statement.executeUpdate() > 0;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occured while adding a comment to document with id: " + comment.fileId);
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDocumentComment(int commentId)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM doc_comment WHERE comment_id=" + commentId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while removing comment with id: " + commentId);
            e.printStackTrace();
            return false;
        }
    }

    public boolean addDocument(Document document)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(folder_id) FROM folder");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(folder_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO document VALUES (?,?,?,?,?,?,?,?,?)");
                statement.setInt(0, nextId);
                statement.setInt(1, document.ownerId);
                statement.setInt(2, document.parentFolderId);
                statement.setInt(3, document.createdBy);
                statement.setInt(4, document.fileSize);
                statement.setTimestamp(5, document.dateCreated);
                statement.setTimestamp(6, document.dateModified);
                statement.setString(7, document.fileType);
                statement.setString(8, document.fileName);
                return statement.executeUpdate() > 0;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occured while adding a document.");
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDocument(int documentId)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM document WHERE document_id=" + documentId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while removing document with id: " + documentId);
            e.printStackTrace();
            return false;
        }
    }

    public boolean addFolder(Folder folder)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(folder_id) FROM folder");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(folder_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO folder VALUES (?,?,?,?,?,?,?,?)");
                statement.setInt(0, nextId);
                statement.setInt(1, folder.ownerId);
                statement.setInt(2, folder.parentFolderId);
                statement.setInt(3, folder.createdBy);
                statement.setTimestamp(4, folder.dateCreated);
                statement.setTimestamp(5, folder.dateModified);
                statement.setString(6, "dir");
                statement.setString(7, folder.folderName);
                return statement.executeUpdate() > 0;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occured while adding a folder.");
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteFolder(int folderId)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM folder WHERE folder_id=" + folderId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while removing folder with id: " + folderId);
            e.printStackTrace();
            return false;
        }
    }

    public boolean addUserPermForDocument(Permission permission)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(permission_id) FROM permission");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(permission_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO doc_user VALUES (?,?,?,?,?,?)");
                statement.setInt(0, nextId);
                statement.setInt(1, permission.fileId);
                statement.setInt(2, permission.folderId);
                statement.setInt(3, permission.userId);
                statement.setInt(4, permission.teamId);
                statement.setInt(5, permission.abilities);
                return statement.executeUpdate() > 0;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occured while adding a permission.");
            e.printStackTrace();
            return false;
        }
    }

    // Change abilities of a user
    public boolean updateUserPermForDocument(Permission permission)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE permission SET abilities=" + permission.abilities + " WHERE permission_id=" + permission.permissionId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while updating permission with id: " + permission.permissionId);
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeUserPermForDocument(int permissionId)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM permission WHERE permission_id=" + permissionId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while removing permission with id: " + permissionId);
            e.printStackTrace();
            return false;
        }
    }
}
