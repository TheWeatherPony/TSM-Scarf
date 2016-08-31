package weatherpony.seasons.donator.physics;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
/**
 * This class is based on C++ class on http://cg.alexandra.dk/?p=147 by Jesper Mosegaard.
 * Many changes have been made, however, both for language differences and usage requirements.
 */
public class Cloth<Info> implements ITickable{
	public interface AttachmentLocationProvider<Info>{
		public void beginNewQuery(Info data);
		public Point3d getStartingTopLocation();
		public Point3d getEndingTopLocation();
		public Point3d getStartingBottomLocation();
		public List getPotentialCollisionsNear(Point3d location);
		
		public Point3d renderAsIfStartingTopAt();
	}
	public static final float TRANSPARANCY = 1f; //0.25f;
	private final int num_particles_width;
	private final int num_particles_height;
	private final List<DoubleTexturedParticle> particles;
	private final ArrayList<Constraint> constraints;
	
	private final int constraintIterations;
	
	private final List<Particle> unmovingParticles;
	private final AttachmentLocationProvider movementProvider;
	
	private final ResourceLocation image1, image2;
	
	private final Info info;
	public Cloth(int num_particles_width, int num_particles_height,
				double mass, double springDampening, int constraintIterations,
				AttachmentLocationProvider<Info> movementProvider,
				ResourceLocation imageFront, ResourceLocation imageBack,
				Info info){
		this.info = info;
		this.movementProvider = movementProvider;
		
		movementProvider.beginNewQuery(info);
		Point3d startingTop = movementProvider.getStartingTopLocation();
		Point3d endingTop = movementProvider.getEndingTopLocation();
		Point3d startingBottom = movementProvider.getStartingBottomLocation();
		
		this.num_particles_width = num_particles_width;
		this.num_particles_height = num_particles_height;
		this.particles = new ArrayList(num_particles_width * num_particles_height);
		this.constraints = new ArrayList();
		this.constraintIterations = constraintIterations;
		this.image1 = imageFront;
		this.image2 = imageBack;
		Vector3d xMov = new Vector3d();
		xMov.sub(startingTop, endingTop);
		xMov.scale(1d/num_particles_width);
		Vector3d yMov = new Vector3d();
		yMov.sub(startingTop, startingBottom);
		yMov.scale(1d/num_particles_height);
		
		for(int x=0; x<num_particles_width; x++){
			Point3d row = new Point3d(xMov);
			row.scale(x);
			row.add(startingTop);
			for(int y=0; y<num_particles_height; y++){
				Point3d point = new Point3d(yMov);
				point.scale(y);
				point.add(row);
				
				DoubleTexturedParticle particlePoint = new DoubleTexturedParticle(point, mass, springDampening,
												(x/(double)num_particles_width), (y / (double)num_particles_height),
												1 - (x/(double)num_particles_width), 1 - (y / (double)num_particles_height));
				//this.particles.set((y * num_particles_width + x), particlePoint);
				this.particles.add(particlePoint);
			}
		}
		
		// Connecting immediate neighbor particles with constraints (distance 1 and sqrt(2) in the grid)
		for(int x=0; x<num_particles_width; x++)
		{
			for(int y=0; y<num_particles_height; y++)
			{
				if (x<num_particles_width-1) this.constraints.add(new Constraint(getParticle(x,y),getParticle(x+1,y)));
				if (y<num_particles_height-1) this.constraints.add(new Constraint(getParticle(x,y),getParticle(x,y+1)));
				if (x<num_particles_width-1 && y<num_particles_height-1) this.constraints.add(new Constraint(getParticle(x,y),getParticle(x+1,y+1)));
				if (x<num_particles_width-1 && y<num_particles_height-1) this.constraints.add(new Constraint(getParticle(x+1,y),getParticle(x,y+1)));
			}
		}
		// Connecting secondary neighbors with constraints (distance 2 and sqrt(4) in the grid)
		for(int x=0; x<num_particles_width; x++)
		{
			for(int y=0; y<num_particles_height; y++)
			{
				if (x<num_particles_width-2) this.constraints.add(new Constraint(getParticle(x,y),getParticle(x+2,y)));
				if (y<num_particles_height-2) this.constraints.add(new Constraint(getParticle(x,y),getParticle(x,y+2)));
				if (x<num_particles_width-2 && y<num_particles_height-2) this.constraints.add(new Constraint(getParticle(x,y),getParticle(x+2,y+2)));
				if (x<num_particles_width-2 && y<num_particles_height-2) this.constraints.add(new Constraint(getParticle(x+2,y),getParticle(x,y+2)));			}
		}
		
		constraints.trimToSize();
		
		this.unmovingParticles = new ArrayList(num_particles_width);
		for(int x=0; x<num_particles_width; x++){
			Particle point = this.getParticle(x, 0);
			point.makeUnmovable();
			this.unmovingParticles.add(point);
		}
	}
	
	private DoubleTexturedParticle getParticle(int x, int y){ return this.particles.get(y * this.num_particles_width + x); }
	
