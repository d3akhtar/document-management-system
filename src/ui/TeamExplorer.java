package ui;

import java.awt.*;
import javax.swing.*;

public class TeamExplorer extends JPanel {
    
    public TeamExplorer()
    {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("TeamExplorer", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }

}
