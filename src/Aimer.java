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

	static final double TOLERANCE = 0.01;
	static final double SQUARE = 0.117;
	static final double TRIANGLE = 0.93;
	static final double PENTAGON = 0.158;
	static final double ENEMY = 0.103;
	
	boolean debug = true;
	int cycles = 0;
	
	Controller controls;

	private double width;
	private double height;
	private double centerX;
	private double centerY;
	private double scale;
	
	public Aimer(Controller c) {
		controls = c;
		width = c.screenRegion().getWidth();
		height = c.screenRegion().getHeight();
		centerX = width / 2;
		centerY = height / 2;

		// average dimension of the view window, used to guess the approximate scale of the objects
		scale = (width + height) / 2;
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
				try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); System.exit(1); }	// TODO: REMOVE ME, DEBUG ONLY
				long start = System.currentTimeMillis();
			
				Robot robot = null;
				try {
					robot = new Robot();
				} catch (AWTException e) { e.printStackTrace(); System.exit(1); }
				
				Mat img = getScreenshot(robot);
				debugSaveImage("01.png", img);

				// Do image processingws
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
			double w = stats_squares.get(label, Imgproc.CC_STAT_WIDTH)[0];
			double h = stats_squares.get(label, Imgproc.CC_STAT_HEIGHT)[0];
			double area = stats_squares.get(label, Imgproc.CC_STAT_AREA)[0];
			if( Math.abs( (w/h) - 1.0 ) < 0.1  && area < 10000 && isInRange(w, SQUARE) ) {
				double threat = calcThreatLevel(x, y, 2.0);
				System.out.println(String.format("Square at (%1.0f, %1.0f) with threat %1.3f",x,y,threat));
				targets.add(new Target((int)x, (int)y, threat));
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
			double w = stats_pentagons.get(label, Imgproc.CC_STAT_WIDTH)[0];
			double h = stats_pentagons.get(label, Imgproc.CC_STAT_HEIGHT)[0];
			double area = stats_pentagons.get(label, Imgproc.CC_STAT_AREA)[0];
			if( Math.abs( (w/h) - 1.0 ) < 0.1 && area < 10000 && isInRange(w, PENTAGON)) {  // area ~= 925 ?
				double threat = calcThreatLevel(x, y, 4.0);
				System.out.println(String.format("Pentagon at (%1.0f, %1.0f) with threat %1.3f",x,y,threat));
				targets.add(new Target((int)x, (int)y, threat));
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
			double w = stats_others.get(label, Imgproc.CC_STAT_WIDTH)[0];
			double h = stats_others.get(label, Imgproc.CC_STAT_HEIGHT)[0];
			double area = stats_others.get(label, Imgproc.CC_STAT_AREA)[0];
			if( Math.abs( (w/h) - 1.0 ) < 0.1 && area < 10000 && isInRange(w, ENEMY)) { 	// variable in size, but area > 600 ?
				double threat = calcThreatLevel(x, y, 5.0);
				System.out.println(String.format("ENEMY at (%1.0f, %1.0f) with threat %1.3f",x,y,threat));
				targets.add(new Target((int)x, (int)y, threat));
			} else if( Math.abs( (w/h) - 1.0 ) < 0.1 && area < 10000 && isInRange(w, TRIANGLE) ) {	// area ~=333 ?
				double threat = calcThreatLevel(x,y,3.0);
				System.out.println(String.format("Triangle at (%1.0f, %1.0f) with threat %1.3f",x,y,threat));
				targets.add(new Target((int)x, (int)y, threat));
			}
		}

		targets.addLast(new Target(centerX, centerY));
		return targets;
	}
	
	private boolean isInRange(double size, double ratio) {
		System.out.println(String.format("Size %f / scale %f = %f, compared to ratio %f", size, scale, (size/scale), ratio));
		return Math.abs((size/scale) - ratio) < TOLERANCE;
	}
	
	private double calcThreatLevel(double x, double y, double base) {
		double a = centerX - x;
		double b = centerY - y;
		double dist = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		double distFactor = (1/dist);
		return base + distFactor;
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
			Imgcodecs.imwrite(name, image);
		}
	}
	
}
