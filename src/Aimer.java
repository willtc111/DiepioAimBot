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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


public class Aimer implements Runnable {
	
	boolean debug = true;
	int cycles = 0;
	
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
		
		// print view window information
		System.out.println("height" + viewHeight() + ", width " + viewWidth() );
		
		// Main loop
		while( !controls.isClosed() ) {
			if( !controls.shouldRun() ) {
				// Do nothing
				System.out.print(" . ");
				try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); System.exit(1); }
			} else {
				// Do aiming!
				long start = System.currentTimeMillis();
			
				Robot robot = null;
				try {
					robot = new Robot();
				} catch (AWTException e) { e.printStackTrace(); System.exit(1); }
				
				Mat img = getScreenshot(robot);
				debugSaveImage("01.png", img);

				// Do image processing to find targets
				LinkedList<Target> targets = processImage(img);
				
				// Pick where to point the mouse
				targets.sort(Target.BY_THREAT_LEVEL());
				Target target = targets.getFirst();
				
				// Move the mouse and maybe click
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
				
				if( target.shouldShoot() ) {
					int x = controls.screenRegion().x + target.x;
					int y = controls.screenRegion().y + target.y;
					robot.mouseMove(x, y);
					System.out.println("Shooting!");
					robot.mousePress(InputEvent.BUTTON1_MASK);
				}
				
				long end = System.currentTimeMillis();
				System.out.println("\nduration: " + (end - start));
				cycles++;
				
				//try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); System.exit(1); }	// TODO: REMOVE ME, DEBUG ONLY
				
			}
		}
		System.exit(0);
	}
	
	private LinkedList<Target> processImage(Mat im_in) {
		LinkedList<Target> targets = new LinkedList<Target>();
		
		Mat im_hsv = new Mat();
	    Imgproc.cvtColor(im_in, im_hsv, Imgproc.COLOR_RGB2HSV);
	    im_hsv.convertTo(im_hsv, CvType.CV_8UC3);
	    
	    List<Mat> channels = new ArrayList<Mat>();
 		Core.split(im_hsv, channels);
 		Mat hue = channels.get(0);
 		Mat sat = channels.get(1);
 		Mat val = channels.get(2);
	    
 		int minRadius = 15;
 		int maxRadius = 70;
 		Mat targetMask = Mat.zeros(im_in.size(), CvType.CV_8U);	// includes only enemy players
 		for( int col = 0; col < im_hsv.width(); col++ ) {
 			for( int row = 0; row < im_hsv.height(); row++ ) {
 				double sVal = sat.get(row, col)[0];
 				double hVal = hue.get(row, col)[0];
 				double vVal = val.get(row, col)[0];
 				if( 100 < hVal && 165 < sVal && sVal < 175.0 && 150 < vVal ) {
 					targetMask.put(row, col, 255.0);
 				}
 			}
 		}

 		
 		//showResult(targetMask, "mask");
        
 		Mat circles = new Mat();
 		Imgproc.HoughCircles(val, circles, Imgproc.CV_HOUGH_GRADIENT, 1, minRadius*2, 20, 50, minRadius, maxRadius);
 		
 		for (int i = 0; i < circles.cols(); i++) {
            double[] circleInfo = circles.get(0, i);
            int x = (int) Math.floor(Math.abs(circleInfo[0]));
            int y = (int) Math.floor(Math.abs(circleInfo[1]));
            int radius = (int) Math.round(circleInfo[2]);
            if( targetMask.get(y, x)[0] > 0 ) {
            	// WE GOT ONE!
            	double threatLevel = calcThreatLevel(x,y);
            	System.out.println("found at " + x+","+y + " with TL: " + threatLevel);
            	targets.addFirst(new Target(x, y, threatLevel));
            	
            	// draw center point of target circle
                Imgproc.circle(im_in, new Point(x, y), 1, new Scalar(0,0,0), 3, 8, 0 );
                // draw outline of target circle
                Imgproc.circle(im_in, new Point(x, y), (int) radius, new Scalar(0,0,0), 3, 8, 0 );
            }
            
        }
 		//showResult(im_in, "Found");
 		
		targets.addLast(new Target(centerX(), centerY()));
		return targets;
	}
	
	
	private double calcThreatLevel(double x, double y) {
		double a = centerX() - x;
		double b = centerY() - y;
		double dist = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		double distFactor = (1/dist);
		return distFactor;
	}
	
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
	
	public static void showResult(Mat img, String title) {
	    Imgproc.resize(img, img, new Size(640, 480));
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
	        frame.setVisible(true);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private void debugSaveImage(String name, Mat image) {
		if( debug ) {
			Imgcodecs.imwrite(name, image);
		}
	}
	
}
