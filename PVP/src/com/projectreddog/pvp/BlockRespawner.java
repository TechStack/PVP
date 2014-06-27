package com.projectreddog.pvp;

import org.bukkit.Location;
import org.bukkit.Material;

public class BlockRespawner {
	
	/**
	 *  Respawn a Block.
	 *   - Location
	 *   - Material
	 *   - Respawn Time
	 */

	public Location respawnLocation;
	public Material blockMaterial;
	public int respawnTime;
	
	
	public BlockRespawner( Location location, Material material, int time )
	{
		this.respawnLocation = location;
		this.blockMaterial = material;
		this.respawnTime = time;
	}
	
	public int processTimer()
	{
		respawnTime--;
		return respawnTime;
	}
	
	public void spawnBlock()
	{
		respawnLocation.getBlock().setType(blockMaterial);
	}
}
