package ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import model.Document;
import model.Folder;
import model.FolderContent;
import repository.DocumentRepository;

public class FileExplorer extends JPanel {

    // Explicitly define a table model for folder contents 
    class FolderContentTableModel extends AbstractTableModel
    {
        private String[] columnNames = {"Name", "Date Created", "Date Modified", "Type", "Size (kb)"};
        private ArrayList<FolderContent> folderContents;
        
        public FolderContentTableModel(ArrayList<FolderContent> folderContents) {
            this.folderContents = folderContents;
        }

        @Override
        public int getRowCount() {
            return folderContents.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        public void addRow(FolderContent fc) { 
            folderContents.add(fc); 
            fireTableRowsInserted(getRowCount()-1, getRowCount());
        }
        public void clear() { 
            int prevRowCount = getRowCount();
            folderContents.clear(); 
            fireTableRowsDeleted(0, prevRowCount);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            FolderContent fc = folderContents.get(rowIndex);
            switch (columnIndex) {
                case 0: return fc.name;
                case 1: return fc.dateCreated;
                case 2: return fc.dateModified;
                case 3: return fc.type;
                case 4: return fc.size == -1 ? "":fc.size;
                default: return null;
            }
        }

        public int getFolderContentIdForRow(int rowIndex)
        {
            return folderContents.get(rowIndex).id;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
    }

    private String searchQuery;
    private FolderContentTableModel folderContentsTableModel;

    private DocumentRepository docRepo;
    
    /*
     * Keep track of the current parent folder id, which is what we will update tables based off of.
     * One thing to note is that when the value for this variable is 0, it will retrive all the files
     * and folders who have parent_folder_id=NULL rather than 0.
     */
    private int currentParentFolderId;
    
    // For undoing and redoing
    private Stack<Integer> parentFolderIdUndoHistory;
    private Stack<Integer> parentFolderIdRedoHistory;
    
    // Keep currentPathField reference to change every update
    private JTextField currentPathField;

    // Keep track of right-clicked item to refer to it when updating
    private int folderContentId;
    private boolean isDir;

    // Keep track of update folder content popup menu to show it when we want without creating a new one
    private JPopupMenu updateFolderContentPopupMenu;

    public FileExplorer(DocumentRepository docRepo)
    {
        folderContentId = 0;
        isDir = true;

        currentParentFolderId = 0;
        parentFolderIdUndoHistory = new Stack<Integer>();
        parentFolderIdRedoHistory = new Stack<Integer>();

        updateFolderContentPopupMenu = getUpdateFolderContentDropdownMenu();

        this.docRepo = docRepo;

        folderContentsTableModel = new FolderContentTableModel(new ArrayList<FolderContent>() {}){
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };

        searchQuery = "";
        // Load the initial table content
        updateModel(searchQuery);

        JTable directoryContentsTable = initTable();

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0,0,0,0);
        
        gbc.gridx = 0;
        gbc.gridy = 0;

        add(createFileExplorerMenu(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;

        add(new JScrollPane(directoryContentsTable), gbc);
    }

    // Create the JTable instance that will use our explicitly defined table model
    private JTable initTable()
    {
        JTable table = new JTable(folderContentsTableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(0).setPreferredWidth(400);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(5);
        table.getColumnModel().getColumn(4).setPreferredWidth(5);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Striped rows
                if (row % 2 == 0){
                    component.setBackground(new Color(250,250,250));
                }
                else{
                    component.setBackground(new Color(240, 240, 240));
                }
                
                return component;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(70,70,70));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 45));

        table.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            int row = table.rowAtPoint(e.getPoint()); // Get the row that was clicked
            if (row >= 0) {
                String type = table.getValueAt(row, 3).toString();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // If the row was left-clicked, run this

                    // If a directory was clicked, go into the directory, otherwise, open the file
                    if (type.equals("dir")){
                        // Go into directory and reset the redo history
                        parentFolderIdRedoHistory.clear();
                        parentFolderIdUndoHistory.push(folderContentsTableModel.getFolderContentIdForRow(row));
                        currentParentFolderId = parentFolderIdUndoHistory.peek();
                        updateModel(searchQuery);
                    }
                    else{
                        // Open the document, aka create a new instance of DocumentEditor with this Document
                        Document document = docRepo.getDocumentById(
                            folderContentsTableModel.getFolderContentIdForRow(row), 
                            MainFrame.window.currentUser.userId);

                        if (document == null) {
                            showErrDialog("The document couldn't be found in the database.");
                        } else {
                            MainFrame.window.addDocumentEditorTab(document);
                        }
                    }
                } 
                else if (e.getButton() == MouseEvent.BUTTON3) {
                    // If the row was right-clicked, run this
                    isDir = type.equals("dir");
                    folderContentId = folderContentsTableModel.getFolderContentIdForRow(row);

                    showUpdateFolderContentMenu(e);
                }
            }
          } 
        });

        return table;
    }

    private JPanel createFileExplorerMenu()
    {
        JPanel menu = new JPanel();
        menu.setLayout(new FlowLayout());
        menu.setBackground(new Color(200, 200, 200));

        // Back button
        JButton backButton = createButtonWithImagePath("/images/backButton.png");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!parentFolderIdUndoHistory.empty()) {
                    parentFolderIdRedoHistory.push(parentFolderIdUndoHistory.pop());
                    currentParentFolderId = parentFolderIdUndoHistory.empty() ? 0:parentFolderIdUndoHistory.peek();
                    updateModel(searchQuery);
                }
            }
        });
        backButton.setToolTipText("Go to previous directory.");

        // Forward button
        JButton forwardButton = createButtonWithImagePath("/images/forwardButton.png");
        forwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!parentFolderIdRedoHistory.empty()) {
                    parentFolderIdUndoHistory.push(parentFolderIdRedoHistory.pop());
                    currentParentFolderId = parentFolderIdUndoHistory.peek();
                    updateModel(searchQuery);
                }
            }
        });
        forwardButton.setToolTipText("Go down a directory you have been through");

        // Current path field
        currentPathField = new JTextField(){
            @Override
            public Insets getInsets() {
                return new Insets(5, 10, 5, 10); // add padding
            }
        };
        currentPathField.setMinimumSize(new Dimension(750, currentPathField.getPreferredSize().height));
        currentPathField.setPreferredSize(new Dimension(750, currentPathField.getPreferredSize().height));
        currentPathField.setToolTipText("The path of the current directory");
        
        // Search field
        JTextField searchField = createSearchTextField();

        // Insert folder content dropdown menu
        JPopupMenu insertFolderContentMenu = new JPopupMenu();
        JMenuItem createFile = new JMenuItem("Create File");
        createFile.addActionListener(e -> createNewFile());
        JMenuItem createFolder = new JMenuItem("Create Folder");
        createFolder.addActionListener(e -> createNewFolder());
        insertFolderContentMenu.add(createFile);
        insertFolderContentMenu.add(createFolder);

        JButton dropdownButton = new JButton("▼");
        dropdownButton.addActionListener(e -> insertFolderContentMenu.show(
            dropdownButton, 
            0, 
            dropdownButton.getHeight()));

        // Adding all components to menu JPanel
        menu.add(backButton);
        menu.add(forwardButton);
        menu.add(Box.createHorizontalStrut(15)); // Adds a space of 20 pixels
        menu.add(currentPathField);
        menu.add(searchField);
        menu.add(dropdownButton);

        return menu;
    }

    private JTextField createSearchTextField()
    {
        JTextField searchField = new JTextField("Search by file or directory name..."){
            @Override
            public Insets getInsets() {
                return new Insets(5, 10, 5, 10); // add padding
            }
        };

        searchField.setForeground(Color.GRAY);
        searchField.setMinimumSize(new Dimension(300, searchField.getPreferredSize().height));
        searchField.setPreferredSize(new Dimension(300, searchField.getPreferredSize().height));
        searchField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e){
                if (searchField.getText().equals("Search by file or directory name...")){
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e){
                if (searchField.getText().isEmpty()){
                    searchField.setText("Search by file or directory name...");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    searchQuery = searchField.getText();
                    updateModel(searchQuery);
                }
            }
        });
        searchField.setToolTipText("Search entries by name. Type something then press ENTER");
        searchField.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        return searchField;
    }

    private JButton createButtonWithImagePath(String resourcePath)
    {
        ImageIcon icon = new ImageIcon(this.getClass().getResource(resourcePath));
        JButton button = new JButton(icon);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
        
        return button;
    }

    private JPopupMenu getUpdateFolderContentDropdownMenu()
    {
        JPopupMenu updateFolderContentMenu = new JPopupMenu();
        JMenuItem renameFolderContent = new JMenuItem("Rename");
        renameFolderContent.addActionListener(e -> renameFolderContent());
        JMenuItem deleteFolderContent = new JMenuItem("Delete");
        deleteFolderContent.addActionListener(e -> deleteFolderContent());
        updateFolderContentMenu.add(renameFolderContent);
        updateFolderContentMenu.add(deleteFolderContent);

        return updateFolderContentMenu;
    }

    private void showUpdateFolderContentMenu(MouseEvent e)
    {
        updateFolderContentPopupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    // Where the table is updated
    private void updateModel(String searchQuery)
    {
        folderContentsTableModel.clear();
        ArrayList<FolderContent> data = getFilteredData(searchQuery);
        for (FolderContent row : data){
            folderContentsTableModel.addRow(row);
        }
        
        // Update the text in the field that shows the current path
        if (currentPathField != null) currentPathField.setText(docRepo.getPathOfFolder(currentParentFolderId));
    }

    // Filter data by name based on a search query, which the user can input in the search text field in the menu
    private ArrayList<FolderContent> getFilteredData(String searchQuery)
    {
        ArrayList<FolderContent> filteredData = new ArrayList<FolderContent>();

        ArrayList<FolderContent> data = docRepo.getFolderContentsForUser(currentParentFolderId, MainFrame.window.currentUser.userId);

        for (FolderContent fc : data){
            if (fc.name.contains(searchQuery)){
                filteredData.add(fc);
            }
        }

        return filteredData;
    }

    private void showErrDialog(String msg)
    {
        JOptionPane.showMessageDialog(this, msg);
    }

    // Show a dialog where a user can enter a new file name
    private void createNewFile()
    {
        boolean done = false;
        // Keep showing the dialog in case user enters invalid input
        while (!done){
            String fileNameWithExtension = JOptionPane.showInputDialog(this, "Enter File Name (with extension)");

            if (fileNameWithExtension == null || fileNameWithExtension == "") {
                // Don't do anything if "Cancel" was clicked
                done = true;
                continue;
            } 
            
            // File names need to be of the format: *.*
            if (!fileNameWithExtension.matches("([^\\.]*)\\.([^\\.]*)")) {
                JOptionPane.showMessageDialog(this, "File name must have only one period");
                continue;
            }

            String fileName = fileNameWithExtension.substring(0,fileNameWithExtension.indexOf("."));
            String fileExtension = fileNameWithExtension.substring(fileNameWithExtension.indexOf(".") + 1);
            Document document = new Document(
                0, 
                MainFrame.window.currentUser.userId, 
                currentParentFolderId == 0 ? null:currentParentFolderId, 
                MainFrame.window.currentUser.userId, 
                0, 
                Timestamp.from(Instant.now()), 
                Timestamp.from(Instant.now()), 
                fileExtension, 
                fileName);
            
            done = docRepo.addDocument(document);
            updateModel(searchQuery);

            // If there was error, tell the user, and give them a possible reason for the error
            if (!done){
                JOptionPane.showMessageDialog(this, "Something went wrong while attempting to add document. Check if the name has been taken.");
            }
        }
    }

    // Show a dialog where a user can enter a new folder name
    private void createNewFolder()
    {
        boolean done = false;
        while (!done){
            String folderName = JOptionPane.showInputDialog(this, "Enter Folder Name");

            if (folderName == null || folderName == "") {
                // Don't do anything if "Cancel" was clicked
                done = true;
                continue;
            } 

            Folder folder = new Folder(
                0, 
                MainFrame.window.currentUser.userId, 
                currentParentFolderId == 0 ? null:currentParentFolderId, 
                MainFrame.window.currentUser.userId, 
                Timestamp.from(Instant.now()), 
                Timestamp.from(Instant.now()), 
                "dir", 
                folderName);
            
            done = docRepo.addFolder(folder);
            updateModel(searchQuery);

            // If there was error, tell the user, and give them a possible reason for the error
            if (!done){
                JOptionPane.showMessageDialog(this, "Something went wrong while attempting to add folder. Check if the name has been taken.");
            }
        }
    }

    private void renameFolderContent()
    {
        boolean done = false;
        while (!done){
            String folderContentName = JOptionPane.showInputDialog(this, "Enter New Name");

            if (folderContentName == null || folderContentName == "") {
                // Don't do anything if "Cancel" was clicked
                done = true;
                continue;
            }
            
            done = isDir ? docRepo.updateFolderName(folderContentId, folderContentName):docRepo.updateDocumentName(folderContentId, folderContentName);
            updateModel(searchQuery);
            if (!done){
                JOptionPane.showMessageDialog(this, "Something went wrong while attempting to rename folderContent. Check if the name has been taken.");
            }
        }
    }

    private void deleteFolderContent()
    {
        boolean success = false;
        if (isDir) {
            success = docRepo.deleteFolder(folderContentId);
        } else {
            success = docRepo.deleteDocument(folderContentId);
        }
        updateModel(searchQuery);

        if (!success) {
            JOptionPane.showMessageDialog(this, "An error occured while trying to delete this folder content.");
        }
    }
}
