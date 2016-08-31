package weatherpony.seasons.donator;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.vecmath.Point3d;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import weatherpony.seasons.donator.physics.Cloth;

public class DonatorPerks {
	public static final class Perks{
		public static final class Scarf{
			String front, back;
			
			transient boolean loadedTextures;
			public boolean loadTextures(){
				if(!this.loadedTextures){
					this.frontImage = new ResourceLocation(front);
					this.backImage = new ResourceLocation(back);
					this.loadedTextures = true;
				}
				return this.loadedTextures;
			}
			transient ResourceLocation frontImage, backImage;
			transient Cloth cloth;
		}
		public Scarf scarf;
	}
	
	//This method gets called by the main portion of the mod during Forge PostInitialization
	public static void registerPerkRequirements(){
		MinecraftForge.EVENT_BUS.register(new DonatorPerks());
	}
	
	Cloth.AttachmentLocationProvider<EntityPlayer> connector = new Cloth.AttachmentLocationProvider<EntityPlayer>(){
		Point3d startTop = null;
		Point3d startBottom = null;
		Point3d endTop = null;
		EntityPlayer entity;
		@Override
		public void beginNewQuery(EntityPlayer data) {
			this.entity = data;
			Point3d prev = new Point3d(entity.prevPosX,entity.prevPosY,entity.prevPosZ);
            Point3d next = new Point3d(entity.posX,entity.posY,entity.posZ);
            Point3d current = new Point3d(next);
            //current.interpolate(prev, (double)data.v2);
            
            
            
            this.startTop = new Point3d(current);
            startTop.add(new Point3d(.25,.5,.0));
            this.startBottom = new Point3d(current);
            startBottom.add(new Point3d(.25,1.5,.0));
            this.endTop = new Point3d(current);
            endTop.add(new Point3d(.25,.5,.25));
            
		}
		@Override
		public Point3d getStartingTopLocation() {
			return this.startTop;
		}
		@Override
		public Point3d getEndingTopLocation() {
			return this.endTop;
		}
		@Override
		public Point3d getStartingBottomLocation() {
			return this.startBottom;
		}
		@Override
		public List getPotentialCollisionsNear(Point3d location) {
			List<AxisAlignedBB> potentialCollisions = this.entity.worldObj.getCollidingBoundingBoxes(entity, 
					new AxisAlignedBB(location.x-3,location.y-3,location.z-3,
									  location.x+3,location.y+3,location.z+3));
			
			return potentialCollisions;
		}
		@Override
		public Point3d renderAsIfStartingTopAt() {
			return new Point3d();
		}
	};
	@SubscribeEvent
	public void entityUpdate(PlayerTickEvent event){
		if(event.phase == Phase.END){
			Entity entity = event.player;
			Perks perks = getPerks(entity);
			if(perks != null && perks.scarf != null){
				if(perks.scarf.cloth == null){
					if(perks.scarf.loadTextures()){
						perks.scarf.cloth = new Cloth(8,32,
								0.5d, 0.00625d, 3,
								connector,
								perks.scarf.frontImage,perks.scarf.backImage,
								entity);
					}else
						return;
				}
				perks.scarf.cloth.update();
			}
		}
	}
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void postEntityRender(RenderPlayerEvent.Pre event){
		EntityLivingBase entity = event.entityLiving;
		Perks perks = getPerks(entity);
		if(perks != null && perks.scarf != null && perks.scarf.cloth != null){
			double x = event.x;
			double y = event.y;
			double z = event.z;
			float partialTicks = event.partialRenderTick;
			float entityYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
			
			GlStateManager.pushMatrix();
			GlStateManager.disableCull();
			
			//GlStateManager.translate((float)x, (float)y, (float)z);
			//GlStateManager.translate(-(float)x, -(float)y, -(float)z);
			
			//GlStateManager.translate(x, y, z);
			double nx = TileEntityRendererDispatcher.staticPlayerX;
            double ny = TileEntityRendererDispatcher.staticPlayerY;
            double nz = TileEntityRendererDispatcher.staticPlayerZ;
			
			
            GlStateManager.translate(-nx, -ny, -nz);
			
			/*float f = getF(entity, partialTicks);
			GlStateManager.rotate(180.0F - f, 0.0F, 1.0F, 0.0F);
			*/
			GlStateManager.enableRescaleNormal();
			//GlStateManager.scale(-1.0F, -1.0F, 1.0F);
			/*float f = 0.9375F;
        	GlStateManager.scale(f, f, f);*/
			/*float f4 = 0.0625F;
			GlStateManager.translate(0.0F, -1.5078125F, 0.0F);
			*/
			
			GlStateManager.enableAlpha();
			
			//ModelPlayer model = event.renderer.getMainModel();
            //model.b
			
			/*float finalScalant = 0.0625F;
			GlStateManager.scale(finalScalant,finalScalant,finalScalant);
			*/
			//RENDER
			//GlStateManager.disablePolygonOffset();
			Point3d removeDistance = new Point3d(nx, ny, nz);
			GlStateManager.scale(1/64d, 1/64d, 1/64d);
			perks.scarf.cloth.render(partialTicks,removeDistance);
            
			GlStateManager.disableRescaleNormal();
			//GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
	        //GlStateManager.enableTexture2D();
	        //GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
	        //GlStateManager.enableCull();
	        GlStateManager.popMatrix();
		}
	}
	protected float getF(EntityLivingBase entity, float partialTicks){
		float f = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
        float f1 = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
        float f2 = f1 - f;

        if (entity.isRiding() && entity.ridingEntity instanceof EntityLivingBase)
        {
            EntityLivingBase entitylivingbase = (EntityLivingBase)entity.ridingEntity;
            f = this.interpolateRotation(entitylivingbase.prevRenderYawOffset, entitylivingbase.renderYawOffset, partialTicks);
            f2 = f1 - f;
            float f3 = MathHelper.wrapAngleTo180_float(f2);

            if (f3 < -85.0F)
            {
                f3 = -85.0F;
            }

            if (f3 >= 85.0F)
            {
                f3 = 85.0F;
            }

            f = f1 - f3;

            if (f3 * f3 > 2500.0F)
            {
                f += f3 * 0.2F;
            }
        }
        return f;
	}
	protected float interpolateRotation(float par1, float par2, float par3)
    {
        float f;

        for (f = par2 - par1; f < -180.0F; f += 360.0F)
        {
            ;
        }

        while (f >= 180.0F)
        {
            f -= 360.0F;
        }

        return par1 + par3 * f;
    }
	static Map<UUID,Perks> perkMap = new WeakHashMap();
	static Map<Entity,Perks> madeMap = new WeakHashMap();
	
