package ui;

import java.awt.*;
import javax.swing.*;

public class FileExplorer extends JPanel {

    public FileExplorer()
    {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("FileExplorer", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
