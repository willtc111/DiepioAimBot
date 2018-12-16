import java.util.Comparator;
/**
 * Object to represent a target of the diep.io aimbot.
 * 
 * @author William Carver
 *
 */
public class Target {

	int x;
	int y;
	double threatLevel;
	
	/**
	 * Constructor for regular targets
	 * 
	 * @param x x coordinate of the target
	 * @param y y coordinate of the target
	 * @param threatLevel threat level of the target
	 */
	public Target(double x, double y, double threatLevel ) {
		this.x = (int) Math.round(x);
		this.y = (int) Math.round(y);
		this.threatLevel = threatLevel;
	}
	
	/**
	 * Constructor for the lowest priority target
	 * @param x x coordinate of the target
	 * @param y y coordinate of the target
	 */
	public Target(double x, double y) {
		this(x, y, -1.0);
	}
	
	/**
	 * Should this target be shot at? 
	 * @return True if it should be shot at, otherwise false
	 */
	public boolean shouldShoot() {
		return threatLevel > 0.0;
	}
	
	/**
	 * Comparator for sorting targets by threat level.
	 * @return A comparator for sorting targets by threat level
	 */
	public static Comparator<Target> BY_THREAT_LEVEL() {
		return new Comparator<Target>() {
			@Override
			public int compare(Target a, Target b) {
				return -1 * Double.compare(a.threatLevel, b.threatLevel);
			}
		};
	}
}
