package model;

public class Permission {
    
    public int permissionId;
    public Integer fileId;
    public Integer folderId;
    public Integer userId;
    public Integer teamId;
    public int abilities;

    public enum Ability 
    {
        VIEW,
        COMMENT,
        EDIT
    };

    public Permission(int permissionId, Integer fileId, Integer folderId, Integer userId, Integer teamId, int abilities) {
        this.permissionId = permissionId;
        this.fileId = fileId;
        this.folderId = folderId;
        this.userId = userId;
        this.teamId = teamId;
        this.abilities = abilities;
    }

    public Ability getAbilityEnum()
    {
        switch (abilities){
            case 1: return Ability.VIEW;
            case 2: return Ability.COMMENT;
            case 3: return Ability.EDIT;
            default: throw new IllegalArgumentException("No valid enum for abilities value: " + abilities);
        }
    }
}
