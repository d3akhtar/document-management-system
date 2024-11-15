package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import model.Comment;
import ui.MainFrame;
import ui.MainFrame.User;

public class UserRepository {
    private Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    // Normally, we would hash the password, then check with the database, but for now, we are directly checking
    public void login(String email, String password)
    {
        // Try to find a user with given details, if found, set current user in MainFrame, otherwise return
        String loginQuery = 
        "SELECT user_id,username,email from doc_user\r\n" + //
        "WHERE email=" + email + "\r\n" + //
        "AND password_hash=" + password;

        try {
            PreparedStatement statement = connection.prepareStatement(loginQuery);
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                
                MainFrame.window.currentUser = new User(userId, username, email);
            }
        } catch (Exception e){
            System.err.println("An error occured while attempting to login");
            e.printStackTrace();
        }
    }

    public boolean addUser(String username, String email, String password)
    {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT max(user_id) FROM doc_user");
            ResultSet rs = statement.executeQuery();
            if (rs.next()){
                int nextId = rs.getInt("max(user_id)") + 1;
                statement = connection.prepareStatement("INSERT INTO doc_user VALUES (?,?,?,?)");
                statement.setInt(0, nextId);
                statement.setString(1, username);
                statement.setString(2, email);
                statement.setString(3, password);
                return statement.executeUpdate() > 0;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("An error occured while adding a user. The email could've been taken.");
            e.printStackTrace();
            return false;
        }
    }
}
