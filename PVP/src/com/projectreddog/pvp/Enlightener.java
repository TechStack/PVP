package com.projectreddog.pvp;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Enlightener {
	/**
	 *  Enlightener
	 *   - Add enchantments to items based on merit.
	 *
	 *
	 *  Global Variables
	 */
	private List<Material> enchantableSwords = Arrays.asList(Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLD_SWORD, Material.DIAMOND_SWORD);
	private List<Material> enchantableAxes = Arrays.asList(Material.STONE_AXE , Material.IRON_AXE, Material.GOLD_AXE, Material.DIAMOND_AXE);
	private List<Material> enchantableArmor = Arrays.asList(Material.LEATHER_BOOTS, Material.LEATHER_CHESTPLATE, 
			Material.LEATHER_HELMET, Material.LEATHER_LEGGINGS, Material.IRON_BOOTS, Material.IRON_CHESTPLATE, 
			Material.IRON_HELMET, Material.IRON_LEGGINGS, Material.GOLD_BOOTS, Material.GOLD_CHESTPLATE, 
			Material.GOLD_HELMET, Material.GOLD_LEGGINGS, Material.DIAMOND_BOOTS, Material.DIAMOND_CHESTPLATE, 
			Material.DIAMOND_HELMET, Material.DIAMOND_LEGGINGS );
	
	private static final int MAX_LEVEL = 5;
	
	private static Random rand = new Random();
	
	/**
	 *  Constructor - Is this needed if it has no content?
	 */
	public Enlightener() {
		
	}

	public void enlighten(Player p, int enlightenChance) {  //  TODO  Can I make this static and just use it without creating an Enlightener object first?
		/**
		 *  Enchant an item on a player.
		 *   - Highest chance to be weapon in player's hand
		 *      -- 5% chance to be non-hand item (whether held item is currently enchanted or not)
		 *   - Search Inventory 
		 *      -- Armor
		 *      -- Other enchanted items
		 *           ---  Add to current enchant level.
		 *      -- Swords
		 *      -- Axes
		 *      -- Pickaxes?
		 *      -- Bow
		 */
		
		if (chanceToEnlighten(p, enlightenChance))
		{
			/**
			 *  Are there enchantments on the currently held item?
			 */
			ItemStack itemToEnchant = p.getItemInHand();
			
			if( itemToEnchant != null )
			{
				Enchantment[] enchants = getCurrentEnchants(p.getItemInHand());
				
				Enchantment tempEnchantment = null;
				
				if( enchants != null && Math.random()*100 <= 95 )
				{
					/**
					 *  Held Item has current enchantment(s).
					 *   - Pick one at random.
					 */
					tempEnchantment = enchants[randInt(0, enchants.length)];
				}
				else
				{
					/**
					 *  Held Item does not have current enchantment, or no item currently held.
					 *  
					 *  Can the Item normally be enchanted?
					 */
					if( canEnchant(p.getItemInHand())  && Math.random()*100 <= 95 )
					{
						/**
						 *  Held Item can be enchanted.
						 *   - Get a possible enchantment for held item.
						 */
						tempEnchantment = getEchantmentByWeaponType(p.getItemInHand().getType());
					}
					else
					{
						/**
						 *  Held Item cannot be enchanted.
						 *   - Get another item from player inventory.
						 *   - If found, get a possible enchantment for the item.
						 */
						itemToEnchant = searchInventory(p);
						
						if( itemToEnchant != null )
						{
							enchants = getCurrentEnchants(itemToEnchant);
							
							if( enchants != null )
								tempEnchantment = enchants[randInt(0, enchants.length)];
							else
								tempEnchantment = getEchantmentByWeaponType(itemToEnchant.getType());
						}
					}
				}
				
				/**
				 *  If an enchantment was found, add to Item.
				 */
				if( tempEnchantment != null && itemToEnchant != null )
				{			
					addEnchantLevel(tempEnchantment, p, itemToEnchant);
				}
			}
		}
	}	

	public boolean chanceToEnlighten(Player p, int enlightenChance){
		/**
		 *  Get a random number between 0 and 100
		 */
		double tmp = Math.random()*100;
		
		/**
		 *  If the number is less than the enlighten chance then we return true (allow enchantment)
		 */
		if( tmp <= enlightenChance ){
			return true;
		}else 
		{
			return false;
		}
	}
	
	public Enchantment[] getCurrentEnchants(ItemStack itemStack) {
		ItemMeta tempIM = null;
		Map<Enchantment, Integer> currentEnchants = null;
		Enchantment[] enchants = null;
		
		/**
		 *  Does held item already have an enchantment.
		 */
		if( itemStack.hasItemMeta() )
		{
			tempIM = itemStack.getItemMeta();
			
			if( tempIM.hasEnchants() )
			{
				currentEnchants = tempIM.getEnchants();
				
				if( currentEnchants.size() > 0 )
				{
					enchants = new Enchantment[currentEnchants.size()];
					Iterator<Entry<Enchantment, Integer>> it = currentEnchants.entrySet().iterator();

					int i=0;
					while (it.hasNext()) 
					{
						Entry<Enchantment, Integer> entry = it.next();

						enchants[i] = entry.getKey();
						i++;
					}
				}
			}
		}	
		
		return enchants;
	}
	
	private boolean canEnchant(ItemStack itemInHand) {

		if( itemInHand != null )
		{
			/**
			 *  Can Item be enchanted?
			 *   - Is it a sword, axe, or bow?
			 *   - Is it armor?
			 *   - Is it any item with a current enchantment?
			 */
			Material itemMaterial = itemInHand.getType();
			
			if( enchantableSwords.contains(itemMaterial) )
				return true;
			else if( enchantableAxes.contains(itemMaterial) )
				return true;
			else if( enchantableArmor.contains(itemMaterial) )
				return true;
			else if( itemMaterial.equals(Material.BOW) )
				return true;
			else if( itemInHand.hasItemMeta() )
			{
				if( itemInHand.getItemMeta().hasEnchants() )
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	private ItemStack searchInventory(Player p) {
		ItemStack[] playerInventory = p.getInventory().getContents();
		ItemStack tempItem = null;
		Boolean validItemFound = false;
		int INVENTORY_ITERATIONS = 10;
		int counter = 0;
		
		/**
		 *  Find another enchantable item from the Player's inventory.
		 *   - Search through a max of 10 iterations
		 */
		while( !validItemFound && counter < INVENTORY_ITERATIONS )
		{
			tempItem = playerInventory[randInt(0, playerInventory.length)];
			
			/**
			 *  If item is a sword, axe, bow, or armor, return it.
			 */
			if( canEnchant( tempItem) )
				validItemFound = true;
				
			counter++;
		}
		
		return tempItem;
	}
	
	public Enchantment getEchantmentByWeaponType(Material material){
		/**
		 *  Return a possible enchantment for the Material. 
		 */
		Enchantment returnEnchant = null;
		
		if( enchantableSwords.contains(material) )
		{
			Enchantment[] possibleEnchants = {Enchantment.DAMAGE_ALL, Enchantment.FIRE_ASPECT, Enchantment.KNOCKBACK};
			
			returnEnchant = possibleEnchants[randInt(0, possibleEnchants.length)];
		}
		else if( enchantableAxes.contains(material) )
		{
			Enchantment[] possibleEnchants = {Enchantment.DAMAGE_ALL, Enchantment.FIRE_ASPECT, Enchantment.KNOCKBACK};
			
			returnEnchant = possibleEnchants[randInt(0, possibleEnchants.length)];
		}
		else if( enchantableArmor.contains(material) )
		{
			Enchantment[] possibleEnchants = {Enchantment.PROTECTION_ENVIRONMENTAL, Enchantment.PROTECTION_EXPLOSIONS, Enchantment.PROTECTION_FALL, 
					Enchantment.PROTECTION_FIRE, Enchantment.PROTECTION_PROJECTILE};
			
			returnEnchant = possibleEnchants[randInt(0, possibleEnchants.length)];
		}
		else if( material.equals(Material.BOW) )
		{
			Enchantment[] possibleEnchants = {Enchantment.ARROW_DAMAGE, Enchantment.ARROW_FIRE, Enchantment.ARROW_KNOCKBACK};
			
			returnEnchant = possibleEnchants[randInt(0, possibleEnchants.length)];
		}
		
		return returnEnchant;
	}
	
	public void addEnchantLevel(Enchantment enchantment, Player player, ItemStack itemStack){
		int currLevel;
		currLevel= itemStack.getEnchantmentLevel(enchantment);
		if (currLevel< MAX_LEVEL){
			try {
				itemStack.addUnsafeEnchantment(enchantment, currLevel+1);
				player.playSound(player.getLocation(), Sound.ANVIL_USE, 1, 1);
				if( currLevel == 0 )
					player.sendMessage("Your skill as a warrior has been proven. You have been granted an enchantment!");
				else
					player.sendMessage("Your skill as a warrior increases. You have been granted a higher enchantment!");
			}
			finally {
				// do nothing if an error only put try catch in to prevent errors in case user switches to another weapon in the time
				//between picking the enchant type till the enchantment is added or if somehow the player killed 2 players close together 
			}
		}
	}
	
	public static int randInt(int min, int max) {
		/**
		 *  Max value is not included as a result.
		 */
		
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
}
