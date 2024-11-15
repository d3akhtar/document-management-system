package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import model.Team;
import ui.MainFrame;
import ui.MainFrame.User;

public class TeamRepository {
    private Connection connection;

    public TeamRepository(Connection connection) {
        this.connection = connection;
    }

    public ArrayList<Team> getTeamsThatUserIsIn(int userId)
    {
        ArrayList<Team> teams = new ArrayList<Team>();
        String teamsQuery =
        "SELECT t.team_id,owner_id,team_name,team_description FROM team t\r\n" + //
        "JOIN user_team ut ON ut.user_id=" + Integer.toString(userId) + " AND ut.team_id=t.team_id";

        try {
            PreparedStatement statement = connection.prepareStatement(teamsQuery);
            ResultSet rs = statement.executeQuery();
            while (rs.next()){
                int teamId = rs.getInt("team_id");
                int ownerId = rs.getInt("owner_id");
                String teamName = rs.getString("team_name");
                String teamDescription = rs.getString("team_description");
                teams.add(new Team(teamId, ownerId, teamName, teamDescription));    
            }
        } catch (Exception e){
            System.err.println("An error occured while getting teams that user with id: " + userId + " is a part of");
            e.printStackTrace();
        }

        return teams;
    }

    public boolean addTeam(Team team)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(team_id) FROM team");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(team_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO team VALUES (?,?,?,?)");
                statement.setInt(1, nextId);
                statement.setInt(2, team.ownerId);
                statement.setString(3, team.teamName);
                statement.setString(4, team.teamDescription);
                return statement.executeUpdate() > 0;
            }
            else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occured while adding a team.");
            e.printStackTrace();
            return false;
        }
    }

    public boolean addMemberToTeam(User user, Team team)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(user_team_id) FROM user_team");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(user_team_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO user_team VALUES (?,?,?)");
                statement.setInt(1, nextId);
                statement.setInt(2, team.teamId);
                statement.setInt(3, user.userId);
                return statement.executeUpdate() > 0;
            }
            else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occured while adding user: " + user.username + " to team: " + team.teamName);
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeMemberFromTeam(User user, Team team)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM user_team WHERE user_id=? AND team_id=?");
            statement.setInt(1, user.userId);
            statement.setInt(2, team.teamId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while removing user: " + user.username + " from team: " + team.teamName);
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeTeam(int teamId)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM team WHERE team_id=" + teamId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("An error occured while removing team with id: " + teamId);
            e.printStackTrace();
            return false;
        }
    }
}
