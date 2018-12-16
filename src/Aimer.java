import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Performs the object detection and aiming for the diep.io aimbot.
 * 
 * @author William Carver
 *
 */
public class Aimer implements Runnable {
		
	Controller controls;
	
	public Aimer(Controller c) {
		controls = c;
	}
	
	@Override
	public void run() {
		// Allow the user to position the window properly
		while( !controls.shouldRun() ) {
			try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); System.exit(1); }
		}
		
		// Main loop
		while( !controls.isClosed() ) {
			if( !controls.shouldRun() ) {
				// Do nothing
				System.out.print(" . ");
				try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); System.exit(1); }
			} else {
				// Do aiming!
				
				// Instantiate a robot for screen capture and mouse control
				Robot robot = null;
				try {
					robot = new Robot();
				} catch (AWTException e) { e.printStackTrace(); System.exit(1); }
				
				// Get a screenshot
				Mat img = getScreenshot(robot);

				// Do image processing to find targets
				LinkedList<Target> targets = processImage(img);
				
				// Pick which target to point the mouse at
				targets.sort(Target.BY_THREAT_LEVEL());
				Target target = targets.getFirst();
				
				// Unclick from the last cycle
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
				
				// Check if this is a good target
				if( target.shouldShoot() ) {
					// Move the mouse
					int x = controls.screenRegion().x + target.x;
					int y = controls.screenRegion().y + target.y;
					robot.mouseMove(x, y);
					
					// Initiate a mouse click
					robot.mousePress(InputEvent.BUTTON1_MASK);
				}
			}
		}
		System.exit(0);
	}
	
	private LinkedList<Target> processImage(Mat im_in) {
		// Make a list of targets
		LinkedList<Target> targets = new LinkedList<Target>();
		
		// Convert the input image to HSV
		Mat im_hsv = new Mat();
	    Imgproc.cvtColor(im_in, im_hsv, Imgproc.COLOR_RGB2HSV);
	    im_hsv.convertTo(im_hsv, CvType.CV_8UC3);
	    
	    // Break the image into individual hue, saturation, and value channels
	    List<Mat> channels = new ArrayList<Mat>();
 		Core.split(im_hsv, channels);
 		Mat hue = channels.get(0);
 		Mat sat = channels.get(1);
 		Mat val = channels.get(2);
	    
 		// Perform Hough Transform for circles on the value channel of the image
 		int minRadius = 20;
 		int maxRadius = 100;        
 		Mat circles = new Mat();
 		Imgproc.HoughCircles(val, circles, Imgproc.CV_HOUGH_GRADIENT, 1, minRadius*2, 80, 30, minRadius, maxRadius);
 		
 		// For each circle found
 		for (int i = 0; i < circles.cols(); i++) {
            double[] circleInfo = circles.get(0, i);
            int x = (int) Math.floor(Math.abs(circleInfo[0]));
            int y = (int) Math.floor(Math.abs(circleInfo[1]));
            
            // Check if it is a potential target
            if( isTarget(hue, sat, val, x, y) ) {
            	// calculate the threat level
            	double threatLevel = calcThreatLevel(x,y);
            	
            	// Add the new target to the target list
            	targets.addFirst(new Target(x, y, threatLevel));
            }
        }
 		
 		// Add a default target (the "do not shoot" target)
		targets.addLast(new Target(centerX(), centerY()));
		
		return targets;
	}
	
	/**
	 * Determine whether or not the coordinate provided is a valid target
	 * 
	 * @param h hue channel of the image
	 * @param s saturation channel of the image
	 * @param v value channel of the image
	 * @param x x coordinate of the target
	 * @param y y coordinate of the target
	 * @return True if target, otherwise false
	 */
	private boolean isTarget( Mat h, Mat s, Mat v, int x, int y ) {
		double sVal = s.get(y, x)[0];
		double hVal = h.get(y, x)[0];
		double vVal = v.get(y, x)[0];
		return ( 100 < hVal && 165 < sVal && sVal < 175.0 && 150 < vVal );
	}
	
	/**
	 * Calculate the threat level of the target at the given coordinates.
	 * Threat level is inversely proportional to the distance from the center of the image.
	 * 
	 * @param x x coordinate of the target
	 * @param y y coordinate of the target
	 * @return The threat level
	 */
	private double calcThreatLevel(double x, double y) {
		double a = centerX() - x;
		double b = centerY() - y;
		double dist = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		double distFactor = (1/dist);
		return distFactor;
	}
	
	/**
	 * Take a screenshot
	 */
	private Mat getScreenshot(Robot r) {

		BufferedImage origImg = r.createScreenCapture(controls.screenRegion());
		BufferedImage bgrImg = new BufferedImage(origImg.getWidth(), origImg.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		bgrImg.getGraphics().drawImage(origImg, 0, 0, null);
				
		Mat img = new Mat(bgrImg.getHeight(), bgrImg.getWidth(), CvType.CV_8UC3);
		byte[] pixels = ((DataBufferByte) bgrImg.getRaster().getDataBuffer()).getData();
	    img.put(0, 0, pixels);
	    
	    return img;
	}

	private double viewWidth() {
		return controls.screenRegion().getWidth();
	}
	
	private double viewHeight() {
		return controls.screenRegion().getHeight();
	}
	
	private double centerX() {
		return viewWidth() / 2;
	}
	
	private double centerY() {
		return viewHeight() / 2;
	}
	
	/**
	 * Debug function to show an image.
	 * @param img Image to show
	 * @param title The title of the image
	 */
	public static void showImage(Mat img, String title) {
	    Imgproc.resize(img, img, new Size(img.width()*0.75, img.height()*0.75));
	    MatOfByte matOfByte = new MatOfByte();
	    Imgcodecs.imencode(".jpg", img, matOfByte);
	    byte[] byteArray = matOfByte.toArray();
	    BufferedImage bufImage = null;
	    try {
	        InputStream in = new ByteArrayInputStream(byteArray);
	        bufImage = ImageIO.read(in);
	        JFrame frame = new JFrame(title);
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
	        frame.pack();
	        frame.setAutoRequestFocus(false);
	        frame.setVisible(true);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
}
