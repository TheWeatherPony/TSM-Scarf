package weatherpony.seasons.donator.physics;

import javax.vecmath.Vector3d;
/**
 * This class is taken from the C++ class on http://cg.alexandra.dk/?p=147 by Jesper Mosegaard.
 * Additional changes have been made to satisfy language variations.
 */
public class Constraint {
	private final double rest_distance;// the length between particle p1 and p2 in rest configuration
	private final Particle p1, p2;
	
	public Constraint(Particle p1, Particle p2){
		this.p1 = p1;
		this.p2 = p2;
		this.rest_distance = p1.getPos().distance(p2.getPos());
	}
	/* This is one of the important methods, where a single constraint between two particles p1 and p2 is solved
	the method is called by Cloth.time_step() many times per frame*/
	void satisfyConstraint()
	{
		Vector3d p1_to_p2 = new Vector3d(p2.getPos());
		p1_to_p2.sub(p1.getPos());
		
		double current_distance = p1_to_p2.length(); // current distance between p1 and p2
		p1_to_p2.scale(1 - rest_distance/current_distance); // The offset vector that could moves p1 into a distance of rest_distance to p2
		p1_to_p2.scale(0.5); // Lets make it half that length, so that we can move BOTH p1 and p2.
		p1.offsetPos(p1_to_p2); // correctionVectorHalf is pointing from p1 to p2, so the length should move p1 half the length needed to satisfy the constraint.
		p1_to_p2.negate();
		p2.offsetPos(p1_to_p2); // we must move p2 the negative direction of correctionVectorHalf since it points from p2 to p1, and not p1 to p2.	
	}
}
