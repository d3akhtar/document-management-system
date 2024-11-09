package ui;

import java.awt.*;
import javax.swing.*;

public class DocumentEditor extends JPanel {

    public DocumentEditor()
    {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("DocumentEditor", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
