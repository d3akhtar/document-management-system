package ui;

import java.awt.*;
import javax.swing.*;

public class LoginScreen extends JPanel {
    
    public LoginScreen()
    {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("LoginScreen", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