	static Perks perkTest = makeTestPerks();
	private static Perks makeTestPerks(){
		Perks ret = new Perks();
		Perks.Scarf scarf = ret.scarf = new Perks.Scarf();
		scarf.back = "seasonsmod:perktest/scarf_back.png";
		scarf.front = "seasonsmod:perktest/scarf_front.png";
		return ret;
	}
	protected static Perks getPerks(Entity entity){
		if(entity instanceof EntityPlayer)
			return getPerks((EntityPlayer)entity);
		return madeMap.get(entity);//should be null
	}
	public static Perks getPerks(EntityPlayer player){
		if(madeMap.containsKey(player))
			return madeMap.get(player);
		UUID uuid = player.getGameProfile().getId();
		if(perkMap.containsKey(uuid)){
			Perks val = perkMap.get(uuid);
			madeMap.put(player,val);
			return val;
		}
		
		//return null;
		System.err.println("activation perk test for player");
		madeMap.put(player, perkTest);
		return perkTest;
	}
	private static void attachPlayer(EntityPlayer player){
		if(madeMap.containsKey(player))
			return;
		UUID uuid = player.getGameProfile().getId();
		if(perkMap.containsKey(uuid)){
			Perks val = perkMap.get(uuid);
			madeMap.put(player,val);
		}
		
		System.err.println("activation perk test for player");
		perkMap.put(uuid, perkTest);
	}
	public static void attachPerks(Entity entity){
		if(entity instanceof EntityPlayer){
			//attachPlayer((EntityPlayer)entity);
		}
	}
}
