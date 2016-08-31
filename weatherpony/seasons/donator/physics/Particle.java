package weatherpony.seasons.donator.physics;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
/**
 * This class is taken from the C++ class on http://cg.alexandra.dk/?p=147 by Jesper Mosegaard.
 * Additional changes have been made to satisfy language variations.
 */
public class Particle {
	private boolean movable;
	private final double mass;
	private Point3d pos;
	private Point3d old_pos;
	private Vector3d acceleration;
	private Vector3d accumulated_normal;
	
	private double dampening = 0.01;
	
	public Particle(){
		this.pos = new Point3d();
		this.old_pos = new Point3d();
		this.acceleration = new Vector3d();
		this.accumulated_normal = new Vector3d();
		this.mass = 1;
		this.movable = true;
	}
	public Particle(Point3d location){
		this.pos = new Point3d(location);
		this.old_pos = new Point3d(location);
		this.acceleration = new Vector3d();
		this.accumulated_normal = new Vector3d();
		this.mass = 1;
		this.movable = true;
	}
	public Particle(Point3d location, double mass, double dampening){
		this.pos = new Point3d(location);
		this.old_pos = new Point3d(location);
		this.acceleration = new Vector3d();
		this.accumulated_normal = new Vector3d();
		this.mass = mass;
		this.movable = true;
		this.dampening = dampening;
	}
	public void addForce(Vector3d f){
		Vector3d addedAcceleration = new Vector3d(f);
		addedAcceleration.scale(1/this.mass);
		
		this.acceleration.add(addedAcceleration);
	}
	public void forceMove(Point3d newLocation){
		this.old_pos = this.pos;
		this.pos = new Point3d(newLocation);
	}
	public Point3d getPos(){ return this.pos; }
	public Point3d getPos(double partialTime){
		Point3d ret = new Point3d(this.old_pos);
		ret.interpolate(this.pos, partialTime);
		return ret;
	}
	public void resetAcceleration(){ this.acceleration.set(0, 0, 0); }
	public void offsetPos(Vector3d v) { if(movable) pos.add(v);}
	public void makeUnmovable() {movable = false;}
	public void addToNormal(Vector3d normal){
		Vector3d norm = new Vector3d(normal);
		norm.normalize();
		accumulated_normal.add(norm);
	}
	public Vector3d getNormal() { return accumulated_normal;} // notice, the normal is not unit length
	public void resetNormal() {accumulated_normal.set(0, 0, 0);}
	public void move(){
		if(this.movable){
			Point3d temp = new Point3d(pos);
			//acceleration.scale(1d);
			Point3d dif = new Point3d(pos);
			dif.sub(old_pos);
			dif.scale((1.0-(this.dampening)));
			pos.add(dif);
			pos.add(acceleration);
			
			old_pos = temp;
			
			acceleration.set(0, 0, 0);
		}
	}
}
