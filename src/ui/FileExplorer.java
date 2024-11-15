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
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import repository.DocumentRepository;

public class FileExplorer extends JPanel {

    private String searchQuery;
    private DefaultTableModel directoryContentsTableModel;

    private DocumentRepository docRepo;

    public FileExplorer(DocumentRepository docRepo)
    {
        this.docRepo = docRepo;
        String[] columnNames = {"Name", "Date Created", "Date Modified", "Type", "Size"};

        directoryContentsTableModel = new DefaultTableModel(new Object[][] {}, columnNames){
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };

        searchQuery = "";
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

    private JTable initTable()
    {
        JTable table = new JTable(directoryContentsTableModel);
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
                    component.setBackground(Color.WHITE);
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
                // If a directory was clicked, go into the directory, otherwise, open the file
                String type = table.getValueAt(row, 3).toString();
                String rowName = table.getValueAt(row, 0).toString();
                System.out.println("Row with name: " + rowName + " was clicked");
                if (type == "dir"){
                    System.out.println("Going into directory!");
                }
                else{
                    System.out.println("Opening file!");
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

        JButton backButton = createButtonWithImagePath("/images/backButton.png");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Go up a directory");
            }
        });
        backButton.setToolTipText("Go up a directory");

        JButton forwardButton = createButtonWithImagePath("/images/forwardButton.png");
        forwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Go down a directory you have been through");
            }
        });
        forwardButton.setToolTipText("Go down a directory you have been through");

        JTextField currentPathField = new JTextField(){
            @Override
            public Insets getInsets() {
                return new Insets(5, 10, 5, 10); // add padding
            }
        };
        currentPathField.setMinimumSize(new Dimension(800, currentPathField.getPreferredSize().height));
        currentPathField.setPreferredSize(new Dimension(800, currentPathField.getPreferredSize().height));
        currentPathField.setToolTipText("The path of the current directory");
        
        JTextField searchField = createSearchTextField();

        menu.add(backButton);
        menu.add(forwardButton);
        menu.add(Box.createHorizontalStrut(15)); // Adds a space of 20 pixels
        menu.add(currentPathField);
        menu.add(searchField);

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

    private void updateModel(String searchQuery)
    {
        directoryContentsTableModel.setRowCount(0);
        Object[][] data = getFilteredData(searchQuery);
        for (Object[] row : data){
            directoryContentsTableModel.addRow(row);
        }
    }

    private Object[][] getSampleData()
    {        
        Object[][] data = {
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"DirA", "1/1/2011", "1/1/2011", "dir", "10KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"DirA", "1/1/2011", "1/1/2011", "dir", "10KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"DirA", "1/1/2011", "1/1/2011", "dir", "10KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"DirA", "1/1/2011", "1/1/2011", "dir", "10KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
            {"DirA", "1/1/2011", "1/1/2011", "dir", "10KB"},
            {"FileA", "1/1/2011", "1/1/2011", "txt", "2KB"},
        };

        return data;
    }

    private Object[][] getFilteredData(String searchQuery)
    {
        ArrayList<Object[]> filteredData = new ArrayList<>();

        Object[][] data = getSampleData();

        for (Object[] row : data){
            if (row[0].toString().contains(searchQuery)){
                filteredData.add(row);
            }
        }

        return filteredData.toArray(new Object[filteredData.size()][5]);
    }
}
