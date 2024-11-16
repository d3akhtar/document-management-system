package ui;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;

import javax.swing.*;

import com.mysql.cj.jdbc.Blob;

import model.Document;
import model.FolderContent;
import model.Version;
import repository.DocumentRepository;

public class DocumentEditor extends JPanel {

    private DocumentRepository docRepo;
    private Document document;

    public DocumentEditor(Document document, DocumentRepository docRepo)
    {
        this.docRepo = docRepo;
        this.document = document;

        setLayout(new BorderLayout());

        add(getDocumentEditorMenuBar(), BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        textArea.setTabSize(4);

        add(textArea, BorderLayout.CENTER);

        // Load content from latest version of this document into the textarea
        textArea.setText(docRepo.getLatestFileVersionContent(document.fileId, MainFrame.window.currentUser.userId));
    }

    private JMenuBar getDocumentEditorMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();

        // File options
        JMenu fileMenu = new JMenu("File");
        JMenuItem save = new JMenuItem("Save");
        save.addActionListener(e -> saveFile());
        JMenuItem reload = new JMenuItem("Reload");
        reload.addActionListener(e -> reloadFile());
        fileMenu.add(save);
        fileMenu.add(reload);

        // Collaboration options
        JMenu teamMenu = new JMenu("Collaboration");
        JMenuItem comments = new JMenuItem("Comments");
        comments.addActionListener(e -> showComments());
        JMenuItem permissions = new JMenuItem("Permissions");
        permissions.addActionListener(e -> managePermissions());
        teamMenu.add(comments);
        teamMenu.add(permissions);

        // History options
        JMenu historyMenu = new JMenu("History");
        JMenuItem versions = new JMenuItem("Versions");
        versions.addActionListener(e -> manageVersions());
        historyMenu.add(versions);

        menuBar.add(fileMenu);
        menuBar.add(teamMenu);
        menuBar.add(historyMenu);

        return menuBar;
    }

    public int getDocumentIdOfEditor() { return document.fileId; }

    private void saveFile()
    {
        Version newFileVersion = new Version(
            0, 
            document.fileId, 
            MainFrame.window.currentUser.userId, 
            docRepo.getLatestVersionNumberForDocument(document.fileId) + 1, 
            Timestamp.from(Instant.now()),
            new Blob(textArea.getText().getBytes(),null));

        if (!docRepo.addFileVersion(newFileVersion, textArea.getText().length())) {
            JOptionPane.showMessageDialog(this, "An error occured while attempting to save the file.");
        } 
    }

    private void reloadFile()
    {

    }

    private void showComments()
    {

    }

    private void managePermissions()
    {

    }

    private void manageVersions()
    {
        
    }

    // GUI Components
    JTextArea textArea;
}
