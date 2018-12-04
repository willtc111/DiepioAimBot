import java.util.Comparator;

public class Target {

	int x;
	int y;
	double threatLevel;
	
	public Target(int x, int y, double threatLevel ) {
		this.x = x;
		this.y = y;
		this.threatLevel = threatLevel;
	}
	
	/**
	 * Constructor for the lowest priority target
	 */
	public Target(int x, int y) {
		this.x = x;
		this.y = y;
		threatLevel = -1.0;
	}
	
	
	public boolean shouldShoot() {
		return threatLevel > 0.0;
	}
	
	public static Comparator<Target> BY_THREAT_LEVEL() {
		return new Comparator<Target>() {
			@Override
			public int compare(Target a, Target b) {
				return -1 * Double.compare(a.threatLevel, b.threatLevel);
			}
		};
	}
}
