package ui;

import java.awt.*;
import javax.swing.*;

import repository.DocumentRepository;

public class FileExplorer extends JPanel {

    private DocumentRepository docRepo;

    public FileExplorer(DocumentRepository docRepo)
    {
        this.docRepo = docRepo;
        
        setLayout(new BorderLayout());
        JLabel label = new JLabel("FileExplorer", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
