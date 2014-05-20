package com.projectreddog.pvp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Weapon {
	
	private Location weaponLocation;
	private Material weaponMaterial;
	private int amount, damage;
	private ItemStack weaponItemStack;
	
	public Weapon(Location location, Material material, int amt, int dmg) {
		this.weaponLocation = location;
		this.weaponMaterial = material;
		this.amount = amt;
		this.damage = dmg;
		
		if(damage != 0)
		{		
			weaponItemStack = new ItemStack(weaponMaterial, amount, (short) damage);
		}
		else
		{
			weaponItemStack = new ItemStack(weaponMaterial, amount);
		}
	}
	
	public Location getWeaponLocation() {
		return weaponLocation;
	}
	
	public ItemStack getWeaponItemStack() {
		return weaponItemStack;
	}
}
