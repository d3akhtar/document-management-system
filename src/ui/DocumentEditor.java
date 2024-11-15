package ui;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

import model.Document;
import model.FolderContent;
import repository.DocumentRepository;

public class DocumentEditor extends JPanel {

    private DocumentRepository docRepo;

    public DocumentEditor(DocumentRepository docRepo)
    {
        this.docRepo = docRepo;

        setLayout(new BorderLayout());
        JLabel label = new JLabel("DocumentEditor", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
