package ui;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import model.Document;
import model.Folder;
import model.FolderContent;
import model.Team;
import model.Version;
import repository.TeamRepository;
import repository.UserRepository;
import ui.FileExplorer.FolderContentTableModel;
import ui.MainFrame.User;

public class TeamExplorer extends JPanel {
    
    // Explicitly define a table model for teams 
    class TeamTableModel extends AbstractTableModel
    {
        private String[] columnNames = {"Name", "# Members", "Status"};
        private ArrayList<Team> teams;
        private TeamRepository teamRepo;
        
        public TeamTableModel(ArrayList<Team> teams, TeamRepository teamRepo) {
            this.teams = teams;
            this.teamRepo = teamRepo;
        }

        @Override
        public int getRowCount() {
            return teams.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        public void addRow(Team t) { 
            teams.add(t); 
            fireTableRowsInserted(getRowCount()-1, getRowCount());
        }
        public void clear() { 
            int prevRowCount = getRowCount();
            teams.clear(); 
            fireTableRowsDeleted(0, prevRowCount);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Team t = teams.get(rowIndex);
            switch (columnIndex) {
                case 0: return t.teamName;
                case 1: return teamRepo.getAmountOfMembersInTeam(t.teamId);
                case 2: return t.ownerId == MainFrame.window.currentUser.userId ? "Owner":"Member";
                default: return null;
            }
        }

        public int getTeamIdForRow(int rowIndex)
        {
            return teams.get(rowIndex).teamId;
        }

        public Team getTeamById(int teamId)
        {
            for (Team t : teams) {
                if (t.teamId == teamId) return t;
            }
            return null;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
    }

    private String searchQuery;
    private TeamTableModel teamTableModel;

    // We need both team and user related content, since we need to find users based on email to add them to teams
    private TeamRepository teamRepo;
    private UserRepository userRepo;

    // Have global variables for selected ids to refer to when adding, deleting, updating, etc...
    private int selectedTeamId;
    private int selectedMemberId;

    public TeamExplorer(TeamRepository teamRepo, UserRepository userRepo)
    {
        this.teamRepo = teamRepo;
        this.userRepo = userRepo;

        selectedTeamId = 0;
        selectedMemberId = 0;

        teamTableModel = new TeamTableModel(new ArrayList<Team>() {}, teamRepo){
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        
        searchQuery = "";
        // Load the initial table content
        updateModel(searchQuery);

        JTable teamTable = initTable();

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0,0,0,0);
        
        gbc.gridx = 0;
        gbc.gridy = 0;

        add(createTeamExplorerMenu(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;

        add(new JScrollPane(teamTable), gbc);

        // Create other GUI components that will be used when adding/updating teams or members to teams
        initComponents();
    }

    private JPanel createTeamExplorerMenu()
    {
        JPanel menu = new JPanel();
        menu.setLayout(new FlowLayout());
        menu.setBackground(new Color(200, 200, 200));
        
        // Search field
        JTextField searchField = createSearchTextField();

        // Insert folder content dropdown menu
        JPopupMenu insertTeamMenu = new JPopupMenu();
        JMenuItem createTeam = new JMenuItem("Create Team");
        createTeam.addActionListener(e -> createNewTeam());
        insertTeamMenu.add(createTeam);

        JButton dropdownButton = new JButton("+");
        dropdownButton.addActionListener(e -> insertTeamMenu.show(
            dropdownButton, 
            0, 
            dropdownButton.getHeight()));

        // Adding all components to menu JPanel
        menu.add(searchField);
        menu.add(Box.createHorizontalStrut(10)); // Adds a space of 20 pixels
        menu.add(dropdownButton);

        return menu;
    }

    private JTextField createSearchTextField()
    {
        JTextField searchField = new JTextField("Search by team name..."){
            @Override
            public Insets getInsets() {
                return new Insets(5, 10, 5, 10); // add padding
            }
        };

        searchField.setForeground(Color.GRAY);
        searchField.setMinimumSize(new Dimension(1000, searchField.getPreferredSize().height));
        searchField.setPreferredSize(new Dimension(1000, searchField.getPreferredSize().height));
        searchField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e){
                if (searchField.getText().equals("Search by team name...")){
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e){
                if (searchField.getText().isEmpty()){
                    searchField.setText("Search by team name...");
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

    // Create the JTable instance that will use our explicitly defined table model
    private JTable initTable()
    {
        JTable table = new JTable(teamTableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(0).setPreferredWidth(400);
        table.getColumnModel().getColumn(1).setPreferredWidth(20);
        table.getColumnModel().getColumn(2).setPreferredWidth(20);

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
                selectedTeamId = teamTableModel.getTeamIdForRow(row);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // If the row was left-clicked, run this
                    manageMembers();
                } 
                else if (e.getButton() == MouseEvent.BUTTON3){
                    // Show popup menu with option to delete team
                    showTeamUpdateMenu(e);
                }
            }
          } 
        });

        return table;
    }

    // Where the table is updated
    private void updateModel(String searchQuery)
    {
        teamTableModel.clear();
        ArrayList<Team> data = getFilteredData(searchQuery);
        for (Team row : data){
            teamTableModel.addRow(row);
        }
    }

    // Filter data by name based on a search query, which the user can input in the search text field in the menu
    private ArrayList<Team> getFilteredData(String searchQuery)
    {
        ArrayList<Team> filteredData = new ArrayList<Team>();

        ArrayList<Team> data = teamRepo.getTeamsThatUserIsIn(MainFrame.window.currentUser.userId);

        for (Team t : data){
            if (t.teamName.contains(searchQuery)){
                filteredData.add(t);
            }
        }

        return filteredData;
    }

    // Show a dialog where a user can create a new team and also add a description for the team
    private void createNewTeam()
    {
        JPanel createNewTeamDialogPanel = new JPanel();
        createNewTeamDialogPanel.setLayout(new BoxLayout(createNewTeamDialogPanel, BoxLayout.Y_AXIS));

        JTextField teamNameInputField = new JTextField();
        JPanel teamNameInputLabelPanel = new JPanel(new BorderLayout());
        teamNameInputLabelPanel.add(new JLabel("Enter Team Name"), BorderLayout.WEST);
        createNewTeamDialogPanel.add(teamNameInputLabelPanel);
        createNewTeamDialogPanel.add(teamNameInputField);

        createNewTeamDialogPanel.add(Box.createVerticalStrut(10));

        JTextArea teamDescTextArea = new JTextArea();
        teamDescTextArea.setLineWrap(true);
        teamDescTextArea.setRows(15);
        teamDescTextArea.setColumns(50);
        JPanel teamDescInputLabelPanel = new JPanel(new BorderLayout());
        teamDescInputLabelPanel.add(new JLabel("Enter Team Description"), BorderLayout.WEST);
        createNewTeamDialogPanel.add(teamDescInputLabelPanel);
        createNewTeamDialogPanel.add(teamDescTextArea);

        int result = JOptionPane.showConfirmDialog(
            this, createNewTeamDialogPanel, "Add Team", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION){
            Team team = new Team(0, MainFrame.window.currentUser.userId, teamNameInputField.getText(), teamDescTextArea.getText());
            if (!teamRepo.addTeam(team)){
                JOptionPane.showMessageDialog(this, "An error occured while adding new team.");
            }

            updateModel(searchQuery);
        }
    }

    // Show a dialog where users can add or remove members from a team
    private void manageMembers()
    {
        // Load team members for the selected team
        loadMemberList();

        memberDialogPanel = new JPanel();
        memberDialogPanel.setLayout(new BoxLayout(memberDialogPanel, BoxLayout.Y_AXIS));
        memberDialogPanel.setMinimumSize(new Dimension(854, 480));
        memberDialogPanel.setPreferredSize(new Dimension(854, 480));

        JScrollPane scrollPane = new JScrollPane(memberListPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        memberDialogPanel.add(scrollPane);

        JOptionPane.showMessageDialog(this, memberDialogPanel, "Manage Members", JOptionPane.PLAIN_MESSAGE);
    }

    // Refresh member list, which is useful to show updates when we add or remove members
    private void loadMemberList()
    {
        memberListPanel.removeAll();

        for (User member : teamRepo.getTeamMembers(selectedTeamId)){
            JPanel memberLabelPanel = new JPanel(new BorderLayout());
            memberLabelPanel.setMinimumSize(new Dimension(memberLabelPanel.getWidth(), 20));
            memberLabelPanel.setPreferredSize(new Dimension(memberLabelPanel.getWidth(), 20));
            JLabel memberLabel = new JLabel("Username: " + member.username + ", Email: " + member.email);
            
            memberLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            memberLabel.setFont(new Font("Serif", Font.PLAIN, 16));
            memberLabel.setFocusable(true);

            memberLabelPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            memberLabelPanel.add(memberLabel, BorderLayout.CENTER);
            memberLabel.setHorizontalAlignment(SwingConstants.CENTER);

            memberLabelPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Save memberId for reference if user wants to remove the member
                    selectedMemberId = member.userId;
                    if (e.getButton() == MouseEvent.BUTTON3){
                        // If right clicked, show popup menu with option to remove or add member
                        showMemberUpdateMenu(e);
                    }
                }
            });
            
            memberListPanel.add(memberLabelPanel);
        }
    }

