package repository;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import model.Comment;
import model.Document;
import model.Folder;
import model.FolderContent;
import model.Permission;
import model.Version;
import ui.MainFrame;
import ui.MainFrame.User;

public class DocumentRepository {

    // This class is used when getting children folders
    class CondensedFolderData
    {
        public int folderId;
        public String folderName;

        public CondensedFolderData(int folderId, String folderName) {
            this.folderId = folderId;
            this.folderName = folderName;
        }
    }
    
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
        "SELECT f.folder_id AS `Id`, f.folder_name AS `Name`, f.folder_type AS `Type`, f.date_created AS `Date Created`, f.date_modified AS `Date Modified`, -1 AS `Size`\n" + //
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
        "WHERE v.file_id=" + Integer.toString(fileId) + "\n" + // 
        "ORDER BY version_number desc";

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

    public String getLatestFileVersionContent(int fileId, int userId)
    {
        Version latestVersion = getLatestFileVersion(fileId, userId);
        if (latestVersion.content == null) return "";
        try {
            String content = new String(latestVersion.content.getBytes(1, (int) latestVersion.content.length()), StandardCharsets.UTF_8);
            return content;
        } catch (SQLException e) {
            System.err.println("An error occured while retrieving latest content for file with id: " + fileId);
            e.printStackTrace();
            return null;
        }
    }

