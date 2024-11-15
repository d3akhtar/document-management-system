package model;

public class Team {
    public int teamId;
    public int ownerId;
    public String teamName;
    public String teamDescription;

    public Team(int teamId, int ownerId, String teamName, String teamDescription) {
        this.teamId = teamId;
        this.ownerId = ownerId;
        this.teamName = teamName;
        this.teamDescription = teamDescription;
    }

}
