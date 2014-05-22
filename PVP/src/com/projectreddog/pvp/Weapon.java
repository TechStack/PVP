package com.projectreddog.pvp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Weapon {
	
	private Location weaponLocation;
	private Material weaponMaterial;
	private int amount, damage;
	private String name;
	private ItemStack weaponItemStack;
	private int respawnTimeRemaining;
	
	public Weapon(Location location, Material material, int amt, int dmg, String name) {
		this.weaponLocation = location;
		this.weaponMaterial = material;
		this.amount = amt;
		this.damage = dmg;
		this.name = name;
		this.respawnTimeRemaining = 0;
		
		if(damage != 0)
		{		
			weaponItemStack = new ItemStack(weaponMaterial, amount, (short) damage);
		}
		else
		{
			weaponItemStack = new ItemStack(weaponMaterial, amount);
		}
		
		ItemMeta itemMeta = null;
		//ItemMeta itemMeta = weaponItemStack.getItemMeta();
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
	
	public void setRespawnTimer( int setTime ) {
		respawnTimeRemaining = setTime;
	}
	
	public int getRespawnTimer() {
		return respawnTimeRemaining;
	}
	
}
