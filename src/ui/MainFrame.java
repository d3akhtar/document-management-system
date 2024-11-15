package ui;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.*;

import repository.DocumentRepository;
import repository.TeamRepository;

public class MainFrame extends JFrame
{
    public static MainFrame window;
    private Connection connection = null;

    public static class User
    {
        public int userId;
        public String username;
        public String email;

        public User(int userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;
        }
    }

    public User currentUser;
    
    public MainFrame()
    {
        attemptConnection();

        window = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1280, 720);

        if (connection != null){

            initComponents();

            tabbedPane.add("File Explorer", new FileExplorer(new DocumentRepository(connection)));
            tabbedPane.add("Document Editor", new DocumentEditor(new DocumentRepository(connection)));
            tabbedPane.add("Team Explorer", new TeamExplorer(new TeamRepository(connection)));

            setContentPane(tabbedPane);
        }
    }

    private void initComponents()
    {
        tabbedPane = new JTabbedPane();
    }

    private void attemptConnection()
    {
        JDialog dialog = new JDialog();
        dialog.setTitle("Connection Status");
        dialog.setSize(300, 150);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JLabel messageLabel = new JLabel("Connecting to database...", JLabel.CENTER);
        dialog.add(messageLabel, BorderLayout.CENTER);

        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        try{
            // Attempt connection
            String url = "jdbc:mysql://localhost:3306/documents";
            String userame = "root";
            String password = "";
            connection = DriverManager.getConnection(url, userame, password);

            messageLabel.setText("Connected successfully!");
            Thread.sleep(1000);
        }
        catch(Exception e){
            dialog.dispose();
            JOptionPane.showConfirmDialog(null, "The connection to the database failed. Click to close application", "Connection Status", JOptionPane.CLOSED_OPTION);
        }

        dialog.dispose();
    }

    // GUI Components
    private JTabbedPane tabbedPane;

}