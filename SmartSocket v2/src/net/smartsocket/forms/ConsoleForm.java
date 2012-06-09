/*
 * ConsoleForm.java
 *
 * Created on Feb 25, 2011, 10:10:14 PM
 */
package net.smartsocket.forms;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import net.smartsocket.Config;
import net.smartsocket.SmartSocketServer;
import net.smartsocket.serverextensions.AbstractExtension;
import net.smartsocket.serverextensions.TCPExtension;

/**
 * The ConsoleForm class is the hub of all GUI activity.
 * @author XaeroDegreaz
 */
public class ConsoleForm extends javax.swing.JFrame {

	/** Creates new form ConsoleForm */
	public ConsoleForm() {
		//# TODO - Think up some stuff for the drop down menus.
		Toolkit kit = Toolkit.getDefaultToolkit();
		initComponents();

		instance = this;
		screenSize = kit.getScreenSize();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabbedPane = new JTabbedPane();
        scrollPaneCritical = new JScrollPane();
        logText = new JEditorPane();
        lblConnectedClients = new JLabel();
        lblUpstream = new JLabel();
        lblDownstream = new JLabel();
        lblUptime = new JLabel();
        lblMemoryUsage = new JLabel();
        menuBar = new JMenuBar();
        jMenu1 = new JMenu();
        jMenu2 = new JMenu();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("SmartSocket V2");
        logText.setContentType("text/html");
        logText.setEditable(false);
        logText.setFont(new Font("Verdana", 0, 10));
        logText.setText("<html>\r\n  \n");
        scrollPaneCritical.setViewportView(logText);
        tabbedPane.addTab("Critical", scrollPaneCritical);
        lblConnectedClients.setText("Connected Clients: 0");
        lblUpstream.setText("Upstream: 0 kb/s");
        lblDownstream.setText("Downstream: 0 kb/s");
        lblUptime.setText("Uptime: 0 hrs");
        lblMemoryUsage.setText("Memory Usage: 0 MB");
        jMenu1.setText("File");
        menuBar.add(jMenu1);
        jMenu2.setText("Edit");
        menuBar.add(jMenu2);

        setJMenuBar(menuBar);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lblConnectedClients)
                .addGap(18, 18, 18)
                .addComponent(lblUpstream)
                .addGap(18, 18, 18)
                .addComponent(lblDownstream)
                .addGap(18, 18, 18)
                .addComponent(lblUptime)
                .addGap(18, 18, 18)
                .addComponent(lblMemoryUsage))
            .addComponent(tabbedPane, GroupLayout.PREFERRED_SIZE, 610, GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(tabbedPane, GroupLayout.PREFERRED_SIZE, 343, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblConnectedClients)
                    .addComponent(lblUpstream)
                    .addComponent(lblDownstream)
                    .addComponent(lblUptime)
                    .addComponent(lblMemoryUsage))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/**
	 * This method is called when TCP, or UDP extension is initialized. If there are multiple extensions that will be ran, 
	 * only the first extension to load will effect this console form. The addition of the tabs for each extension is done
	 * through the actual TCPExtension and UDPExtension classes.
	 * @param caller
	 */
	public static synchronized void start( final AbstractExtension caller ) {
		//# Forego all of this console setup stuff if it's been done already...
		if ( isSetup || Config.useGUI == false ) {
			AbstractExtension.isConsoleFormRegistered = true;
			return;
		}

		UIManager.LookAndFeelInfo[] lafInfos =  UIManager.getInstalledLookAndFeels();

		//# Set our look and feel to Windows if it's there...
		for( int i = 0; i < lafInfos.length; i++ )
		{
			if ( lafInfos[i].getName().equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel" ) == true ) {
				try {
					UIManager.setLookAndFeel( "com.sun.java.swing.plaf.windows.WindowsLookAndFeel" );
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		//# Begin the running of the console creation thread
		java.awt.EventQueue.invokeLater( new Runnable() {

			public void run() {
				new ConsoleForm();

				logText.setText( ""
						+ "<html>"
						+ "<head>"
						+ "<style>"
						+ "td{text-align: left;}"
						+ "body{font-family: Verdana; font-size: 10px;}"
						+ "</style>"
						+ "</head>"
						+ "<table border='0' cellspacing='0' cellpadding='0' width='100%'>"
						+ "<tr id='marker'><td></td><td width='100%'></td></tr>"
						+ "" );
				instance.setLocation( (screenSize.width / 2) - (instance.getPreferredSize().width / 2), (screenSize.height / 2) - (instance.getPreferredSize().height / 2) );
				instance.setVisible( true );

				//# In order to keep everything running in the proper order, after this form is operation,
				//# and while in the same thread, we start our server extension, which is subclassing AbstractExtension
				//# We also need to take into account that this form is opened when double-clicking the SmartSocket.jar
				//# and we need to also still call open on that class.
				if ( caller != null ) {
					AbstractExtension.isConsoleFormRegistered = true;
				} else {
					SmartSocketServer.open();
				}

				//# Begin having the StatisticsTracker perform timed operations that display stats in GUi
				StatisticsTracker.start();

				new Thread( new Runnable() {

					public void run() {
						((TCPExtension) caller).open();
					}
				}, "TCPClient Thread" ).start();

			}
		} );
		//# Mark the setup as complete so as not to allow other extensions to call this start method and mess up the console output
		isSetup = true;
	}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static JMenu jMenu1;
    private static JMenu jMenu2;
    public static JLabel lblConnectedClients;
    public static JLabel lblDownstream;
    public static JLabel lblMemoryUsage;
    public static JLabel lblUpstream;
    public static JLabel lblUptime;
    public static JEditorPane logText;
    public static JMenuBar menuBar;
    public static JScrollPane scrollPaneCritical;
    public static JTabbedPane tabbedPane;
    // End of variables declaration//GEN-END:variables
	public static JFrame instance = null;
	private static boolean isSetup = false;
	/**
	 * 
	 */
	public static Dimension screenSize;
}
