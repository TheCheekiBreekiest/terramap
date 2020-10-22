package fr.thesmyler.terramap.network.playersync;

import java.util.UUID;

import fr.thesmyler.terramap.TerramapMod;
import fr.thesmyler.terramap.TerramapRemote;
import fr.thesmyler.terramap.TerramapUtils;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TerramapLocalPlayer extends TerramapPlayer {

	protected EntityPlayer player;
	
	public TerramapLocalPlayer(EntityPlayer player) {
		this.player = player;
	}
	
	@Override
	public UUID getUUID() {
		return this.player.getPersistentID();
	}

	@Override
	public ITextComponent getDisplayName() {
		return this.player.getDisplayName();
	}

	@Override
	public double[] getGeoCoordinates() {
		GeographicProjection proj;
		if(this.player.world.isRemote) {
			proj = TerramapRemote.getRemote().getProjection();
		} else {
			proj = TerramapUtils.getEarthGeneratorSettingsFromWorld(this.player.world).getProjection();
		}
		if(proj == null) return new double[] {Double.NaN, Double.NaN};
		return proj.toGeo(this.player.posX, this.player.posZ);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ResourceLocation getSkin() {
		return ((AbstractClientPlayer)this.player).getLocationSkin();
	}
	
	public EntityPlayer getPlayer() {
		return this.player;
	}

	@Override
	public GameType getGamemode() {
		return TerramapMod.proxy.getGameMode(this.player);
	}

	@Override
	public float getAzimut() {
		GeographicProjection proj = TerramapUtils.getEarthGeneratorSettingsFromWorld(this.player.world).getProjection();
		if(proj == null) return Float.NaN;
		//TODO Implement TerramapLocalPlayer::getAzimut
		return 0;
	}

}