    public Document getDocumentById(int documentId, int userId)
    {
        String documentQuery = 
        "SELECT d.file_id,owner_id,parent_folder_id,created_by,file_size,date_created,date_modified,file_type,file_name FROM document d \r\n" + //
        "JOIN permission p on p.file_id=d.file_id AND p.user_id=" + Integer.toString(userId) + "\r\n" + //
        "WHERE d.file_id=" + Integer.toString(documentId);
        try {
            PreparedStatement statement = connection.prepareStatement(documentQuery);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int ownerId = rs.getInt("owner_id");
                Integer parentFolderId = rs.getInt("parent_folder_id");
                int createdBy = rs.getInt("created_by");
                int fileSize = rs.getInt("file_size");
                Timestamp dateCreated = rs.getTimestamp("date_created");
                Timestamp dateModified = rs.getTimestamp("date_modified");
                String fileType = rs.getString("file_type");
                String fileName = rs.getString("file_name");
                return new Document(documentId, ownerId, parentFolderId, createdBy, fileSize, dateCreated, dateModified, fileType, fileName);
            } else { 
                return null;
            }
        } catch (Exception e) {
            System.err.println("An error occured while retrieving document with id: " + documentId);
            e.printStackTrace();
            return null;
        }
    }

    public String getPathOfFolder(int parentFolderId)
    {
        if (parentFolderId == 0) return "";
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT folder_path FROM parent_folder WHERE parent_folder_id=" + parentFolderId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                return rs.getString("folder_path");
            } else {
                return null;
            }
        } catch (Exception e) {
            System.err.println("An error occured while retrieving folder path with parentFolderId: " + parentFolderId);
            e.printStackTrace();
            return null;
        }
    }

    public Integer getParentFolderIdOfFolder(int folderId)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM folder WHERE folder_id=" + folderId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("parent_folder_id");
            } else {
                return null;
            }
        } catch (Exception e) {
            System.err.println("An error occured while retrieving parentFolderId of folder with id: " + folderId);
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

    public int getLatestVersionNumberForDocument(int documentId)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(version_number) FROM doc_version WHERE file_id=" + documentId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                return rs.getInt("max(version_number)");
            } else {
                return -1;
            }
        } catch (Exception e) {
            System.err.println("An error occured while retrieving comments for file with id: " + documentId);
            e.printStackTrace();
            return -1;
        }
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
                statement.setInt(1, nextId);
                statement.setInt(2, version.fileId);
                statement.setInt(3, version.ownerId);
                statement.setInt(4, version.versionNumber);
                statement.setTimestamp(5, version.dateModified);
                statement.setBlob(6, version.content);
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
                statement.setInt(1, nextId);
                statement.setInt(2, comment.fileId);
                statement.setInt(3, comment.createdBy);
                statement.setString(4, comment.content);
                statement.setTimestamp(5, comment.timePosted);
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
            PreparedStatement statement = connection.prepareStatement("SELECT max(file_id) FROM document");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(file_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO document VALUES (?,?,?,?,?,?,?,?,?)");
                statement.setInt(1, nextId);
                statement.setInt(2, document.ownerId);
                statement.setObject(3, document.parentFolderId);
                statement.setInt(4, document.createdBy);
                statement.setInt(5, document.fileSize);
                statement.setTimestamp(6, document.dateCreated);
                statement.setTimestamp(7, document.dateModified);
                statement.setString(8, document.fileType);
                statement.setString(9, document.fileName);
                return 
                    statement.executeUpdate() > 0 && 
                    addUserPermForItem(new Permission(0, nextId, null, document.ownerId, null, 3)) && 
                    addFileVersion(new Version(0, nextId, document.ownerId, 1, document.dateCreated, null), document.fileSize);
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
            PreparedStatement statement = connection.prepareStatement("DELETE FROM document WHERE file_id=" + documentId);
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
                statement.setInt(1, nextId);
                statement.setInt(2, folder.ownerId);
                statement.setObject(3, folder.parentFolderId);
                statement.setInt(4, folder.createdBy);
                statement.setTimestamp(5, folder.dateCreated);
                statement.setTimestamp(6, folder.dateModified);
                statement.setString(7, folder.folderType); // always going to be "dir"
                statement.setString(8, folder.folderName);
                return 
                    statement.executeUpdate() > 0 &&
                    addUserPermForItem(new Permission(0, null, nextId, folder.ownerId, null, 3)) &&
                    addParentFolder(nextId, folder.folderName);
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

    public boolean updateFolderName(int folderId, String newName)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE folder SET folder_name=? WHERE folder_id=" + folderId);
            statement.setString(1, newName);
            return statement.executeUpdate() > 0 && updateParentFolderPath(folderId, newName);
        } catch (Exception e) {
            System.err.println("An error occured while updating name of folder with id: " + folderId);
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDocumentName(int documentId, String newName)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE document SET file_name=? WHERE file_id=" + documentId);
            statement.setString(1, newName);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while updating name of file with id: " + documentId);
            e.printStackTrace();
            return false;
        }
    }

    // Update parentFolder path based on new name
    public boolean updateParentFolderPath(int parentFolderId, String newName)
    {
        if (parentFolderId == 0) return true;

        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE parent_folder SET folder_path=? WHERE parent_folder_id=" + parentFolderId);
            statement.setString(1, getPathOfFolder(getParentFolderIdOfFolder(parentFolderId)) + "/" + newName);
            if (statement.executeUpdate() <= 0) return false;

            ArrayList<CondensedFolderData> children = getChildFoldersOfParentFolder(parentFolderId);
            for (CondensedFolderData cfd : children){
                if (!updateParentFolderPath(cfd.folderId, cfd.folderName)) { return false; }
            }
            return true;

        } catch (Exception e) {
            System.err.println("An error occured while updating path of parent folder with id: " + parentFolderId);
            e.printStackTrace();
            return false;
        }
    }

    // Return a list of objects, each containing the child folder id and name 
    public ArrayList<CondensedFolderData> getChildFoldersOfParentFolder(int parentFolderId)
    {
        ArrayList<CondensedFolderData> children = new ArrayList<CondensedFolderData>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT folder_id,folder_name FROM folder WHERE parent_folder_id=" + parentFolderId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int folderId = rs.getInt("folder_id");
                String folderName = rs.getString("folder_name");
                children.add(new CondensedFolderData(folderId, folderName));
            }
            return children;
        } catch (Exception e) {
            System.err.println("An error occured while getting children folder ids of parent folder with id: " + parentFolderId);
            e.printStackTrace();
            return null;
        }
    }

    public boolean addParentFolder(int folderId, String name)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO parent_folder VALUES (?,?)");
            statement.setInt(1, folderId);
            statement.setString(2, getPathOfFolder(getParentFolderIdOfFolder(folderId)) + "/" + name);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while trying to add parent folder");
            e.printStackTrace();
            return false;
        }
    }

    public boolean addUserPermForItem(Permission permission)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(permission_id) FROM permission");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(permission_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO permission VALUES (?,?,?,?,?,?)");
                statement.setInt(1, nextId);
                statement.setObject(2, permission.fileId);
                statement.setObject(3, permission.folderId);
                statement.setObject(4, permission.userId);
                statement.setObject(5, permission.teamId);
                statement.setInt(6, permission.abilities);
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
