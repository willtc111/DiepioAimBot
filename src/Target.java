import java.util.Comparator;

public class Target {

	int x;
	int y;
	double threatLevel;
	
	public Target(double x, double y, double threatLevel ) {
		this.x = (int) Math.round(x);
		this.y = (int) Math.round(y);
		this.threatLevel = threatLevel;

		System.out.println(String.format("Target at %d,%d of risk %f", this.x, this.y, this.threatLevel));
	}
	
	/**
	 * Constructor for the lowest priority target
	 */
	public Target(double x, double y) {
		this(x, y, -1.0);
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
