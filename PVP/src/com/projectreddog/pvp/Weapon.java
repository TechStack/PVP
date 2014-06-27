package com.projectreddog.pvp;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
	private int spawnInterval;
	private Material pickupBlockMaterial;
	
	public Weapon(Location location, Material material, int amt, int dmg, String name, int spwnIntvl, Material blockMaterial) {
		this.weaponLocation = location;
		this.weaponMaterial = material;
		this.amount = amt;
		this.damage = dmg;
		this.spawnInterval = spwnIntvl;
		this.pickupBlockMaterial = blockMaterial;
		
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
	
	public int getSpawnInterval() {
		return ((int) (spawnInterval*20));
	}
	
	public Material getRespawnBlockMaterial() {
		return pickupBlockMaterial;
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

	public void spawnBlock() 
	{
		weaponLocation.getBlock().setType(pickupBlockMaterial);
	}

	public void upgradeWeapon(Player player, Material materialToBeReplaced) {
		
		PlayerInventory playerInventory = player.getInventory();
		
		if( playerInventory.contains(materialToBeReplaced) )
		{
			playerInventory.remove(new ItemStack(materialToBeReplaced));
			
			playerInventory.addItem(weaponItemStack);
		}
		else
		{
			player.getInventory().addItem(weaponItemStack);
		}
	}
	
}