	/* A private method used by drawShaded() and addWindForcesForTriangle() to retrieve the  
	normal vector of the triangle defined by the position of the particles p1, p2, and p3.
	The magnitude of the normal vector is equal to the area of the parallelogram defined by p1, p2 and p3
	*/
	private Vector3d calcTriangleNormal(Particle p1,Particle p2,Particle p3, double partialTick)
	{
		Point3d pos1 = p1.getPos(partialTick);
		Point3d pos2 = p2.getPos(partialTick);
		Point3d pos3 = p3.getPos(partialTick);

		Vector3d v1 = new Vector3d(pos2);
		v1.sub(pos1);
		Vector3d v2 = new Vector3d(pos3);
		v2.sub(pos1);

		v1.cross(v1, v2);
		return v1;
	}
	/* A private method used by windForce() to calculate the wind force for a single triangle 
	defined by p1,p2,p3*/
	private void addWindForcesForTriangle(Particle p1,Particle p2,Particle p3, Vector3d direction)
	{
		Vector3d normal = calcTriangleNormal(p1,p2,p3,.5);
		Vector3d d = new Vector3d(normal);
		d.normalize();
		d.dot(direction);
		normal.scale(d.dot(direction));
		p1.addForce(normal);
		p2.addForce(normal);
		p3.addForce(normal);
	}
	/* A private method used by drawShaded(), that draws a single triangle p1,p2,p3 with a color*/
	private void drawTriangle(DoubleTexturedParticle p1, DoubleTexturedParticle p2, DoubleTexturedParticle p3, boolean side2, Point3d removeDistance)
	{
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		Point3d p1p = p1.getPos();
		Point3d p2p = p2.getPos();
		Point3d p3p = p3.getPos();
		Vector3d p1n = p1.getNormal();
		Vector3d p2n = p2.getNormal();
		Vector3d p3n = p3.getNormal();
		
		p1p.sub(removeDistance);
		p2p.sub(removeDistance);
		p3p.sub(removeDistance);
		
		p1p.scale(64);
		p2p.scale(64);
		p3p.scale(64);
		/*DoubleBuffer test = BufferUtils.createDoubleBuffer(16);
		GL11.glGetDouble(GL11.GL_MODELVIEW_MATRIX, test);
		test.rewind();
		double[] buffer = new double[16];
		test.get(buffer);
		Matrix4d mat = new Matrix4d(buffer);
		Point3d p1pc = new Point3d(p1p);
		mat.transform(p1pc);
		System.out.println("rendering point... "+p1pc.x+','+p1pc.y+','+p1pc.z);
		*/
		//GL11.GL_POLYGON
		if(side2){//.GL_TRIANGLES
			worldrenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
			worldrenderer.pos(p1p.x, p1p.y, p1p.z).tex(p1.texture1X, p1.texture1Y).normal((float)p1n.x, (float)p1n.y, (float)p1n.z).endVertex();
			worldrenderer.pos(p2p.x, p2p.y, p2p.z).tex(p2.texture1X, p2.texture1Y).normal((float)p2n.x, (float)p2n.y, (float)p2n.z).endVertex();
			worldrenderer.pos(p3p.x, p3p.y, p3p.z).tex(p3.texture1X, p3.texture1Y).normal((float)p3n.x, (float)p3n.y, (float)p3n.z).endVertex();
			tessellator.draw();
		}else{
			worldrenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
			worldrenderer.pos(p3p.x, p3p.y, p3p.z).tex(p3.texture2X, p3.texture2Y).normal((float)p3n.x, (float)p3n.y, (float)p3n.z).endVertex();
			worldrenderer.pos(p2p.x, p2p.y, p2p.z).tex(p2.texture2X, p2.texture2Y).normal((float)p2n.x, (float)p2n.y, (float)p2n.z).endVertex();
			worldrenderer.pos(p1p.x, p1p.y, p1p.z).tex(p1.texture2X, p1.texture2Y).normal((float)p1n.x, (float)p1n.y, (float)p1n.z).endVertex();
			tessellator.draw();
		}
	}
	