    private void removeMember()
    {
        if (teamTableModel.getTeamById(selectedTeamId).ownerId != MainFrame.window.currentUser.userId) {
            JOptionPane.showMessageDialog(this, "Can't remove members from the team unless you're the owner.");
            return;
        }

        if (selectedMemberId == teamTableModel.getTeamById(selectedTeamId).ownerId){
            JOptionPane.showMessageDialog(this, "Can't remove owner from the team.");
            return;
        }

        if (!teamRepo.removeMemberFromTeam(selectedMemberId, selectedTeamId)){
            JOptionPane.showMessageDialog(this, "An error occured while removing member from team.");
        }

        loadMemberList();
        memberDialogPanel.revalidate();
        memberDialogPanel.repaint();
    }

    // Show a dialog where the user can add the email of the user they want added to the selected team, and display any errors
    private void addMember()
    {
        String email = JOptionPane.showInputDialog(this, "Add member to team using email");

        if (email == null || email == "") return;

        User userToAdd = userRepo.getUserWithEmail(email);

        if (userToAdd == null){
            JOptionPane.showMessageDialog(this, "Can't find user with this email");
            return;
        }

        if (!teamRepo.addMemberToTeam(userToAdd.userId, selectedTeamId)){
            JOptionPane.showMessageDialog(this, "An error occured while adding member to team.");
        }

        loadMemberList();
        memberDialogPanel.revalidate();
        memberDialogPanel.repaint();
    }

