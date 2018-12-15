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

public class Controller implements WindowListener, ActionListener {
	private JButton toggle;
	private JFrame frame;
	private boolean enabled = false;
	private boolean paused = true;
	private boolean closed = false;
	
	public Controller() {
		JButton toggleButton = new JButton("Enable");
		toggleButton.setPreferredSize(new Dimension(400, 400));
		
		toggleButton.addActionListener(this);
		
		frame = new JFrame("Control Window");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.addWindowListener(this);
		
		Container contents = frame.getContentPane();
		contents.add(toggleButton, BorderLayout.CENTER);
		
		frame.pack();
		frame.setVisible(true);
		toggle = toggleButton;
	}
	
	public boolean shouldRun() {
		return !closed && enabled && !paused;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	public Rectangle screenRegion() {
		Dimension size = frame.getSize();
		int x = frame.getX();
		int y = frame.getY();
		return new Rectangle(x,y,size.width,size.height);
		
	}
	
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == toggle ) {
			if( enabled == false ) {
				enabled = true;
				toggle.setText("Disable");
			} else {
				enabled = false;
				toggle.setText("Enable");
			}

			System.out.print( enabled ? "enabled " : "disabled " );
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		paused = true;
		//System.out.print("activated ");
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		//System.out.print("closed ");
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		closed = true;
		//System.out.print("closing ");
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		paused = false;
		//System.out.println("deactivated ");
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		//System.out.print("deiconified ");
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		//System.out.print("iconified ");
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		//System.out.print("opened ");
	}
}
