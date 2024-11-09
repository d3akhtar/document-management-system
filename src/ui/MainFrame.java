package ui;

import javax.swing.*;

public class MainFrame extends JFrame
{
    private final MainFrame window;
    
    public MainFrame()
    {
        window = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1920, 1080);

        initComponents();

        tabbedPane.add("File Explorer", new FileExplorer());
        tabbedPane.add("Document Editor", new DocumentEditor());
        tabbedPane.add("Login Screen", new LoginScreen());
        tabbedPane.add("Team Explorer", new TeamExplorer());

        setContentPane(tabbedPane);
    }

    private void initComponents()
    {
        tabbedPane = new JTabbedPane();
    }

    // GUI Components
    private JTabbedPane tabbedPane;

}