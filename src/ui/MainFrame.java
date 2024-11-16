package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.Border;

import model.Document;
import repository.DocumentRepository;
import repository.TeamRepository;
import repository.UserRepository;

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

    // For keeping track of document editor instances. Entries refer to document ids
    private ArrayList<Integer> documentEditorInstances;
    
    public MainFrame()
    {
        currentUser = null;
        attemptConnection();
        documentEditorInstances = new ArrayList<Integer>();

        window = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1280, 720);

        if (connection != null){
            while (currentUser == null)
            {
                LoginDialog loginDialog = new LoginDialog(this, new UserRepository(connection));
                loginDialog.setVisible(true);

                if (currentUser == null){
                    int option = JOptionPane.showOptionDialog(
                        this, 
                        "You haven't logged in or signed up. Close app, or try again?", 
                        "Authentication Status", 
                        0, 
                        1, 
                        null, 
                        new String[] { "Close", "Try Again"}, 
                        null);
                    
                    if (option == 0) {
                        System.exit(0);
                    }
                }
            }

            initComponents();

            tabbedPane.add("File Explorer", new FileExplorer(new DocumentRepository(connection)));
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

    public void addDocumentEditorTab(Document document)
    {
        // Check if an instance of DocumentEditor exists with this document. If so, focus on the tab, otherwise add a new tab
        for (int i = 0; i < documentEditorInstances.size(); i++)
        {
            int documentId = documentEditorInstances.get(i);
            if (documentId == document.fileId) {
                tabbedPane.setSelectedIndex(i + 2); // index 0 and 1 have the FileExplorer and TeamExplorer tabs, so we need to offset by 2
                return;
            }
        }
        

        JPanel tabContent = new DocumentEditor(document, new DocumentRepository(connection));
        tabbedPane.addTab(document.fileName, tabContent);

        // Add a tab with a close button
        int index = tabbedPane.indexOfComponent(tabContent);
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        tabPanel.setOpaque(false);
        documentEditorInstances.add(document.fileId);

        tabPanel.add(new JLabel(document.fileName));

        // Create the close button for the tab
        JButton tabCloseButton = new JButton("ⓧ");
        tabCloseButton.setFont(new Font(getFont().getName(), Font.BOLD, 15));
        tabCloseButton.setFocusable(false);
        tabCloseButton.setContentAreaFilled(false);
        tabCloseButton.setMargin(new Insets(0, 0, 0, 0));
        tabCloseButton.setBorder(BorderFactory.createEmptyBorder());
        tabCloseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                documentEditorInstances.remove(index - 2);
                tabbedPane.remove(tabContent);
            }
        });

        tabPanel.add(Box.createHorizontalStrut(5)); // Add some space between the tab name and the X button
        tabPanel.add(tabCloseButton);
        
        // Show tab on the actual JTabbedPane object in this class
        tabbedPane.setTabComponentAt(index, tabPanel);

        // After opening the tab, select it
        tabbedPane.setSelectedIndex(index);
    }

    // GUI Components
    private JTabbedPane tabbedPane;

}