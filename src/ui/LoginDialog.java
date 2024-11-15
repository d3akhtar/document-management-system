package ui;

import java.awt.*;
import javax.swing.*;

import repository.UserRepository;

public class LoginDialog extends JDialog {
    
    private UserRepository userRepo;

    public LoginDialog(JFrame parent, UserRepository userRepo)
    {
        super(parent, "Login", true);

        JTabbedPane tabbedPane = new JTabbedPane();

    }

    private JPanel loginSection()
    {
        JPanel loginPanel = new JPanel();

        

        return loginPanel;
    }

    private JPanel signUpPanel()
    {
        JPanel signUpPanel = new JPanel();



        return signUpPanel;
    }
}
