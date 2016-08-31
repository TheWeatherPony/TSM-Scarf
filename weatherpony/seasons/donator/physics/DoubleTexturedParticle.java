package weatherpony.seasons.donator.physics;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class DoubleTexturedParticle extends Particle{
	public final double texture1X, texture1Y, texture2X, texture2Y;
	public DoubleTexturedParticle(Point3d location, double mass, double dampening,
			double texture1X, double texture1Y, double texture2X, double texture2Y){
		super(location, mass, dampening);
		this.texture1X = texture1X;
		this.texture1Y = texture1Y;
		this.texture2X = texture2X;
		this.texture2Y = texture2Y;
	}
}