    private void removeTeam()
    {
        if (teamTableModel.getTeamById(selectedTeamId).ownerId != MainFrame.window.currentUser.userId) {
            JOptionPane.showMessageDialog(this, "Can't remove team unless you're the owner.");
            return;
        }

        if (!teamRepo.removeTeam(selectedTeamId)){
            JOptionPane.showMessageDialog(this, "Something went wrong when attempting to remove team.");
        }

        updateModel(searchQuery);
    }

    // Show the popup menu for updating teams
    private void showTeamUpdateMenu(MouseEvent e)
    {
        teamUpdateMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    // Show the popup menu for updating team members
    private void showMemberUpdateMenu(MouseEvent e)
    {
        memberUpdateMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void initComponents()
    {
        // Create team update popup menu (only option will be removing team)
        teamUpdateMenu = new JPopupMenu();
        JMenuItem removeTeam = new JMenuItem("Remove");
        removeTeam.addActionListener(e -> removeTeam());
        teamUpdateMenu.add(removeTeam);


        // Create member update popup menu
        memberUpdateMenu = new JPopupMenu();
        JMenuItem addMember = new JMenuItem("Add");
        addMember.addActionListener(e -> addMember());
        JMenuItem removeMember = new JMenuItem("Remove");
        removeMember.addActionListener(e -> removeMember());
        memberUpdateMenu.add(addMember);
        memberUpdateMenu.add(removeMember);

        // Prepare version list panel for loading
        memberListPanel = new JPanel();
        memberListPanel.setLayout(new BoxLayout(memberListPanel, BoxLayout.Y_AXIS));
    }

    // GUI Components
    JPopupMenu teamUpdateMenu;

    JPopupMenu memberUpdateMenu;
    JPanel memberListPanel;
    JPanel memberDialogPanel;
}
