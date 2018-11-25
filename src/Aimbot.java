import org.opencv.core.Core;

public class Aimbot {

	static{System.loadLibrary(Core.NATIVE_LIBRARY_NAME);}
	
	public static void main(String[] args) {
		
		Controller controls = new Controller();
		
		Aimer aimer = new Aimer( controls );
		
		new Thread(aimer).start();
		
	}
}
