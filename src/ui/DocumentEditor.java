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
import model.Permission;
import model.Version;
import model.Permission.Ability;
import repository.DocumentRepository;
import repository.UserRepository;
import ui.MainFrame.User;

public class DocumentEditor extends JPanel {

    // We need both document and user related info, since we will also be dealing with giving permissions to user on the document assigned to this instance of DocumentEditor
    private DocumentRepository docRepo;
    private UserRepository userRepo;
    
    private Document document;

    // Have global variables for selected ids to refer to when adding, deleting, updating, etc...
    private int selectedCommentId;
    private int selectedCommentIdCreatedBy;
    private int selectedPermissionId;
    private int selectedVersionId;
    private int selectedVersionNumber;

    public DocumentEditor(Document document, DocumentRepository docRepo, UserRepository userRepo)
    {
        this.docRepo = docRepo;
        this.userRepo = userRepo;
        this.document = document;

        selectedCommentId = 0;
        selectedCommentIdCreatedBy = 0;
        selectedPermissionId = 0;
        selectedVersionId = 0;
        selectedVersionNumber = 0;

        setLayout(new BorderLayout());

        add(getDocumentEditorMenuBar(), BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        textArea.setTabSize(4);

        // Only allow edits if the user that is logged in has edit permission
        textArea.setEditable(docRepo.getUserPermForDocument(document.fileId, MainFrame.window.currentUser.userId).getAbilityEnum() == Ability.EDIT);

        add(textArea, BorderLayout.CENTER);

        // After we add the text area, put the content of the latest version of this document in that text area
        reloadFile();

        // Create other GUI components that will be used when adding/updating comments or permissions
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
        permissions.addActionListener(e -> togglePermissionVisibility());
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
        // Only allow ability to save file to users with edit permission
        if (docRepo.getUserPermForDocument(document.fileId, MainFrame.window.currentUser.userId).getAbilityEnum() != Ability.EDIT){
            JOptionPane.showMessageDialog(this, "Cannot save file if you don't have edit permission.");
            return;
        }

        // The way we implement saving is to add a new version for the document
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
        // Load content from latest version of this document
        textArea.setText(docRepo.getLatestFileVersionContent(document.fileId, MainFrame.window.currentUser.userId));
    }

    private void toggleCommentVisibility()
    {
        if (!commentPanel.isVisible()) { loadCommentList(); }
        commentPanel.setVisible(!commentPanel.isVisible());
    }

    private void togglePermissionVisibility()
    {
        if (!permissionPanel.isVisible()) { loadPermissionList(); }
        permissionPanel.setVisible(!permissionPanel.isVisible());
    }

    private void showCommentUpdateMenu(MouseEvent e)
    {
        commentUpdateMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showPermissionUpdateMenu(MouseEvent e)
    {
        permissionUpdateMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showVersionUpdateMenu(MouseEvent e)
    {
        versionUpdateMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    // Show a dialog where a user can manage document versions, and can maybe go to a previous version
    private void manageVersions()
    {
        loadVersionList();

        versionDialogPanel = new JPanel();
        versionDialogPanel.setLayout(new BoxLayout(versionDialogPanel, BoxLayout.Y_AXIS));
        versionDialogPanel.setMinimumSize(new Dimension(854, 480));
        versionDialogPanel.setPreferredSize(new Dimension(854, 480));

        JScrollPane scrollPane = new JScrollPane(versionListPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        versionDialogPanel.add(scrollPane);
        versionDialogPanel.add(versionContentTextArea);

        int result = JOptionPane.showConfirmDialog(
            this, versionDialogPanel, "Select Version", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION){
            // Switch to a previous version of the document by adding another version of the document with the same content
            String versionContent = docRepo.getVersionContentById(selectedVersionId);
            
            Version newFileVersion = new Version(
            0, 
            document.fileId, 
            MainFrame.window.currentUser.userId, 
            docRepo.getLatestVersionNumberForDocument(document.fileId) + 1, 
            Timestamp.from(Instant.now()),
            new Blob(versionContent.getBytes(),null));

            if (!docRepo.addFileVersion(newFileVersion, versionContent.length())){
                JOptionPane.showMessageDialog(this, "Something went wrong went switching to previous document version.");
            } else {
                textArea.setText(versionContent); // Onlyupdate text in editor on success
            }
        }
    }

    // Show a dialog where a user can add a comment
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

    // Show a dialog where a user can edit a comment
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

    // Show a dialog where a user can add a permission for a user based off their email
    private void addPermission()
    {
        if (document.ownerId != MainFrame.window.currentUser.userId){
            JOptionPane.showMessageDialog(this, "Cannot add permissions if you aren't the document owner.");
            return;
        }

        JPanel permissionInputDialogPanel = new JPanel();
        permissionInputDialogPanel.setLayout(new BoxLayout(permissionInputDialogPanel, BoxLayout.Y_AXIS));

        // Specify who the permission is for
        JTextField permissionUserEmailField = new JTextField();
        permissionInputDialogPanel.add(new JLabel("Enter user email: "));
        permissionInputDialogPanel.add(permissionUserEmailField);

        // Specify the ability
        JComboBox<String> permissionAbilityOptions = new JComboBox<>(new String[] {"View", "Comment", "Edit"});
        permissionInputDialogPanel.add(new JLabel("Select ability:"));
        permissionInputDialogPanel.add(permissionAbilityOptions);

        int result = JOptionPane.showConfirmDialog(
            this, permissionInputDialogPanel, "Add Permission", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION){
            String email = permissionUserEmailField.getText();
            int abilities = permissionAbilityOptions.getSelectedIndex() + 1;

            User user = userRepo.getUserWithEmail(email);

            if (user == null) {
                JOptionPane.showMessageDialog(this, "Couldn't find a user with this email.");
                return;
            }

            if (docRepo.getUserPermForDocument(document.fileId, user.userId) != null) {
                JOptionPane.showMessageDialog(this, "User already has a permission on this document.");
                return;
            }

            if (!docRepo.addUserPermForItem(new Permission(0, document.fileId, null, user.userId, null, abilities))){
                JOptionPane.showMessageDialog(this, "Something went wrong when adding permission.");
            }

            // Hide, reload, then show the permission to make it look as if the box was updated in real-time
            togglePermissionVisibility();
            loadPermissionList();
            togglePermissionVisibility();
        }
    }

    // Show a dialog where a user can edit the abilities of a permission for a user
    private void editPermission()
    {
        if (document.ownerId != MainFrame.window.currentUser.userId){
            JOptionPane.showMessageDialog(this, "Cannot edit permissions if you aren't the document owner.");
            return;
        }

        Permission permission = docRepo.getPermissionById(selectedPermissionId);

        JPanel permissionInputDialogPanel = new JPanel();
        permissionInputDialogPanel.setLayout(new BoxLayout(permissionInputDialogPanel, BoxLayout.Y_AXIS));

        // Don't need to specify who the permission is for, but still show the email associated with the permission
        JTextField permissionUserEmailField = new JTextField();
        permissionInputDialogPanel.add(new JLabel("Enter user email: "));
        permissionInputDialogPanel.add(permissionUserEmailField);
        permissionUserEmailField.setText(userRepo.getUserEmailById(permission.userId));
        permissionUserEmailField.setEditable(false);

        // Specify the ability
        JComboBox<String> permissionAbilityOptions = new JComboBox<>(new String[] {"View", "Comment", "Edit"});
        permissionInputDialogPanel.add(new JLabel("Select ability:"));
        permissionInputDialogPanel.add(permissionAbilityOptions);
        permissionAbilityOptions.setSelectedIndex(permission.abilities - 1);

        int result = JOptionPane.showConfirmDialog(
            this, permissionInputDialogPanel, "Update Permission", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION){
            int abilities = permissionAbilityOptions.getSelectedIndex() + 1;

            if (!docRepo.updateUserPermForDocument(new Permission(permission.permissionId, permission.fileId, permission.folderId, permission.userId, permission.teamId, abilities))){
                JOptionPane.showMessageDialog(this, "Something went wrong when updating permission.");
            }

            // Hide, reload, then show the permission to make it look as if the box was updated in real-time
            togglePermissionVisibility();
            loadPermissionList();
            togglePermissionVisibility();
        }
    }

    private void deletePermission()
    {
        if (document.ownerId != MainFrame.window.currentUser.userId){
            JOptionPane.showMessageDialog(this, "Cannot delete permissions if you aren't the document owner.");
            return;
        }

        if (docRepo.getPermissionById(selectedPermissionId).userId == document.ownerId) {
            JOptionPane.showMessageDialog(this, "Cannot delete the permission that refers to the owner.");
            return;
        }

        if (!docRepo.removeUserPermForDocument(selectedPermissionId)){
            JOptionPane.showMessageDialog(this, "Something went wrong when deleting permission.");
        }

        // Hide, reload, then show the permission to make it look as if the box was updated in real-time
        togglePermissionVisibility();
        loadPermissionList();
        togglePermissionVisibility();
    }

    // Refresh comment list, which is useful to show updates when we add or delete comments
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

    // Refresh permission list, which is useful to show updates when we add or delete permissions
    private void loadPermissionList()
    {
        permissionListPanel.removeAll();

        ArrayList<Permission> permissions = docRepo.getDocumentPermissions(document.fileId);

        for (Permission permission : permissions) {
            String permissionDisplay =
            "Username: " + userRepo.getUsernameById(permission.userId) + "\n" +
            "Ability: " + permission.getAbilityString(); 

            JTextArea permissionTextArea = new JTextArea(permissionDisplay);
            permissionTextArea.setLineWrap(true);
            permissionTextArea.setColumns(10);
            permissionTextArea.setRows(1);
            permissionTextArea.setEditable(false);
            permissionTextArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

            // We need the id to reference when editing or deleting permission
            permissionTextArea.putClientProperty("permissionId", permission.permissionId);
            // We also need to add a reference to the user id of the person who created the permission

            permissionTextArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedPermissionId = (int) permissionTextArea.getClientProperty("permissionId");
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        // If right-clicked, show a popup menu with the options to edit or delete a permission
                        showPermissionUpdateMenu(e);
                    }
                }
            });

            permissionListPanel.add(permissionTextArea);
        }

        // Add a vertical filler to consume extra space if needed
        permissionListPanel.add(Box.createVerticalGlue());

        permissionListPanel.revalidate();
        permissionListPanel.repaint();
    }

    // Refresh version list, which is useful to show updates when we add or delete versions
    private void loadVersionList()
    {
        versionListPanel.removeAll();

        for (Version v : docRepo.getAllVersionsForDocument(document.fileId)){
            JPanel versionLabelPanel = new JPanel(new BorderLayout());
            JLabel versionLabel = new JLabel(v.dateModified.toString());
            
            versionLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            versionLabel.setFont(new Font("Serif", Font.PLAIN, 20));
            versionLabel.setFocusable(true);

            versionLabelPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            versionLabelPanel.add(versionLabel, BorderLayout.CENTER);
            versionLabel.setHorizontalAlignment(SwingConstants.CENTER);

            versionLabelPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Save versionId for reference if user wants to delete the version
                    selectedVersionId = v.versionId;
                    selectedVersionNumber = v.versionNumber;
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        versionContentTextArea.setText(docRepo.getVersionContentById(v.versionId));
                    }
                    else if (e.getButton() == MouseEvent.BUTTON3){
                        // If right clicked, show popup menu with option to delete version
                        showVersionUpdateMenu(e);
                    }
                }
            });
            
            versionListPanel.add(versionLabelPanel);
        }
    }

    private void deleteVersion()
    {
        if (document.ownerId != MainFrame.window.currentUser.userId){
            JOptionPane.showMessageDialog(this, "Cannot delete versions if you aren't the document owner.");
            return;
        }

        if (selectedVersionNumber == docRepo.getLatestVersionNumberForDocument(document.fileId)){
            JOptionPane.showMessageDialog(this, "Cannot delete the latest version of a document.");
            return;
        }

        if (!docRepo.deleteDocumentVersion(selectedVersionId)){
            JOptionPane.showMessageDialog(this, "Something went wrong while attempting to delete version.");
        }

        loadVersionList();
        versionDialogPanel.revalidate();
        versionDialogPanel.repaint();
    }

    private void initComponents()
    {
        // Create JPanel to show list of comments
        commentPanel = new JPanel(new BorderLayout());
        commentListPanel = new JPanel();
        commentListPanel.setLayout(new BoxLayout(commentListPanel, BoxLayout.Y_AXIS));

        commentScrollPane = new JScrollPane(commentListPanel);
        commentScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel commentInputPanel = new JPanel(new BorderLayout());
        JButton addCommentButton = new JButton("Add Comment");
        addCommentButton.setBorder(BorderFactory.createLineBorder(Color.black));
        addCommentButton.addActionListener(e -> addComment());
        commentInputPanel.add(addCommentButton, BorderLayout.SOUTH);

        commentPanel.add(commentScrollPane, BorderLayout.CENTER);
        commentPanel.add(commentInputPanel, BorderLayout.SOUTH);

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

        // Create JPanel to show list of permissions
        permissionPanel = new JPanel(new BorderLayout());
        permissionListPanel = new JPanel();
        permissionListPanel.setLayout(new BoxLayout(permissionListPanel, BoxLayout.Y_AXIS));

        permissionScrollPane = new JScrollPane(permissionListPanel);
        permissionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel permissionInputPanel = new JPanel(new BorderLayout());
        JButton addPermissionButton = new JButton("Add Permission");
        addPermissionButton.setBorder(BorderFactory.createLineBorder(Color.black));
        addPermissionButton.addActionListener(e -> addPermission());
        permissionInputPanel.add(addPermissionButton, BorderLayout.SOUTH);

        permissionPanel.add(permissionScrollPane, BorderLayout.CENTER);
        permissionPanel.add(permissionInputPanel, BorderLayout.SOUTH);

        loadPermissionList();

        add(permissionPanel, BorderLayout.EAST);
        togglePermissionVisibility();

        // Create permission update popup menu
        permissionUpdateMenu = new JPopupMenu();
        JMenuItem editPermission = new JMenuItem("Edit");
        editPermission.addActionListener(e -> editPermission());
        JMenuItem deletePermission = new JMenuItem("Delete");
        deletePermission.addActionListener(e -> deletePermission());
        permissionUpdateMenu.add(editPermission);
        permissionUpdateMenu.add(deletePermission);

        // Create popup menu to update versions (there is only a delete option for now)
        versionUpdateMenu = new JPopupMenu();
        JMenuItem deleteVersion = new JMenuItem("Delete");
        deleteVersion.addActionListener(e -> deleteVersion());
        versionUpdateMenu.add(deleteVersion);

        // Prepare version list panel for loading
        versionListPanel = new JPanel();
        versionListPanel.setLayout(new BoxLayout(versionListPanel, BoxLayout.Y_AXIS));

        // Create version content text area so versionListPanel can refer to it
        versionContentTextArea = new JTextArea();
        versionContentTextArea.setLineWrap(true);
        versionContentTextArea.setEditable(false);
        versionContentTextArea.setFocusable(false);
    }

    // GUI Components
    JTextArea textArea;

    JPanel commentPanel;
    JPanel commentListPanel;
    JScrollPane commentScrollPane;
    JPopupMenu commentUpdateMenu;

    JPanel permissionPanel;
    JPanel permissionListPanel;
    JScrollPane permissionScrollPane;
    JPopupMenu permissionUpdateMenu;

    JPopupMenu versionUpdateMenu;
    JPanel versionListPanel;
    JTextArea versionContentTextArea;
    JPanel versionDialogPanel;
}
