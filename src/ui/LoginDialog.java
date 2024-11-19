package ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;

import repository.UserRepository;

public class LoginDialog extends JDialog {
    
    // We only need user related info for anything related to logging in
    private UserRepository userRepo;

    public LoginDialog(JFrame parent, UserRepository userRepo)
    {
        super(parent, "Login", true);
        this.userRepo = userRepo;

        setSize(500, 200);
        setLocationRelativeTo(parent);

        // Add a tab for logging in or signing up
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Log In", loginSection());
        tabbedPane.addTab("Sign Up", signUpPanel());

        setContentPane(tabbedPane);
    }

    // Return a JPanel where a user can enter login info
    private JPanel loginSection()
    {
        JPanel loginPanel = new JPanel();
        JPanel inputPanel = new JPanel();
        
        loginPanel.setLayout(new BorderLayout());
        inputPanel.setLayout(new GridLayout(3,2));
        
        inputPanel.add(new JLabel("Email"));
        loginEmailField = new JTextField();
        inputPanel.add(loginEmailField);

        inputPanel.add(new JLabel("Password"));
        loginPasswordField = new JPasswordField();
        inputPanel.add(loginPasswordField);

        JButton loginButon = new JButton("Login");
        loginButon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });

        loginPanel.add(inputPanel, BorderLayout.CENTER);
        loginPanel.add(loginButon, BorderLayout.SOUTH);

        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        return loginPanel;
    }

    // Return a JPanel where a user can enter sign up info
    private JPanel signUpPanel()
    {
        JPanel signUpPanel = new JPanel();
        JPanel inputPanel = new JPanel();
        
        signUpPanel.setLayout(new BorderLayout());
        inputPanel.setLayout(new GridLayout(4,2));
        
        inputPanel.add(new JLabel("Username"));
        signUpUsernameField = new JTextField();
        inputPanel.add(signUpUsernameField);
        
        inputPanel.add(new JLabel("Email"));
        signUpEmailField = new JTextField();
        inputPanel.add(signUpEmailField);

        inputPanel.add(new JLabel("Password"));
        signUpPasswordField = new JPasswordField();
        inputPanel.add(signUpPasswordField);

        JButton signUpButton = new JButton("Sign Up");
        signUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                signUp();
            }
        });

        signUpPanel.add(inputPanel, BorderLayout.CENTER);
        signUpPanel.add(signUpButton, BorderLayout.SOUTH);

        signUpPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        return signUpPanel;
    }

    // Attempt a login. On failure, notify the user with a dialog
    private void login()
    {
        String email = loginEmailField.getText();
        String password = new String(loginPasswordField.getPassword());

        boolean success = userRepo.login(email, password);
        if (success){
            dispose();
        } else {
            showErrDialog("Invalid login. Please try again.");
        }
    }

    // Attempt to signup. On failure, notify the user with a dialog about why the failure might have happened (possible duplicate email)
    private void signUp()
    {
        boolean success = userRepo.addUser(signUpUsernameField.getText(), signUpEmailField.getText(), new String(signUpPasswordField.getPassword()));
        if (success){
            dispose();
        } else {
            showErrDialog("Invalid credentials. The email may have been taken. Please try again.");
        }
    }

    // Add a method so it for showing a dialog so "this" refers to the right object
    private void showErrDialog(String msg)
    {
        JOptionPane.showMessageDialog(this, msg);
    }

    // GUI Components
    private JTextField loginEmailField;
    private JPasswordField loginPasswordField;
    private JTextField signUpUsernameField;
    private JTextField signUpEmailField;
    private JPasswordField signUpPasswordField;
}
