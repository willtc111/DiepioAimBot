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

public class Aimer implements Runnable {

	static final double TOLERANCE = 0.05;
	static final double[] SQUARE = {0.1411, 0.5882, 1.0};
	static final double[] TRIANGLE = {0.9988, 0.5317, 0.9882};
	static final double[] PENTAGON = {0.6381, 0.5317, 0.9882};
	static final double[] ENEMY = {0.9939, 0.6763, 0.9451};
	static final double[] PLAYER = {0.5348, 1.0, 0.8824};
	
	boolean debug = true;
	int cycles = 0;
	
	Controller controls;

	private double width;
	private double height;
	private double centerX;
	private double centerY;
	private double distMax;
	
	public Aimer(Controller c) {
		controls = c;
		
		width = c.screenRegion().getWidth();
		height = c.screenRegion().getHeight();
		centerX = width / 2;
		centerY = height / 2;
		distMax = Math.sqrt(Math.pow(centerX, 2) + Math.pow(centerY, 2));
	}
	
	@Override
	public void run() {
		while( !controls.shouldRun() ) {
			try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); System.exit(1); }
			
		}
		while( !controls.isClosed() ) {
			if( !controls.shouldRun() ) {
				System.out.print(" . ");
				try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); System.exit(1); }
			} else {
				long start = System.currentTimeMillis();
			
				Robot robot = null;
				try {
					robot = new Robot();
				} catch (AWTException e) { e.printStackTrace(); System.exit(1); }
				
				Mat img = getScreenshot(robot);
				debugSaveImage("01.png", img);

				// Do image processing
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
			}
		}
		System.exit(0);
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
	
	public static void main( String[] args ) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat im_in = Imgcodecs.imread("testImage.png", 1);
		showResult(im_in, "original");

		Mat im_hsv = new Mat();
	    Imgproc.cvtColor(im_in, im_hsv, Imgproc.COLOR_RGB2HSV);
		im_hsv.convertTo(im_hsv, CvType.CV_8UC3);
		
		List<Mat> channels = new ArrayList<Mat>();
		Core.split(im_hsv, channels);
		Mat hue = channels.get(0);
		showResult(hue, "hue");
		Mat sat = channels.get(1);
		showResult(sat, "sat");
		Mat val = channels.get(2);
		showResult(val, "val");
		
		Mat squares = new Mat();
		Imgproc.threshold(hue, squares, 95, 256, Imgproc.THRESH_TOZERO_INV);
		Imgproc.threshold(squares, squares, 93, 256, Imgproc.THRESH_TOZERO);
		showResult(squares, "squares");
		
		Mat labeled_squares = new Mat(squares.size(), CvType.CV_32S);
		Mat stats_squares = new Mat(squares.size(), CvType.CV_32S);
		Mat centroids_squares = new Mat(squares.size(), CvType.CV_64F);
		int count_squares = Imgproc.connectedComponentsWithStats(squares, labeled_squares, stats_squares, centroids_squares, 8);
		
		for( int label = 0; label < count_squares; label++ ) {
			double x = centroids_squares.get(label, 0)[0];
			double y = centroids_squares.get(label, 1)[0];
			double area = stats_squares.get(label, Imgproc.CC_STAT_AREA)[0];
			if( 425 < area && area < 475 ) {
				System.out.println(String.format("Square at (%1.0f, %1.0f) with area %1.0f",x,y,area));
			}
		}

		Mat pentagons = new Mat();
		Imgproc.threshold(hue, pentagons, 7, 256, Imgproc.THRESH_TOZERO_INV);
		Imgproc.threshold(pentagons, pentagons, 1, 256, Imgproc.THRESH_BINARY);
		showResult(pentagons, "pentagons");
		
		Mat labeled_pentagons = new Mat(pentagons.size(), CvType.CV_32S);
		Mat stats_pentagons = new Mat(pentagons.size(), CvType.CV_32S);
		Mat centroids_pentagons = new Mat(pentagons.size(), CvType.CV_64F);
		int count_pentagons = Imgproc.connectedComponentsWithStats(pentagons, labeled_pentagons, stats_pentagons, centroids_pentagons, 8);
		
		for( int label = 0; label < count_pentagons; label++ ) {
			double x = centroids_pentagons.get(label, 0)[0];
			double y = centroids_pentagons.get(label, 1)[0];
			double area = stats_pentagons.get(label, Imgproc.CC_STAT_AREA)[0];
			if( 900 < area && area < 950 ) {
				System.out.println(String.format("Pentagon at (%1.0f, %1.0f) with area %1.0f",x,y,area));
			}
		}
		
		Mat otherObjects = new Mat();	// includes triangles, players, and bullets
		Imgproc.threshold(hue, otherObjects, 100.0, 255, Imgproc.THRESH_BINARY);
		showResult(otherObjects, "other");
		
		Mat labeled_others = new Mat(otherObjects.size(), CvType.CV_32S);
		Mat stats_others = new Mat(otherObjects.size(), CvType.CV_32S);
		Mat centroids_others = new Mat(otherObjects.size(), CvType.CV_64F);
		int count_others = Imgproc.connectedComponentsWithStats(otherObjects, labeled_others, stats_others, centroids_others, 8);
		
		for( int label = 0; label < count_others; label++ ) {
			double x = centroids_others.get(label, 0)[0];
			double y = centroids_others.get(label, 1)[0];
			double area = stats_others.get(label, Imgproc.CC_STAT_AREA)[0];
			if( 600 < area && area < 800 ) {
				System.out.println(String.format("ENEMY at (%1.0f, %1.0f) with area %1.0f",x,y,area));
			}
			if( 300 < area && area < 360 ) {
				System.out.println(String.format("Triangle at (%1.0f, %1.0f) with area %1.0f",x,y,area));
			}
		}
	}
	
	private LinkedList<Target> processImage(Mat im_in) {

		
		LinkedList<Target> targets = new LinkedList<Target>();
		targets.add(new Target(centerX, centerY));
		

		Mat im_hsv = new Mat();
	    Imgproc.cvtColor(im_in, im_hsv, Imgproc.COLOR_RGB2HSV);
		im_hsv.convertTo(im_hsv, CvType.CV_8UC3);
		
		List<Mat> channels = new ArrayList<Mat>();
		Core.split(im_hsv, channels);
		Mat hue = channels.get(0);
		Mat sat = channels.get(1);
		Mat val = channels.get(2);
		
		Mat squares = new Mat();
		Imgproc.threshold(hue, squares, 95, 256, Imgproc.THRESH_TOZERO_INV);
		Imgproc.threshold(squares, squares, 93, 256, Imgproc.THRESH_TOZERO);
		
		Mat labeled_squares = new Mat(squares.size(), CvType.CV_32S);
		Mat stats_squares = new Mat(squares.size(), CvType.CV_32S);
		Mat centroids_squares = new Mat(squares.size(), CvType.CV_64F);
		int count_squares = Imgproc.connectedComponentsWithStats(squares, labeled_squares, stats_squares, centroids_squares, 8);
		
		for( int label = 0; label < count_squares; label++ ) {
			double x = centroids_squares.get(label, 0)[0];
			double y = centroids_squares.get(label, 1)[0];
			double area = stats_squares.get(label, Imgproc.CC_STAT_AREA)[0];
			if( 430 < area && area < 480 ) {	// area ~= 455
//				System.out.println(String.format("Square at (%1.0f, %1.0f) with area %1.0f",x,y,area));
				targets.add(new Target((int)x,(int)y,calcThreatLevel(x,y,1.0)));
			}
		}

		Mat pentagons = new Mat();
		Imgproc.threshold(hue, pentagons, 7, 256, Imgproc.THRESH_TOZERO_INV);
		Imgproc.threshold(pentagons, pentagons, 1, 256, Imgproc.THRESH_BINARY);
		
		Mat labeled_pentagons = new Mat(pentagons.size(), CvType.CV_32S);
		Mat stats_pentagons = new Mat(pentagons.size(), CvType.CV_32S);
		Mat centroids_pentagons = new Mat(pentagons.size(), CvType.CV_64F);
		int count_pentagons = Imgproc.connectedComponentsWithStats(pentagons, labeled_pentagons, stats_pentagons, centroids_pentagons, 8);
		
		for( int label = 0; label < count_pentagons; label++ ) {
			double x = centroids_pentagons.get(label, 0)[0];
			double y = centroids_pentagons.get(label, 1)[0];
			double area = stats_pentagons.get(label, Imgproc.CC_STAT_AREA)[0];
			if( 900 < area && area < 950 ) {  // area ~= 925
//				System.out.println(String.format("Pentagon at (%1.0f, %1.0f) with area %1.0f",x,y,area));
				targets.add(new Target((int)x,(int)y,calcThreatLevel(x,y,10.0)));
			}
		}
		
		Mat otherObjects = new Mat();	// includes triangles, players, and bullets
		Imgproc.threshold(hue, otherObjects, 100.0, 255, Imgproc.THRESH_BINARY);
		
		Mat labeled_others = new Mat(otherObjects.size(), CvType.CV_32S);
		Mat stats_others = new Mat(otherObjects.size(), CvType.CV_32S);
		Mat centroids_others = new Mat(otherObjects.size(), CvType.CV_64F);
		int count_others = Imgproc.connectedComponentsWithStats(otherObjects, labeled_others, stats_others, centroids_others, 8);
		
		for( int label = 0; label < count_others; label++ ) {
			double x = centroids_others.get(label, 0)[0];
			double y = centroids_others.get(label, 1)[0];
			double area = stats_others.get(label, Imgproc.CC_STAT_AREA)[0];
			if( 600 < area && area < 800 ) { 	// variable in size, but area > 600
//				System.out.println(String.format("ENEMY at (%1.0f, %1.0f) with area %1.0f",x,y,area));
				targets.add(new Target((int)x,(int)y,calcThreatLevel(x,y,100.0)));
			} else if( 300 < area && area < 360 ) {	// area ~=333 
//				System.out.println(String.format("Triangle at (%1.0f, %1.0f) with area %1.0f",x,y,area));
				targets.add(new Target((int)x,(int)y,calcThreatLevel(x,y,5.0)));
			}
		}

		return targets;
	}
	
	private double calcThreatLevel(double x, double y, double mult) {
		double a = centerX - x;
		double b = centerY - y;
		double dist = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		double distFactor = distMax-dist;
		return mult * distFactor;
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
	
	private void debugSaveImage(String name, Mat image) {
		if( debug ) {
			Imgcodecs.imwrite("postimage.png", image);
		}
	}
	
}
