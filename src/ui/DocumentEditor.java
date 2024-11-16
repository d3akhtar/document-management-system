package ui;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

import model.Document;
import model.FolderContent;
import repository.DocumentRepository;

public class DocumentEditor extends JPanel {

    private DocumentRepository docRepo;
    private Document document;

    public DocumentEditor(Document document, DocumentRepository docRepo)
    {
        this.docRepo = docRepo;

        setLayout(new BorderLayout());
        JLabel label = new JLabel("DocumentEditor, name: " + document.fileName, SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }

    public int getDocumentIdOfEditor() { return document.fileId; }
}
