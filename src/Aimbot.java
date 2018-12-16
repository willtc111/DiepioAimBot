import org.opencv.core.Core;

/**
 * Main class for the diep.io aimbot
 * 
 * @author William Carver
 *
 */
public class Aimbot {

	// If you don't do this things break.
	static{System.loadLibrary(Core.NATIVE_LIBRARY_NAME);}
	
	/**
	 * Main method
	 */
	public static void main(String[] args) {
		
		// Create a controller
		Controller controls = new Controller();
		
		// Create the aimer
		Aimer aimer = new Aimer( controls );
		
		// Start aiming!
		new Thread(aimer).start();
		
	}
}
