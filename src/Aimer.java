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
import org.opencv.core.Scalar;
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
	
	public Aimer(Controller c) {
		controls = c;
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
		im_hsv.convertTo(im_hsv, CvType.CV_64FC3);
		
		List<Mat> channels = new ArrayList<Mat>();
		Core.split(im_hsv, channels);
		Mat hue = channels.get(0);
		showResult(hue, "hue");
		Mat sat = channels.get(1);
		showResult(sat, "sat");
		Mat val = channels.get(2);
		showResult(val, "val");
		
		Mat squares = new Mat();
		Imgproc.threshold(hue, squares, 95.0, 256.0, Imgproc.THRESH_TOZERO_INV);
		Imgproc.threshold(squares, squares, 93.0, 256.0, Imgproc.THRESH_BINARY);
		showResult(squares, "squares");

		
		Mat pentagons = new Mat();
		Imgproc.threshold(hue, pentagons, 7.0, 256.0, Imgproc.THRESH_TOZERO_INV);
		Imgproc.threshold(pentagons, pentagons, 1.0, 256.0, Imgproc.THRESH_BINARY);
		showResult(pentagons, "pentagons");
		
		Mat otherObjects = new Mat();	// includes triangles, players, and bullets
		Imgproc.threshold(hue, otherObjects, 100.0, 255, Imgproc.THRESH_BINARY);
		showResult(otherObjects, "other");
		
	}
	
	private static LinkedList<Target> processImage(Mat im_in) {
		
		int width = im_in.width();
		int height = im_in.height();
		int centerX = width / 2;
		int centerY = height / 2;
		
		// put whatever you figure out from that main method here
		

		LinkedList<Target> targets = new LinkedList<Target>();
		targets.add(new Target(centerX, centerY));
		return targets;
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
