import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Takes user input for the diep.io aimbot.
 * 
 * @author William Carver
 *
 */
public class Controller implements WindowListener, ActionListener {
	
	private JButton toggle;
	private JFrame frame;
	private boolean enabled = false;
	private boolean paused = true;
	private boolean closed = false;
	
	/**
	 * Construct a new Controller
	 */
	public Controller() {
		// Make a nice little button
		JButton toggleButton = new JButton("Enable");
		toggleButton.setPreferredSize(new Dimension(400, 400));
		toggleButton.addActionListener(this);
		toggle = toggleButton;
		
		// Make the control window
		frame = new JFrame("Control Window");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.addWindowListener(this);
		
		// Add the button to the window
		Container contents = frame.getContentPane();
		contents.add(toggleButton, BorderLayout.CENTER);
		
		// Show the window
		frame.pack();
		frame.setVisible(true);
	}
	
	// Should the aimbot be aiming?
	public boolean shouldRun() {
		return !closed && enabled && !paused;
	}
	
	// Is this window closed?
	public boolean isClosed() {
		return closed;
	}
	
	// Gets the region of the screen within which to do aiming
	public Rectangle screenRegion() {
		Dimension size = frame.getSize();
		int x = frame.getX();
		int y = frame.getY();
		return new Rectangle(x,y,size.width,size.height);
		
	}
	
	// Handles the button clicking
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == toggle ) {
			if( enabled == false ) {
				enabled = true;
				toggle.setText("Disable");
			} else {
				enabled = false;
				toggle.setText("Enable");
			}
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		paused = true;
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		closed = true;
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		paused = false;
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}
}
