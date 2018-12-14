import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Aimer implements Runnable {
	
	boolean debug = true;
	int cycles = 0;
	
	Controller controls;

	private double width;
	private double height;
	private double centerX;
	private double centerY;
	
	private int numObjectTypes = 6;
	private Mat[] imgs = new Mat[numObjectTypes];
	private MatOfKeyPoint[] kps = new MatOfKeyPoint[numObjectTypes];
	private Mat[] dess = new Mat[numObjectTypes];
	private ORB orb;
	private BFMatcher matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING, true);
	
	public Aimer(Controller c) {
		controls = c;
		
		// get view window information
		width = c.screenRegion().getWidth();
		height = c.screenRegion().getHeight();
		centerX = width / 2;
		centerY = height / 2;

		// create keypoints and descriptors for each of the objects to detect
		orb = ORB.create();
		orb.setScoreType(ORB.FAST_SCORE);
		orb.setEdgeThreshold(9);
		orb.setFastThreshold(1);
		for( int i = 0; i < numObjectTypes; i++ ) {
			String imgName;
			switch(i) {
				case 0:	imgName = "tank.png";	break;
				case 1: imgName = "pink_triangle.png";	break;
				case 2:	imgName = "pentagon.png";	break;
				case 3: imgName = "red_triangle.png";	break;
				case 4: imgName = "square.png";	break;
				default: imgName = "bullet.png";	break;
			}
			imgs[i] = Imgcodecs.imread(imgName);
		    Imgproc.cvtColor(imgs[i], imgs[i], Imgproc.COLOR_RGB2GRAY);
			
			kps[i] = new MatOfKeyPoint();
			dess[i] = new Mat();
			orb.detectAndCompute(imgs[i], Mat.ones(imgs[i].size(), CvType.CV_8U), kps[i], dess[0]);
		}
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
				try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); System.exit(1); }	// TODO: REMOVE ME, DEBUG ONLY
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
			}
		}
		System.exit(0);
	}
	
	private LinkedList<Target> processImage(Mat im_in) {

		LinkedList<Target> targets = new LinkedList<Target>();
		
		MatOfKeyPoint cur_kps = new MatOfKeyPoint();
		Mat cur_des = new Mat();
		orb.detectAndCompute( im_in, Mat.ones(im_in.size(), CvType.CV_8U), cur_kps, cur_des );

		MatOfDMatch[] matches = new MatOfDMatch[numObjectTypes];
		for(int i = 0; i < numObjectTypes; i++ ) {
			matches[i] = new MatOfDMatch();
			matcher.match(dess[i], cur_des, matches[i]);
			DMatch[] matchArray = matches[i].toArray();
			System.out.println(matchArray.length);
		}
		
		Mat im_out = new Mat();
		Features2d.drawMatches(imgs[3], kps[3], im_in, cur_kps, matches[3], im_out);
		showResult(im_out, "matches");
		
		targets.addLast(new Target(centerX, centerY));
		return targets;
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
	    Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);
	    return img;
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
