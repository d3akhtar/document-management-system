package ui;

import java.awt.*;
import javax.swing.*;

import repository.TeamRepository;

public class TeamExplorer extends JPanel {
    
    private TeamRepository teamRepo;

    public TeamExplorer(TeamRepository teamRepo)
    {
        this.teamRepo = teamRepo;
        
        setLayout(new BorderLayout());
        JLabel label = new JLabel("TeamExplorer", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }

}
