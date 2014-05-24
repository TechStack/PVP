package com.projectreddog.pvp;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class Weapon {
	
	/**
	 *  Weapons on Map.
	 *   - Create weapon object
	 *   - Start spawning with processWeapon on each Weapon Object.
	 *   - Process weapons each timeTicked()
	 *   - Track when weapon is picked up and call pickedUp()
	 */
	
	private Location weaponLocation;
	private Material weaponMaterial;
	private int amount, damage;
	private ItemStack weaponItemStack;
	private int respawnTimeRemaining;
	private int spawnInterval;
	
	public Weapon(Location location, Material material, int amt, int dmg, String name, int spwnIntvl) {
		this.weaponLocation = location;
		this.weaponMaterial = material;
		this.amount = amt;
		this.damage = dmg;
		this.respawnTimeRemaining = spwnIntvl;
		this.spawnInterval = spwnIntvl;
		
		if(damage != 0)
		{		
			weaponItemStack = new ItemStack(weaponMaterial, amount, (short) damage);
		}
		else
		{
			weaponItemStack = new ItemStack(weaponMaterial, amount);
		}
		
		//ItemMeta itemMeta = null;
		ItemMeta itemMeta = weaponItemStack.getItemMeta();
		itemMeta.setDisplayName(name);
		weaponItemStack.setItemMeta(itemMeta);
	}
	
	public Location getWeaponLocation() {
		return weaponLocation;
	}
	
	public void setWeaponItemStack(ItemStack inWeapon) {
		weaponItemStack = inWeapon;
	}
	
	public ItemStack getWeaponItemStack() {
		return weaponItemStack;
	}
	
	public void pickedUp() {
		respawnTimeRemaining = spawnInterval*20;
		weaponLocation.getBlock().setType(Material.AIR);
	}
	
	public void weaponVisual(int shortTick, int shortLimit, int longTick, int longLimit) {
		/**
		 *  Show visual effects at the weapon location.
		 */
		Location tempLocation = new Location(weaponLocation.getWorld(), weaponLocation.getX(), weaponLocation.getY(), weaponLocation.getZ());
		
		if( longTick >= longLimit)
		{
			Bukkit.getWorld("world").playEffect(tempLocation, Effect.ENDER_SIGNAL, null);
		}
		
		if( shortTick >= shortLimit)
		{
			Bukkit.getWorld("world").playEffect(tempLocation, Effect.SMOKE, 4);
			tempLocation.add(0, 1, 0);
			Bukkit.getWorld("world").playEffect(tempLocation, Effect.SMOKE, 4);
		}
	}
	
	public void processWeapon() {
		/**
		 *  If respawn timer has a value > 0.
		 *   - Decrement Weapon Respawn timer.
		 *   - Flag that it needs respawned.
		 */
		if( respawnTimeRemaining > 0 )
		{
			respawnTimeRemaining -= 10;
			
			/**
			 *  If respawn timer gets to 0, respawn weapon in cobweb.
			 */
			if( respawnTimeRemaining <= 0 )
			{
				weaponLocation.getBlock().setType(Material.WEB);
				Bukkit.getWorld("World").dropItem(weaponLocation, weaponItemStack).setVelocity(new Vector());
			}
		}
		else
		{
			/**
			 *  Get entities near weapon location.  Check if entity is the weapon.
			 *   - Set time lived to 0 to prevent de-spawning.
			 *   - Set velocity to 0 (or slightly positive in Vertical)
			 *   - Set location to weaponLocation
			 */
			Boolean weaponFound = false;
			Arrow tempArrow = Bukkit.getWorld("world").spawnArrow(weaponLocation, new Vector(), 0, 0);
			List<Entity> nearbyEntities = tempArrow.getNearbyEntities(2, 2, 2);
			
			for(Entity e : nearbyEntities) {
			    if( e instanceof ItemStack )
			    {
			    	if( ((ItemStack)e).hasItemMeta())
		    		{
		    			if( ((ItemStack) e).getItemMeta().hasDisplayName() )
		    			{
		    				if( ((ItemStack) e).getItemMeta().getDisplayName().equals(weaponItemStack.getItemMeta().getDisplayName()) )
		    				{
		    					/**
		    					 *  Weapon was found.  Reset life time, velocity, and position.
		    					 */
		    					weaponFound = true;
		    					e.setTicksLived(0);
		    					e.setVelocity(new Vector());
		    					e.teleport(weaponLocation);  //  Check if this causes a visual glitchy effect
		    				}
		    			}
		    		}
			    }
			}
			
			tempArrow.remove();
			
			if( !weaponFound )
			{
				/**
				 *  Weapon was not found within 2 blocks of location.
				 *   - Treat it as if it were picked up.
				 */
				//pickedUp();
			}
		}
	}
	
}
