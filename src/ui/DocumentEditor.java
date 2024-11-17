package ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import com.mysql.cj.jdbc.Blob;

import model.Comment;
import model.Document;
import model.FolderContent;
import model.Version;
import model.Permission.Ability;
import repository.DocumentRepository;
import repository.UserRepository;

public class DocumentEditor extends JPanel {

    private DocumentRepository docRepo;
    private UserRepository userRepo;
    private Document document;

    private int selectedCommentId;
    private int selectedCommentIdCreatedBy;

    public DocumentEditor(Document document, DocumentRepository docRepo, UserRepository userRepo)
    {
        this.docRepo = docRepo;
        this.userRepo = userRepo;
        this.document = document;

        selectedCommentId = 0;
        selectedCommentIdCreatedBy = 0;

        setLayout(new BorderLayout());

        add(getDocumentEditorMenuBar(), BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        textArea.setTabSize(4);

        add(textArea, BorderLayout.CENTER);

        reloadFile();

        initComponents();
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
        comments.addActionListener(e -> toggleCommentVisibility());
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
        // Run the same code again to load content from latest version of this document
        textArea.setText(docRepo.getLatestFileVersionContent(document.fileId, MainFrame.window.currentUser.userId));
    }

    private void toggleCommentVisibility()
    {
        if (!commentPanel.isVisible()) { loadCommentList(); }
        commentPanel.setVisible(!commentPanel.isVisible());
    }

    private void showCommentUpdateMenu(MouseEvent e)
    {
        commentUpdateMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void managePermissions()
    {

    }

    private void manageVersions()
    {
        
    }

    private void addComment()
    {
        if (
            document.ownerId != MainFrame.window.currentUser.userId && 
            docRepo.getUserPermForDocument(document.fileId, MainFrame.window.currentUser.userId).getAbilityEnum() == Ability.VIEW
            )
        {
            JOptionPane.showMessageDialog(this, "You are not allowed to add comments to this file");
            return;
        }

        String newCommentContent = JOptionPane.showInputDialog(this, "Add comment content");

        if (newCommentContent == null || newCommentContent == "") {
            // Don't do anything if "Cancel" was clicked
            return;
        }

        if (!docRepo.addDocumentComment(new Comment(0, document.fileId, MainFrame.window.currentUser.userId, newCommentContent, Timestamp.from(Instant.now())))) {
            JOptionPane.showMessageDialog(this, "An error occured while trying to add this comment.");
        }

        // Hide, reload, then show the comments to make it look as if the box was updated in real-time
        toggleCommentVisibility();
        loadCommentList();
        toggleCommentVisibility();
    }

    private void editComment()
    {
        if (document.ownerId != MainFrame.window.currentUser.userId && MainFrame.window.currentUser.userId != selectedCommentIdCreatedBy){
            JOptionPane.showMessageDialog(this, "You are not allowed to delete edit comment since you neither own this document or made that comment.");
            return;
        }

        String newCommentContent = JOptionPane.showInputDialog(this, "Edit comment content", docRepo.getCommentContentById(selectedCommentId));

        if (newCommentContent == null || newCommentContent == "") {
            // Don't do anything if "Cancel" was clicked
            return;
        }

        if (!docRepo.updateCommentContent(selectedCommentId, newCommentContent)) {
            JOptionPane.showMessageDialog(this, "An error occured while trying to update this comment.");
        }

        // Hide, reload, then show the comments to make it look as if the box was updated in real-time
        toggleCommentVisibility();
        loadCommentList();
        toggleCommentVisibility();
    }

    private void deleteComment()
    {
        if (document.ownerId != MainFrame.window.currentUser.userId && MainFrame.window.currentUser.userId != selectedCommentIdCreatedBy){
            JOptionPane.showMessageDialog(this, "You are not allowed to delete this comment since you neither own this document or made that comment.");
            return;
        }
        if (!docRepo.deleteDocumentComment(selectedCommentId)){
            JOptionPane.showMessageDialog(this, "An error occured while trying to delete this comment.");
        }

        // Hide, reload, then show the comments to make it look as if the box was updated in real-time
        toggleCommentVisibility();
        loadCommentList();
        toggleCommentVisibility();
    }

    private void loadCommentList()
    {
        commentListPanel.removeAll();

        ArrayList<Comment> comments = docRepo.getFileComments(document.fileId);

        for (Comment comment : comments) {
            JTextArea commentTextArea = new JTextArea(userRepo.getUsernameById(comment.createdBy) + ":\n\n" + comment.content);
            commentTextArea.setLineWrap(true);
            commentTextArea.setColumns(10);
            commentTextArea.setRows(1);
            commentTextArea.setEditable(false);
            commentTextArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

            // We need the id to reference when editing or deleting comment
            commentTextArea.putClientProperty("commentId", comment.commentId);
            // We also need to add a referene to the user id of the person who created the comment
            commentTextArea.putClientProperty("createdBy", comment.createdBy);

            commentTextArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedCommentId = (int) commentTextArea.getClientProperty("commentId");
                    selectedCommentIdCreatedBy = (int) commentTextArea.getClientProperty("createdBy");
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        // If right-clicked, show a popup menu with the options to edit or delete a comment
                        showCommentUpdateMenu(e);
                    }
                }
            });

            commentListPanel.add(commentTextArea);
        }

        // Add a vertical filler to consume extra space if needed
        commentListPanel.add(Box.createVerticalGlue());

        commentListPanel.revalidate();
        commentListPanel.repaint();
    }

    private void initComponents()
    {
        // Create JPanel to show list of comments
        commentPanel = new JPanel(new BorderLayout());
        commentListPanel = new JPanel();
        commentListPanel.setLayout(new BoxLayout(commentListPanel, BoxLayout.Y_AXIS));

        commentScrollPane = new JScrollPane(commentListPanel);
        commentScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel inputPanel = new JPanel(new BorderLayout());
        JButton addCommentButton = new JButton("Add Comment");
        addCommentButton.setBorder(BorderFactory.createLineBorder(Color.black));
        addCommentButton.addActionListener(e -> addComment());
        inputPanel.add(addCommentButton, BorderLayout.SOUTH);

        commentPanel.add(commentScrollPane, BorderLayout.CENTER);
        commentPanel.add(inputPanel, BorderLayout.SOUTH);

        loadCommentList();

        add(commentPanel, BorderLayout.WEST);
        toggleCommentVisibility();

        // Create comment update popup menu
        commentUpdateMenu = new JPopupMenu();
        JMenuItem editComment = new JMenuItem("Edit");
        editComment.addActionListener(e -> editComment());
        JMenuItem deleteComment = new JMenuItem("Delete");
        deleteComment.addActionListener(e -> deleteComment());
        commentUpdateMenu.add(editComment);
        commentUpdateMenu.add(deleteComment);
    }

    // GUI Components
    JTextArea textArea;
    JPanel commentPanel;
    JPanel commentListPanel;
    JScrollPane commentScrollPane;
    JPopupMenu commentUpdateMenu;
}
