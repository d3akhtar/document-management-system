package model;

// Folders can either have files or folders, so this class is made so that it can refer to either a file or a folder
public class FolderContent {
    public int id;
    public String name;
    public String type;
    public String dateCreated;
    public String dateModified;
    public int size;

    public FolderContent(int id, String name, String type, String dateCreated, String dateModifed, int size) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.dateCreated = dateCreated;
        this.dateModified = dateModifed;
        this.size = size;
    }

    public String toString()
    {
        return "[FolderContent] id: " + id + " name: " + name + " type: " + type + " dateCreated: " + dateCreated + " dateModified: " + dateModified + " size: " + size;  
    }
}
