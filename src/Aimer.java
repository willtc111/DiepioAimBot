import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class Aimer implements Runnable {

	boolean debug = false;
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
				
				// Pick where to point the mouse
				
				// Move the mouse and click
			//	robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			//	robot.mouseMove(x, y);
			//	robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
				
				long end = System.currentTimeMillis();
				System.out.println("\nduration: " + (end - start));
				cycles++;
			}
		}
		System.exit(0);
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