	/* drawing the cloth as a smooth shaded OpenGL triangular mesh
	Called from the display() method
	The cloth is seen as consisting of triangles for four particles in the grid as follows:

	(x,y)   *--* (x+1,y)
	        | /|
	        |/ |
	(x,y+1) *--* (x+1,y+1)

	*/
	void drawShaded(double partialTick, Point3d removeDistance)
	{
		// reset normals (which where written to last frame)
		for(Particle each : this.particles)
			each.resetNormal();

		//create smooth per particle normals by adding up all the (hard) triangle normals that each particle is part of
		for(int x = 0; x<num_particles_width-1; x++)
		{
			for(int y=0; y<num_particles_height-1; y++)
			{
				Vector3d normal = calcTriangleNormal(getParticle(x+1,y),getParticle(x,y),getParticle(x,y+1),partialTick);
				this.getParticle(x+1,y).addToNormal(normal);
				this.getParticle(x,y).addToNormal(normal);
				this.getParticle(x,y+1).addToNormal(normal);

				normal = calcTriangleNormal(getParticle(x+1,y+1),getParticle(x+1,y),getParticle(x,y+1),partialTick);
				this.getParticle(x+1,y+1).addToNormal(normal);
				this.getParticle(x+1,y).addToNormal(normal);
				this.getParticle(x,y+1).addToNormal(normal);
			}
		}

		//GlStateManager.disableAlpha();
        GlStateManager.enableDepth();//.disableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, TRANSPARANCY);
        Minecraft mc = Minecraft.getMinecraft();
        
        mc.getTextureManager().bindTexture(this.image1);
		for(int x = 0; x<num_particles_width-1; x++)
		{
			for(int y=0; y<num_particles_height-1; y++)
			{
				drawTriangle(getParticle(x+1,y),getParticle(x,y),getParticle(x,y+1), false, removeDistance);
				drawTriangle(getParticle(x+1,y+1),getParticle(x+1,y),getParticle(x,y+1), false, removeDistance);
			}
		}
		
		mc.getTextureManager().bindTexture(this.image2);
		for(int x = 0; x<num_particles_width-1; x++)
		{
			for(int y=0; y<num_particles_height-1; y++)
			{
				drawTriangle(getParticle(x+1,y),getParticle(x,y),getParticle(x,y+1), true, removeDistance);
				drawTriangle(getParticle(x+1,y+1),getParticle(x+1,y),getParticle(x,y+1), true, removeDistance);
			}
		}
		//glEnd();*///FIXME
	}
	private float lastPartial = 0;
	private long lastWhole = -1;
	public void render(double partial, Point3d removeDistance){
		this.drawShaded(partial, removeDistance);
	}
	void timeStep()
	{
		if(this.movementProvider != null){
			this.movementProvider.beginNewQuery(this.info);
			Point3d startingAnchor = this.movementProvider.getStartingTopLocation();
			Point3d endingAnchor = this.movementProvider.getEndingTopLocation();
			Vector3d step = new Vector3d();
			step.sub(startingAnchor, endingAnchor);
			step.scale(num_particles_width);
			
			for(Particle eachAnchored : this.unmovingParticles){
				eachAnchored.forceMove(startingAnchor);
				startingAnchor.add(step);
			}
		}

		List potentialCollisions = this.getLocalCollisions();
		for(int i=0; i<constraintIterations; i++) // iterate over all constraints several times
		{
			for(Constraint eachConstraint : this.constraints)
			{
				eachConstraint.satisfyConstraint(); // satisfy constraint.
			}
			worldlyCollisions(potentialCollisions);
			//ballCollision(ball_pos, ball_radius);
		}

		for(Particle eachParticle : this.particles){
			eachParticle.move();
		}
		worldlyCollisions(potentialCollisions);
		//ballCollision(ball_pos, ball_radius);
	}
	
	/* used to add gravity (or any other arbitrary vector) to all particles*/
	void addForce(Vector3d direction){
		for(Particle each : this.particles){
			each.addForce(direction);
		}
	}
	
	/* used to add wind forces to all particles, is added for each triangle since the final force is proportional to the triangle area as seen from the wind direction*/
	void windForce(Vector3d direction)
	{
		for(int x = 0; x<num_particles_width-1; x++)
		{
			for(int y=0; y<num_particles_height-1; y++)
			{
				addWindForcesForTriangle(getParticle(x+1,y),getParticle(x,y),getParticle(x,y+1),direction);
				addWindForcesForTriangle(getParticle(x+1,y+1),getParticle(x+1,y),getParticle(x,y+1),direction);
			}
		}
	}
	private List getLocalCollisions(){
		return this.movementProvider == null ? null : this.movementProvider.getPotentialCollisionsNear(this.getParticle(this.num_particles_width /2, 0).getPos());
	}
	private void worldlyCollisions(List potentialCollisions){
		if(potentialCollisions == null || potentialCollisions.isEmpty())
			return;
		for(Object eachPotentialCollision : potentialCollisions){
			if(eachPotentialCollision instanceof AxisAlignedBB){
				AxisAlignedBB potential = (AxisAlignedBB)eachPotentialCollision;
				AABBCollision(potential);
			}
		}
	}
	private void AABBCollision(AxisAlignedBB box){
		for(Particle eachParticle : this.particles){
			Point3d point = eachParticle.getPos();
			Vec3 vec3point = new Vec3(point.x,point.y,point.z);
			if(box.isVecInside(vec3point)){
				
			}
		}
	}
	private void ballCollision(Vector3d center, double radius)
	{
		for(Particle eachParticle : this.particles)
		{
			Vector3d v = new Vector3d(eachParticle.getPos());
			v.sub(center);
			double l = v.length();
			if ( v.length() < radius) // if the particle is inside the ball
			{
				v.normalize();
				v.scale(radius-l);
				eachParticle.offsetPos(v); // project the particle to the surface of the ball
			}
		}
	}

	@Override
	public void update() {
		this.timeStep();
	}
}