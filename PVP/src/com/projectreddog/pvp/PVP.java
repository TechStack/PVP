
/*************************************************************************
 * 
 * CONFIDENTIAL
 * __________________
 * 
 *  [2013] - [2014] Dustin Frysinger AKA TechStack
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Dustin Frysinger if any.  
 * The intellectual and technical concepts contained
 * herein are proprietary may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Dustin Frysinger.
 */

package com.projectreddog.pvp;                 //   TODO   ------------------ Add Kill Streaks Code from DeathCube.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;


/**
 * 
 *  This plugin will contain the following:
 * 
 *    - Game States:
 *       -- Lobby
 *       -- Warm-up
 *       -- Running
 *       -- Game Over
 *    - Free-for-All PVP Combat
 *    - Team PVP Combat
 *    - Spawn Points
 *       -- Safe spawn points
 *    - Power-ups  TODO
 *       -- Add Effects or Enchantments
 *       -- Visual Effect / Collectible
 *    - Weapons on Map (includes Items)
 *       -- Customizable via Config
 *           --- Item Abilities
 *           --- Item Locations
 *           --- Item Respawn (and Duration?)
 *    - Potions  TODO
 *    - Special Points  TODO
 *       -- Armor Upgrade
 *       -- Effect
 *       -- Enchanting
 *    - Cooldown for all item and special points. 
 *    - Scoring
 *       -- Scoreboard
 *       -- Game Over Limits
 *           --- Time
 *           --- Kills
 *           --- Default (and customizable in-game)
 *           --- Penalties (TODO Suicide Only)
 *
 */

public class PVP extends JavaPlugin implements Listener{ 


	private static enum GameStates{
		Lobby,GameWarmup,Running,GameOver
	}
	/**
	 *  World and Location Variables
	 */
	public World world;
	public Logger logger;
	public float cubeMaxX;
	public float cubeMinX;
	public float cubeMaxZ;
	public float cubeMinZ;
	private Location lobbySpawn;
	private Location[] spawnPoints;
	private Weapon[] weapons;
	private String MapName;
	private String gameMode;
	
	/**
	 *  General Game Variables
	 */
	private static String ACTIVE_FORCE_FIELD; 
	private static int SECONDS_TO_END_GAME;
	private static int KILLS_TO_WIN_GAME;
	private static final int MAX_TEAMS = 4;
	public int enlightenChance; 
	private int numSpawnPoints;
	private int numWeapons;
	private int gameSecondsCount = 0;
	private int timeTickedTimer = 1;
	private int leadingKillsScore = 0;
	private Player leadingPlayer;
	private Team leadingTeam;
	private Statistics gameStats;
	private Enlightener enlightener;
	
	/**
	 *  Weapon Variables
	 */
	private static List<BlockRespawner> respawningWeapons;
	private static Material WEAPON_PICKUP_BLOCK = Material.WEB;
	
	/**
	 *  Test Item Variables
	 */
	private Map<Location, Integer> clickableBlocks;
	private Map<Location, Integer> respawningBlocks;
	private static int BONUS_BLOCK_RESPAWN_TIME;
	private static int MAX_BONUS_BLOCK_CLICKS;
	private static int NUM_EMERALDS_FOR_POINT;
	private static Material BONUS_MATERIAL;
	private static List<Item> effectItems;
	
	/**
	 *  Team Variables
	 */
	public Team[] teamsArray;
	public int numTeams;
	private Map<String, String> playerTeam ;
	public static Random rand = new Random();
	
	/**
	 *  TimeTicked() Variables
	 */
	private GameStates GameState;
	private int TimeTillStart=300;
	private int coolDownTimer=10;
	private BukkitTask task ;

	/**
	 *  Visualization Variables
	 */
	private static final int SHORT_TICK_LIMIT = 1;
	private static final int LONG_TICK_LIMIT = 3;
	private int shortTick = SHORT_TICK_LIMIT;
	private int longTick = LONG_TICK_LIMIT;
	
	/**
	 *  Scoreboard Variables
	 */
	Objective kills, teamKills;
	ScoreboardManager manager, teamManager;
	Scoreboard killsBoard, teamBoard;
	org.bukkit.scoreboard.Team[] SBTeam;
	private Score score;	
	
	public void onEnable() {

		/**
		 * ======================================  Main Game Setup ======================================
		 */
		GameState = GameStates.Lobby;
		playerTeam = new HashMap<String, String>();
		clickableBlocks = new HashMap<Location, Integer>();
		respawningBlocks = new ConcurrentHashMap<Location, Integer>();
		effectItems = new CopyOnWriteArrayList<Item>();
		respawningWeapons = new CopyOnWriteArrayList<BlockRespawner>();

		getServer().getPluginManager().registerEvents(this, this);
		logger = this.getLogger();

		this.saveDefaultConfig();

		/**
		 *  Bounding box for the game force field		
		 */
		cubeMaxX = this.getConfig().getInt("cubeMaxX");
		cubeMinX = this.getConfig().getInt("cubeMinX");
		cubeMaxZ = this.getConfig().getInt("cubeMaxZ");
		cubeMinZ = this.getConfig().getInt("cubeMinZ");
		getLogger().info("MAX X: " + cubeMaxX);
		getLogger().info("Min X: " + cubeMinX);
		getLogger().info("MAX Z: " + cubeMaxZ);
		getLogger().info("Min Z: " + cubeMinZ);
		
		double lobbyX, lobbyY, lobbyZ;
		lobbyX = centerBlockValue(this.getConfig().getInt("LobbySawn.X"));
		lobbyY = this.getConfig().getInt("LobbySawn.Y");
		lobbyZ = centerBlockValue(this.getConfig().getInt("LobbySawn.Z"));
		lobbySpawn = new Location(Bukkit.getWorld("World"), lobbyX, lobbyY, lobbyZ);
		
		/**
		 *  Game Options Setup
		 */
		ACTIVE_FORCE_FIELD = this.getConfig().getString("ActiveForceField");
		gameMode = this.getConfig().getString("GameMode");
		KILLS_TO_WIN_GAME = this.getConfig().getInt("KillsToWinGame");
		SECONDS_TO_END_GAME = this.getConfig().getInt("SecondsToEndGame");
		enlightenChance = this.getConfig().getInt("EnlightenChance");
		numTeams = this.getConfig().getInt("NumTeams");
		BONUS_BLOCK_RESPAWN_TIME = this.getConfig().getInt("BonusBlockRespawnTime");
		MAX_BONUS_BLOCK_CLICKS = this.getConfig().getInt("MaxBonusBlockClicks");
		NUM_EMERALDS_FOR_POINT = this.getConfig().getInt("NumEmeralsForPoint");
		BONUS_MATERIAL = Material.getMaterial(this.getConfig().getString("BonusEmeraldMaterial"));
		
		
		/**
		 * ======================================  Spawn Points Setup ======================================
		 *  - Get number of points
		 *  - Create array of Locations to be used to choose a spawn point on Player respawn
		 *  - From the config file, create locations for each point and add to the array
		 */
		numSpawnPoints = this.getConfig().getInt("NumSpawnPoints");
		spawnPoints = new Location[numSpawnPoints];
		double pointX, pointY, pointZ, pointYaw;
		
		for( int i=1; i <= numSpawnPoints; i++ ) {
			pointX = centerBlockValue(this.getConfig().getDouble("RespawnPoint" + i + ".X"));
			pointY = this.getConfig().getDouble("RespawnPoint" + i + ".Y");
			pointZ = centerBlockValue(this.getConfig().getDouble("RespawnPoint" + i + ".Z"));
			spawnPoints[i-1] = new Location(Bukkit.getWorld("World"), pointX, pointY, pointZ);
			
			pointYaw = this.getConfig().getDouble("RespawnPoint" + i + ".Yaw");
			if(Math.abs(pointYaw - 1.0) < 0.0000001)
			{
				/**
				 *  Calculate Yaw from Spawn location to Map Center
				 *  
				 *  DeathCube Center Point
				 */
				double cubeCenterX = cubeMinX + (int) ((cubeMaxX - cubeMinX)/2);
				double cubeCenterZ = cubeMinZ + (int) ((cubeMaxZ - cubeMinZ)/2);
			    
			    /**
			     *  Yaw - to degrees, plus 90 for Minecraft
			     */
			    pointYaw = Math.atan2( (pointZ - cubeCenterZ), (pointX - cubeCenterX) );
				pointYaw = pointYaw * 180.0 / Math.PI;
				pointYaw += 90f;
			}
			spawnPoints[i-1].setYaw((float) pointYaw);
		}
		
		
		/**
		 * ======================================  On-Map Weapons Setup ======================================
		 *  - Get number of on-map weapons
		 *  - Create array of Weapon objects to be used to spawn and track the Weapon
		 *  - From the config file, create weapon object and add to the array
		 */
		numWeapons = this.getConfig().getInt("NumWeapons");
		weapons = new Weapon[numWeapons];
		Location weaponLocation;
		
		for( int i=1; i <= numWeapons; i++ ) {
			/**
			 *  Get Location
			 */
			pointX = centerBlockValue(this.getConfig().getDouble("Weapon" + i + ".X"));
			pointY = this.getConfig().getDouble("Weapon" + i + ".Y");
			pointZ = centerBlockValue(this.getConfig().getDouble("Weapon" + i + ".Z"));
			weaponLocation = new Location(Bukkit.getWorld("World"), pointX, pointY, pointZ);
			
			int amount = 1;
			int damage = 0;
			ItemStack itemStack;
			
			/**
			 *  Get Material
			 */
			Material material = Material.getMaterial(this.getConfig().getString("Weapon" + i + ".Name"));
			
			/**
			 *  Check for Amount
			 */
			if(this.getConfig().getInt("Weapon" + i + ".Amount") != 1)
				amount = this.getConfig().getInt("Weapon" + i + ".Amount");
			
			/**
			 *  Check for Damage Value
			 */
			if(this.getConfig().getInt("Weapon" + i + ".Damage") != 0)
			{
				damage = this.getConfig().getInt("Weapon" + i + ".Damage");
			}
			
			/**
			 *  Get Display Name
			 */
			String displayName = this.getConfig().getString("Weapon" + i + ".DisplayName");
			
			/**
			 *  Get Display Name
			 */
			int spawnInterval = this.getConfig().getInt("Weapon" + i + ".SpawnInterval");
			
			/**
			 *  Create Weapon Object
			 */
			weapons[i-1] = new Weapon(weaponLocation, material, amount, damage, displayName, spawnInterval, WEAPON_PICKUP_BLOCK);
			
			/**
			 *  Enchant Item
			 */
			String configPrefix = "Weapon" + i;
			ItemStack tempIS = weapons[i-1].getWeaponItemStack();
			enchantItem(configPrefix, tempIS);
			weapons[i-1].setWeaponItemStack(tempIS);
		}
		
		/**
		 *  Spawn Weapon Blocks on Map
		 */
		for( Weapon w : weapons )
		{
			w.spawnBlock();
		}
		
		
		/**
		 *  ======================================  Scoring Setup ======================================
		 *   - Scoreboard Setup
		 */
		manager = Bukkit.getScoreboardManager();
		killsBoard = manager.getNewScoreboard();
		
		teamManager = Bukkit.getScoreboardManager();
		teamBoard = teamManager.getNewScoreboard();
		
		kills = killsBoard.registerNewObjective("kills", "dummy");
		kills.setDisplaySlot(DisplaySlot.SIDEBAR);
		kills.setDisplayName("Kills:");
		
		teamKills = teamBoard.registerNewObjective("teamKills", "dummy");
		teamKills.setDisplaySlot(DisplaySlot.SIDEBAR);
		teamKills.setDisplayName("Team Kills:");

		MapName = this.getConfig().getString("MapName");
		logger.info("Map Name: " + MapName);
		task= new PVPRunnable(this).runTaskTimer(this, 10, 10);
	}
	
	public void onDisable(){
		task.cancel();
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		
		if (GameState == GameStates.Running){
			Player killer = null;
			Player victim = e.getEntity();
			
			if(victim.getKiller() instanceof Player)
				killer = victim.getKiller();
			
			if ( killer != null ){
				/**
				 *  A player killed another player.  Allow chance to enlighten the killer.
				 *  
				 *  Also, increment scoreboard (Kills).
				 *   - When the Killer is not also the Victim
				 */
				
				enlightener.enlighten(killer, (enlightenChance + 5*gameStats.getMultiplier(killer)));
				
				if( !killer.getName().equals(victim.getName()) )
					updateScore(killer, 1);
				else
				{
					killer.sendMessage("This is considered suicide ..."); 
					updateScore(killer, -1);
				}
			}
			else
			{
				/**
				 *  Decrement Score for this Player
				 *   - Death Reason: Suicide or non-Player Killer
				 *   TODO Make Decrement for Suicide Only
				 *   - Does this need special handling for teams?
				 */
				
				updateScore(victim, -1);
				
			}
			
			/**
			 *  Update Game Statistics
			 */
			gameStats.updateStatistics(victim, killer);
			
			ArrayList<ItemStack>is =new ArrayList<ItemStack>();

			for (ItemStack drop : e.getDrops())
			{
				if (drop.getType() != Material.COBBLESTONE)
				{
					is.add(drop);
				}
			}
			e.getDrops().removeAll( is);
		}
	}

	@EventHandler
	public void onBlockPlace (BlockPlaceEvent e){
		if (e.getPlayer().getGameMode()== GameMode.CREATIVE){
			/**
			 *  Allow any block placement, if Creative Mode
			 */
		}else{
			/**
			 *  Otherwise, allow only Cobblestone block placement.
			 */
			if (e.getBlock().getType() != Material.COBBLESTONE){
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if( e.getPlayer().getGameMode() == GameMode.SURVIVAL  || e.getPlayer().getGameMode() == GameMode.ADVENTURE) {
			// only run if player is in survival or adventure mode
			if (e.getBlock().getType() == Material.COBBLESTONE)
			{
				/**
				 *  Cobblestone is permitted.
				 */
			}
			else if(e.getBlock().getType() == Material.LONG_GRASS || e.getBlock().getType() == Material.YELLOW_FLOWER || e.getBlock().getType() == Material.RED_ROSE || e.getBlock().getType() == Material.DOUBLE_PLANT)
			{
				/**
				 *  Grass and flowers are permitted.  What are the Material names of the new flowers?
				 *    - Allow to break, but catch any spawned drops from the grass or flowers?
				 */
				e.setCancelled(true);
				e.getBlock().setType(Material.AIR);
			}
			else
			{
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract( PlayerInteractEvent e ) {
		
		Player clickingPlayer = e.getPlayer();
		Block clickedBlock = e.getClickedBlock();
		if( clickedBlock != null )
		{
			Location clickedBlockLoc = clickedBlock.getLocation();
			
			if( clickedBlock.getType() == BONUS_MATERIAL )
			{
				/**
				 *  Check if this block is in the list according to its Location.
				 */
				if( clickableBlocks.containsKey(clickedBlockLoc) )
				{
					/**
					 *  If Block location is already in the HashMap
					 *   - Decrease the Counter
					 */
					int clickCounter = clickableBlocks.get(clickedBlockLoc);
					//Bukkit.broadcastMessage("Block Counter: " + clickCounter + " going on " + (--clickCounter));
					clickCounter--;
					clickableBlocks.put(clickedBlockLoc, clickCounter);
				}
				else
				{
					/**
					 *  Otherwise:
					 *   - Add the Location to the HashMap and set the Counter
					 *   - Counter set to random 0-9
					 */
					int allowedClicks = randInt(0, MAX_BONUS_BLOCK_CLICKS);
					//Bukkit.broadcastMessage("Block Added to HashMap: " + allowedClicks + " allowed Clicks");
					clickableBlocks.put(clickedBlockLoc, allowedClicks);
				}	
				
				/**
				 *  Give the Player an Item, even if counter is Zero.
				 */
				ItemStack itemStack = new ItemStack(Material.EMERALD);
				clickingPlayer.getInventory().addItem(itemStack);
				clickingPlayer.playSound(clickingPlayer.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
				
				/**
				 *  Emerald Effect
				 */
				Location itemLocation = clickedBlock.getLocation().add(0, 1, 0);
				Item item = itemLocation.getWorld().dropItemNaturally(itemLocation, itemStack);
				item.setPickupDelay(100);
				Vector itemVelocity = item.getVelocity();
				itemVelocity.add(new Vector(0, 1, 0));
				item.setVelocity(itemVelocity);
				effectItems.add(item);
				
				/**
				 *  Check if player has ## Emeralds
				 */
				ItemStack emeralds = new ItemStack(Material.EMERALD, NUM_EMERALDS_FOR_POINT);
				if( clickingPlayer.getInventory().contains(emeralds) )
				{
					clickingPlayer.getInventory().removeItem(emeralds);
					
					if (GameState == GameStates.Running)
					{
						/**
						 *  Add a point to player's score, if game is currently running.
						 */
						updateScore(clickingPlayer, 1);
						
						clickingPlayer.sendMessage("Collected " + NUM_EMERALDS_FOR_POINT + " Emeralds!  Score increased!");
					}
					else
					{
						clickingPlayer.sendMessage(NUM_EMERALDS_FOR_POINT + " Emeralds are worth 1 Point in game.");
					}
				}
				
				/**
				 *  Block Click Count
				 */
				if( clickableBlocks.get(clickedBlockLoc) <= 0 )
				{
					/**
					 *  If the counter reaches Zero, set the block to AIR.
					 */
					clickedBlock.setType(Material.AIR);
					//Bukkit.broadcastMessage("Block Clicks Limit Reached.  Should now be AIR.");
					/**
					 *  Set another HashMap<Location, Integer> counter to count to respawn the block -> timeTicked().
					 */
					respawningBlocks.put(clickedBlockLoc, BONUS_BLOCK_RESPAWN_TIME);  //  TODO  Make this use BlockRespawner?
					clickableBlocks.remove(clickedBlockLoc);
				}
			}
			else if( clickedBlock.getType() == WEAPON_PICKUP_BLOCK )
			{ 
				for( Weapon w : weapons )
				{
					/**
					 *  Check if this block matches a Weapon Location
					 */
					if( clickedBlockLoc == w.getWeaponLocation() )
					{
						/**
						 *  Give the Player the Weapon
						 *   - Search inventory and upgrade Sword, Bow, etc if possible
						 */
						if( w.getWeaponItemStack().getType() == Material.BOW )
						{
							w.upgradeWeapon(clickingPlayer, Material.BOW);
						}
						else if( w.getWeaponItemStack().getType() == Material.IRON_SWORD )
						{
							w.upgradeWeapon(clickingPlayer, Material.STONE_SWORD);
						}
						else
						{
							clickingPlayer.getInventory().addItem(w.getWeaponItemStack());
						}
						
						/**
						 *  Remove Block and Add to Respawn Queue
						 */
						clickedBlock.setType(Material.AIR);
						clickingPlayer.playSound(clickingPlayer.getLocation(), Sound.CHICKEN_EGG_POP, 1, 1);
						BlockRespawner tempBlockRespawner = new BlockRespawner(w.getWeaponLocation(), w.getRespawnBlockMaterial(), w.getSpawnInterval());
						respawningWeapons.add(tempBlockRespawner);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onServerPing ( ServerListPingEvent e ){
		e.setMotd(MapName );
		if (GameState== GameStates.Running){
			e.setMaxPlayers( Bukkit.getOnlinePlayers().length);
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e ){

		if( GameState == GameStates.Running){

			/**
			 *  DeathCube perimeter check.  Fry player if outside of cube.
			 */
			if( e.getTo().getX()>cubeMaxX   || e.getTo().getX()<cubeMinX || e.getTo().getZ()> cubeMaxZ || e.getTo().getZ() <cubeMinZ){
				// the player should be outside the bounds of the death cube  

				//getLogger().info(e.getTo().toString());
				if(ACTIVE_FORCE_FIELD.equalsIgnoreCase("Y"))
				{
					e.getPlayer().damage(5);
					fryPlayer(e.getPlayer());
				}
			}
		}
	}

	@EventHandler
	public void onPlayerJoin ( PlayerJoinEvent e){
		if ((playerTeam.containsKey(e.getPlayer().getName())))
		{
			/**
			 *  Player is on a team already; need to setup his / her scoreboard
			 *   - If Free-for-All, player will never have been added to HashMap playerTeam
			 */
			SetPlayersTeam (e.getPlayer(), getTeamByPlayer(e.getPlayer()).getName() );
		}
		else
		{
			/**
			 * Start a new player in the Lobby.
			 */
			e.getPlayer().teleport(lobbySpawn );
			
			/**
			 *  If teams, make initial team "NONE"
			 */
			if(gameMode.equals("teams"))
				SetPlayersTeam(e.getPlayer(),"NONE");
			
			if (GameState == GameStates.Running){
				
				if(gameMode.equals("teams")) {
					Player[] onePlayer = {e.getPlayer()};
					assignPlayersToTeams(onePlayer);					
				}
				
				score = kills.getScore(e.getPlayer());
				
				score.setScore(0);
				
				gameStats.addPlayer(e.getPlayer());
				
				/**
				 *  Show scoreboard.
				 */
				if( gameMode.equals("teams") )
				{
					e.getPlayer().setScoreboard(teamBoard);
				}
				else
					e.getPlayer().setScoreboard(killsBoard);
				
				SpawnPlayerInGame(e.getPlayer(), "no");
			}
		}
	}
	
	@EventHandler
	public void onPlayerRespawn( PlayerRespawnEvent e){
		if( GameState == GameStates.Running){
			/**
			 *  If game is running, spawn in game.
			 */
			Location tempPoint = spawnPoints[randInt(0, numSpawnPoints-1)];
			e.setRespawnLocation(tempPoint);
			
			SpawnPlayerInGame(e.getPlayer(), "yes");
		}
		else 
		{
			/**
			 *  If game is not Running, set player respawn to the lobby.
			 */
			e.setRespawnLocation(lobbySpawn);
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		/**
		 *  Start the game immediately.
		 */
		if(cmd.getName().equalsIgnoreCase("startgame")){
			StartGame();
			return true;  // Return true when a command is executed successfully.
		}
		
		/**
		 *  Display some statistics about the game.
		 */
		if(cmd.getName().equalsIgnoreCase("gamestats")){
			
			String gameModeString = "";
			
			if(gameMode.equals("freeforall"))
				gameModeString = "Free-for-All";
			else if(gameMode.equals("teams"))
				gameModeString = numTeams + "-Team";
			else
				gameModeString = "Mode Not Set (Error)";
			
			if (( sender instanceof ConsoleCommandSender) || ( sender instanceof BlockCommandSender))
			{
				if( (SECONDS_TO_END_GAME - gameSecondsCount) < 60 )
					Bukkit.broadcastMessage("PVP Match - " + MapName + " - Statistics:\n - Game Time Remaining: " + (SECONDS_TO_END_GAME - gameSecondsCount) + " seconds.\n - Kills to Win: " + KILLS_TO_WIN_GAME + "\n - Game Mode: " + gameModeString);
				else
				{
					int minutes = (int) ((SECONDS_TO_END_GAME - gameSecondsCount)/60);
					int seconds = (gameSecondsCount % 60);
					Bukkit.broadcastMessage("PVP Match - " + MapName + " - Statistics:\n - Game Time Remaining: " + minutes + " minutes, " + seconds + " seconds.\n - Kills to Win: " + KILLS_TO_WIN_GAME + "\n - Game Mode: " + gameModeString);
				}
				
				String player;
				if(leadingPlayer == null)
					player = "N/A";
				else
					player = leadingPlayer.getName();
				String team;
				if(leadingTeam == null)
					team = "N/A";
				else
					team = leadingTeam.getName();
				Bukkit.broadcastMessage(" - Leading Player = " + player + "\n - Leading Team = " + team + "\n - Leading Score = " + leadingKillsScore);
			}
			else if( sender instanceof Player)
			{
				if( (SECONDS_TO_END_GAME - gameSecondsCount) < 60 )
					((Player)sender).sendMessage("PVP Match - " + MapName + " - Statistics:\n - Game Time Remaining: " + (SECONDS_TO_END_GAME - gameSecondsCount) + " seconds.\n - Kills to Win: " + KILLS_TO_WIN_GAME + "\n - Game Mode: " + gameModeString);
				else
				{
					int minutes = (int) ((SECONDS_TO_END_GAME - gameSecondsCount)/60);
					int seconds = (gameSecondsCount % 60);
					((Player)sender).sendMessage("PVP Match - " + MapName + " - Statistics:\n - Game Time Remaining: " + minutes + " minutes, " + seconds + " seconds.\n - Kills to Win: " + KILLS_TO_WIN_GAME + "\n - Game Mode: " + gameModeString);
				}
				
				String player;
				if(leadingPlayer == null)
					player = "N/A";
				else
					player = leadingPlayer.getName();
				String team;
				if(leadingTeam == null)
					team = "N/A";
				else
					team = leadingTeam.getName();
				((Player)sender).sendMessage(" - Leading Player = " + player + "\n - Leading Team = " + team + "\n - Leading Score = " + leadingKillsScore);
			}
			
			return true;  // Return true when a command is executed successfully.
		}
		
		/**
		 *  Set the number of kills needed to win the game.
		 */
		if(cmd.getName().equalsIgnoreCase("setkills"))
		{	
			if( args.length != 0)
			{
				if( Integer.parseInt(args[0]) > 0 )
				{
					KILLS_TO_WIN_GAME = Integer.parseInt(args[0]);
					Bukkit.broadcastMessage("Kills to Win Set: " + KILLS_TO_WIN_GAME);
				}
				else
					Bukkit.broadcastMessage("Kills must be greater than zero.");
			}
			else
			{
				Bukkit.broadcastMessage("No Value Given.  Kills to Win: " + KILLS_TO_WIN_GAME);
				return false;
			}
			
			return true;  // Return true when a command is executed successfully.
		}
		
		/**
		 *  Set the time limit until the game is over.
		 */
		if(cmd.getName().equalsIgnoreCase("settimelimit"))
		{	
			if( args.length != 0)
			{
				if( args.length > 1)
				{
					if( args[1].equalsIgnoreCase("m"))
						SECONDS_TO_END_GAME = Integer.parseInt(args[0]) * 60;
					else
					{
						SECONDS_TO_END_GAME = Integer.parseInt(args[0]);
						Bukkit.broadcastMessage("Unrecognized modifier.");
					}
				}
				else
				{
					SECONDS_TO_END_GAME = Integer.parseInt(args[0]);
				}
				
				if( SECONDS_TO_END_GAME > 60)
				{
					int minutes = (int) ((SECONDS_TO_END_GAME - gameSecondsCount)/60);
					int seconds = (gameSecondsCount % 60);
					Bukkit.broadcastMessage("Game Time Limit Set: " + minutes + " minutes, " + seconds + " seconds.");
				}
				else
				{
					Bukkit.broadcastMessage("Game Time Limit Set: " + SECONDS_TO_END_GAME + " seconds.");
				}
			}
			else
			{
				if( SECONDS_TO_END_GAME > 60)
				{
					int minutes = (int) ((SECONDS_TO_END_GAME - gameSecondsCount)/60);
					int seconds = (gameSecondsCount % 60);
					Bukkit.broadcastMessage("No Value Given.  Game Time Limit: " + minutes + " minutes, " + seconds + " seconds.");
					return false;
				}
				else
				{
					Bukkit.broadcastMessage("No Value Given.  Game Time Limit: " + SECONDS_TO_END_GAME + " seconds.");
					return false;
				}
			}
			
			return true;  // Return true when a command is executed successfully.
		}
		
		/**
		 *  Set the game mode, Free-for-All or Teams.
		 */
		if(cmd.getName().equalsIgnoreCase("setgamemode"))
		{	
			if( args.length != 0)
			{
				if(args[0].equals("freeforall") || args[0].equals("f"))
				{
					gameMode = "freeforall";
					Bukkit.broadcastMessage("Game Mode Set: Free-for-All");
				}
				else if(args[0].equals("teams") || args[0].equals("t"))
				{
					gameMode = "teams";
					
					if( args.length > 1)
					{
						if( Integer.parseInt(args[1]) > 1 )
						{
							numTeams = Integer.parseInt(args[1]);
						}
					}
					
					if( numTeams > MAX_TEAMS)
					{
						numTeams = MAX_TEAMS;
						Bukkit.broadcastMessage("Max Number of Teams: " + MAX_TEAMS);
					}
					
					Bukkit.broadcastMessage("Game Mode Set: " + numTeams + "-Team");
				}
				else
				{
					Bukkit.broadcastMessage("Error:  No Game Mode");
					return false;
				}
			}
			else
			{
				if(gameMode.equals("freeforall"))
					Bukkit.broadcastMessage("No Value Given.  Game Mode: Free-for-All");
				else if(gameMode.equals("teams"))
					Bukkit.broadcastMessage("No Value Given.  Game Mode: " + numTeams + "-Team");
				else
					Bukkit.broadcastMessage("Error:  No Game Mode");
				return false;
			}
			
			return true;  // Return true when a command is executed successfully.
		}
		
		return false;  // Otherwise, return false. 
	}
	
	public void gameModeSetup() {
		if( gameMode.equals("freeforall") ) {
			/**
			 *  Do nothing.
			 *   - Scoreboard is already set up.
			 */
		}
		else if( gameMode.equals("teams") ) {
			/**
			 *  Set up Teams
			 *   - Colors and Scoreboard modifications
			 */
			teamsArray = new Team[numTeams];
			SBTeam = new org.bukkit.scoreboard.Team[numTeams];
			
			String name, chatColor;
			int R, G, B;
			
			for(int i=1; i <= numTeams; i++)
			{
				name = this.getConfig().getString("Team" + i + ".Name");
				R = this.getConfig().getInt("Team" + i + ".Armor.Color.R");
				G = this.getConfig().getInt("Team" + i + ".Armor.Color.G");
				B = this.getConfig().getInt("Team" + i + ".Armor.Color.B");
				chatColor = this.getConfig().getString("Team" + i + ".ChatColor");
				
				teamsArray[i-1] = new Team(name, Color.fromRGB(R, G, B), StringToChatColor(chatColor));
			}
			
			/**
			 *  Team Scoreboard Setup
			 */
			for(int i=0; i <= numTeams-1; i++)
			{
				SBTeam[i] = teamBoard.registerNewTeam(teamsArray[i].getName());
				SBTeam[i].setDisplayName(teamsArray[i].getName());
				SBTeam[i].setCanSeeFriendlyInvisibles(true);
				SBTeam[i].setAllowFriendlyFire(false);
				SBTeam[i].setPrefix(teamsArray[i].getChatColor() +"");
				SBTeam[i].setSuffix(ChatColor.WHITE+"");
			}
		}
	}

	public void SetPlayersTeam(Player p , String TeamName)
	{
		/**
		 *  Assign a Player to a Team, given the Team name as String.
		 */
		for( int i=0; i <= numTeams-1; i++ )
		{
			if (TeamName.equals(teamsArray[i].getName()))
			{
				/**
				 *  HashMap: Player_Name, Team_Name
				 *    - Add player to HashMap
				 */
				playerTeam.put(p.getName(),teamsArray[i].getName());
				
				/**
				 *  Remove from all scoreboard teams in case of switching team
				 */
				for( int j=0; j <= numTeams-1; j++ )
				{
					SBTeam[j].removePlayer(p);
				}
				
				/**
				 *  Add back to correct scoreboard team and show player the scoreboard
				 */
				SBTeam[i].addPlayer(p);
				//p.setScoreboard(killsBoard);  //  Done at startGame() and onPlayerJoin()
			}
		}
		if (TeamName.equals("NONE"))
		{
			/**
			 *  Remove from all scoreboard teams when player logs on
			 */
			for( int i=0; i <= numTeams-1; i++ )
			{
				SBTeam[i].removePlayer(p);
			}

			/**
			 *  Show player the scoreboard
			 */
			//p.setScoreboard(killsBoard);  //  Done at startGame() and onPlayerJoin()
		}
	}


	public Team getTeamByPlayer(Player p)
	{
		/**
		 *  Return Team object given a Player object.
		 */
		
		Boolean foundTeam = false;
		
		if (!(p==null))
		{
			for( Team team : teamsArray )
			{
				if (playerTeam.get(p.getName()).equals(team.getName()))
					return team;
			}
			
			if(!foundTeam)
				getLogger().info("Team Name not found: getTeamByName");
		}
		else
		{
			getLogger().info("getTeamByPlayer called with null Player");
		}

		return null;
	}
	
	public Team getTeamByName(String name)
	{
		/**
		 *  Return Team object given a String of the Team name.
		 */
		
		Boolean foundTeam = false;
		
		if (!(name==null))
		{
			for( Team team : teamsArray )
			{
				if (name.equals(team.getName()))
					return team;
			}
			
			if(!foundTeam)
				getLogger().info("Team Name not found: getTeamByName");
		}
		else
		{
			getLogger().info("getTeamByName called with null String");
		}

		return null;
	}
	
	public void assignPlayersToTeams(Player[] playersArray)
	{
		/**
		 *  Given an array of one or more players, assign them to a team.
		 */
		for (Player p : playersArray){
			/**
			 *  Find team with fewest players.
			 */
			Team teamAssignment = getTeamAssignment();
			
			SetPlayersTeam(p,teamAssignment.getName());
		}
	}
	
	public Team getTeamAssignment()
	{
		/**
		 *  Next Team Assignment - Best match with fewest number of players
		 *    - Called each time a player is to be added to a team.
		 *    - First, assume each team has zero players.
		 *    - Second, count the number of players on each team.
		 *    - Third, determine which team has the fewest players
		 *       -- Resolve ties
		 *    - Return best team match.
		 */
		
		/**
		 *  Initially populate each team with zero players.
		 */
		for( Team t : teamsArray )
		{
			t.setPlayerCount(0);
		}

		/**
		 *  Count number of players on each team.
		 *    - For each player, find their team in the HashMap
		 *    - Compare this team name against all team names, and increment a counter on match.
		 */
		for( Player p : Bukkit.getOnlinePlayers() )
		{
			if(playerTeam.get(p.getName()) == null) 
			{
				// Do Nothing
			}
			else
			{
				for( Team t : teamsArray )
				{
					/**
					 *  If the Player's Team name equals a Team name, increment that team's player count.
					 */
					if(playerTeam.get(p.getName()).equals(t.getName()))
					{
						int count = t.getPlayerCount();
						count++;
						t.setPlayerCount(count);
					}
				}
			}
		}
		
		int lowestPlayers = teamsArray[0].getPlayerCount();
		String bestTeamMatch = teamsArray[0].getName();
		
		/**
		 *  Determine the winner - team with the fewest number of players.
		 *    - In case of a tie, choose randomly.
		 */
		for ( Team t : teamsArray ) 
		{
			if(t.getPlayerCount() > lowestPlayers)
			{
				// Do nothing.
			}
			if(t.getPlayerCount() == lowestPlayers)
			{
				/**
				 *  There is a tie between teams having the fewest number of players.
				 *    - If there is not a current Team match, let this entry win.
				 *    - Otherwise, allow or disallow based on a random number.
				 */
				if(bestTeamMatch.equals(t.getName()))
				{
					lowestPlayers = t.getPlayerCount();  //  Make sure the count is properly set.
				}
				else
				{
					if(randInt(1, 2) == 1)
					{
						/**
						 *  If random int equals 1, new entry wins
						 */
						lowestPlayers = t.getPlayerCount();
						bestTeamMatch = t.getName();
					}
				}
			}
			else if(t.getPlayerCount() < lowestPlayers)
			{
				/**
				 *  There is not a tie for the fewest number of players.
				 *    - This entry wins and gets updated as the best choice.
				 */
				lowestPlayers = t.getPlayerCount();
				bestTeamMatch = t.getName();
			}
		}
		
		/**
		 *  Return best team match via it's String name in this method.
		 */
		return getTeamByName(bestTeamMatch);
	}

	public ChatColor StringToChatColor(String stringColor)
	{ 
		if (stringColor.equalsIgnoreCase("RED")){
			return ChatColor.RED;
		}else if (stringColor.equalsIgnoreCase("GREEN")){
			return ChatColor.GREEN;
		}else if (stringColor.equalsIgnoreCase("BLUE")){
			return ChatColor.BLUE;
		}else if (stringColor.equalsIgnoreCase("WHITE")){
			return ChatColor.WHITE;
		}else if (stringColor.equalsIgnoreCase("BLACK")){
			return ChatColor.BLACK;
		}else if (stringColor.equalsIgnoreCase("GRAY")){
			return ChatColor.GRAY;	
		}else if (stringColor.equalsIgnoreCase("DARKGRAY")){
			return ChatColor.DARK_GRAY;
		}else if (stringColor.equalsIgnoreCase("DARK_GRAY")){
			return ChatColor.DARK_GRAY;
		}else if (stringColor.equalsIgnoreCase("GOLD")){
			return ChatColor.GOLD;
		}else if (stringColor.equalsIgnoreCase("DARK_BLUE")){
			return ChatColor.DARK_BLUE;
		}else if (stringColor.equalsIgnoreCase("AQUA")){
			return ChatColor.AQUA;
		}else if (stringColor.equalsIgnoreCase("DARK_AQUA")){
			return ChatColor.DARK_AQUA;
		}else if (stringColor.equalsIgnoreCase("DARK_GREEN")){
			return ChatColor.DARK_GREEN;
		}else if (stringColor.equalsIgnoreCase("DARK_PURPLE")){
			return ChatColor.DARK_PURPLE;
		}else if (stringColor.equalsIgnoreCase("DARK_RED")){
			return ChatColor.DARK_RED;
		}else if (stringColor.equalsIgnoreCase("LIGHT_PURPLE")){
			return ChatColor.LIGHT_PURPLE;
		}else if (stringColor.equalsIgnoreCase("YELLOW")){
			return ChatColor.YELLOW;
		}
		else { 
			getLogger().warning("CHAT COLOR in Config file unsupported." + stringColor);
			return ChatColor.WHITE;
		}
	}
	
	public String getFormattedScorboardName(ChatColor chatcolor, String displayName)
	{
		if (displayName.length() > 14)
		{
			displayName= displayName.substring(0, 14);
		}
		return chatcolor + displayName+ ":";
	}
	
	public void updateScore(Player updatePlayer, int scoreChange) {
		/**
		 *  Add scoreChange to Player's Score
		 */
		score = kills.getScore(updatePlayer);
		score.setScore((int) score.getScore() + scoreChange);
		
		if( gameMode.equals("teams"))
		{
			/**
			 *  Recalculate Team's Score
			 */
			calculateTeamScore(getTeamByPlayer(updatePlayer));
			
			/**
			 *  Update Teams Scoreboard
			 */
			Team tempTeam = getTeamByPlayer(updatePlayer);
			String teamScoreboardName = getFormattedScorboardName(tempTeam.getChatColor(), tempTeam.getName());
			score = teamKills.getScore(Bukkit.getOfflinePlayer(teamScoreboardName));
			score.setScore(tempTeam.getTeamScore());
		}
		
		/**
		 *  Check if new score is greater than leadingKillsScore.
		 *   - Applies for either Teams or Free-for-All
		 *      -- FFA score object is overwritten if gameMode is Teams
		 */
		if(score.getScore() > leadingKillsScore)
		{
			leadingKillsScore = score.getScore();
			
			if( gameMode.equals("teams"))
				leadingTeam = getTeamByPlayer(updatePlayer);
			else
				leadingPlayer = updatePlayer;
		}
		
		/**
		 *  If scoreChange was negative, may need to recalculate new leading player/team.
		 *  
		 */
		if( scoreChange < 0 )
		{
			if( gameMode.equals("teams") && leadingTeam != null )
			{
				/**
				 *  If Player was on the Leading Team
				 */
				if( getTeamByPlayer(updatePlayer).getName() == leadingTeam.getName() )
				{
					/**
					 *  Find new leading Team
					 */
					leadingKillsScore = score.getScore();
					
					for( Team t : teamsArray)
					{
						Team tempTeam = getTeamByPlayer(updatePlayer);
						String teamScoreboardName = getFormattedScorboardName(tempTeam.getChatColor(), tempTeam.getName());
						score = teamKills.getScore(Bukkit.getOfflinePlayer(teamScoreboardName));
						
						if(score.getScore() > leadingKillsScore)
						{
							leadingKillsScore = score.getScore();
							leadingTeam = t;
						}
					}
				}
			}
			else if( gameMode.equals("freeforall") && leadingPlayer != null )
			{
				/**
				 *  If Player was Leading in Kills
				 */
				if( updatePlayer.getName() == leadingPlayer.getName() )
				{
					/**
					 *  Find new leading Player
					 */
					leadingKillsScore = score.getScore();
					
					for( Player p : Bukkit.getOnlinePlayers() )
					{
						score = kills.getScore(p);
						
						if(score.getScore() > leadingKillsScore)
						{
							leadingKillsScore = score.getScore();
							leadingPlayer = p;
						}
					}
					
				}
			}  
		}
	}
	
	public void calculateTeamScore(Team t) {
		
		int tempScore = 0;
		
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (playerTeam.get(p.getName()).equals(t.getName())){
				score = kills.getScore(p);
				tempScore += score.getScore();
			}
		}
		
		t.setTeamScore(tempScore);
	}
	
	public void playSoundForTeam( String teamName, Sound snd, float volume, float pitch){
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (playerTeam.get(p.getName()).equals(teamName)){
				p.playSound(p.getLocation(), snd, volume, pitch);
			}
		}
	}
	
	public void SpawnPlayerInGame (Player p, String isRespawnEvent){
		if (GameState == GameStates.Running){
			p.setHealth(p.getMaxHealth());
			p.setFoodLevel(20);
			GivePlayerGear(p);
			
			if( isRespawnEvent.equals("no") )
				PutPlayerOnMap(p);
		}
	}
	
	public void PutPlayerOnMap(Player p){
		/**
		 *  Choose a Spawn Point at random.
		 */
		int attempts = 0;
		Boolean pointNotFound = true;
		Location tempPoint = spawnPoints[randInt(0, numSpawnPoints-1)];
		
		/**
		 *  Get a possible spawn point.
		 *  Get all nearby entities.
		 *  Assume it is a safe place to spawn.
		 *  If any of the entities is a Player, stop and get another possible spawn point.
		 *  If none of the entities are players, do nothing.
		 *  Safe spawn point found, spawn player.
		 */
		
		while( pointNotFound && attempts < 15 )
		{
			tempPoint = spawnPoints[randInt(0, numSpawnPoints-1)];
			
			Arrow tempArrow = Bukkit.getWorld("world").spawnArrow(tempPoint, new Vector(), 0, 0);
			List<Entity> nearbyEntities = tempArrow.getNearbyEntities(5, 5, 5);
			tempArrow.remove();  //  Does this need to wait until after using nearbyEntities?
			
			pointNotFound = false;
			
			for(Entity e : nearbyEntities) {
			    if( e instanceof Player )
			    {
			    	/**
			    	 *  Point not safe, keep searching.
			    	 */
			    	pointNotFound = true;
			    }
			    else
			    {
			    	/**
			    	 *  Point is safe; no players nearby.
			    	 */
			    }
			}
			
			attempts++;
		}
		p.teleport( tempPoint );
	}
	
	public void GivePlayerGear(Player p ){

		p.getInventory().clear();
		
		String configPrefix;
		
		/**
		 *  If config file has Inventory lines:
		 *  
		 *  Get Armor by Piece
		 *    - If Leather Armor
		 *       -- Color Armor by Team
		 *  Get Items (Normal and Damage Value)
		 *  Get Potions
		 *  Get Items (Special)
		 *    - Compass
		 *    
		 */
		
		/**
		 *  Armor:
		 *   - Get Item
		 *   - If teams, color item.
		 *   - Enchant Item
		 *   - Add Item to Player
		 *   ---
		 *  Helmet - If "LEATHER", set color
		 */
		ItemStack helmet = new ItemStack(Material.getMaterial(this.getConfig().getString("Inventory.Armor.Helmet.Name")), 1);
		if(gameMode.equals("teams")) {
			if((this.getConfig().getString("Inventory.Armor.Helmet.Name")).contains("LEATHER"))
			{   
				colorArmor(getTeamByPlayer(p), helmet);
			}
		}
		configPrefix = "Inventory.Armor.Helmet";
		enchantItem(configPrefix, helmet);
		p.getInventory().setHelmet(helmet);
		
		/**
		 *  ChestPlate - If "LEATHER", set color
		 */
		ItemStack chestPlate = new ItemStack(Material.getMaterial(this.getConfig().getString("Inventory.Armor.ChestPlate.Name")), 1);
		if(gameMode.equals("teams")) {
			if((this.getConfig().getString("Inventory.Armor.ChestPlate.Name")).contains("LEATHER"))
			{
				colorArmor(getTeamByPlayer(p), chestPlate);
			}
		}
		configPrefix = "Inventory.Armor.ChestPlate";
		enchantItem(configPrefix, chestPlate);
		p.getInventory().setChestplate(chestPlate);
		
		/**
		 *  Leggings - If "LEATHER", set color
		 */
		ItemStack leggings = new ItemStack(Material.getMaterial(this.getConfig().getString("Inventory.Armor.Leggings.Name")), 1);
		if(gameMode.equals("teams")) {
			if((this.getConfig().getString("Inventory.Armor.Leggings.Name")).contains("LEATHER"))
			{
				colorArmor(getTeamByPlayer(p), leggings);
			}
		}
		configPrefix = "Inventory.Armor.Leggings";
		enchantItem(configPrefix, leggings);
		p.getInventory().setLeggings(leggings);
		
		/**
		 *  Boots - If "LEATHER", set color
		 */
		ItemStack boots = new ItemStack(Material.getMaterial(this.getConfig().getString("Inventory.Armor.Boots.Name")), 1);
		if(gameMode.equals("teams")) {
			if((this.getConfig().getString("Inventory.Armor.Boots.Name")).contains("LEATHER"))
			{
				colorArmor(getTeamByPlayer(p), boots);
			}
		}
		configPrefix = "Inventory.Armor.Boots";
		enchantItem(configPrefix, boots);
		p.getInventory().setBoots(boots);			
		
		
		
		/**
		 *  Items
		 */
		
		int itemCount = this.getConfig().getInt("Inventory.NumItems");
		for(int i=1 ; i <= itemCount; i++)
		{
			int amount = 1;
			int damage = 0;
			ItemStack itemStack;
			
			/**
			 *  Get Material
			 */
			Material material = Material.getMaterial(this.getConfig().getString("Inventory.Item" + i + ".Name"));
			
			/**
			 *  Check for Amount
			 */
			if(this.getConfig().getInt("Inventory.Item" + i + ".Amount") != 1)
				amount = this.getConfig().getInt("Inventory.Item" + i + ".Amount");
			
			/**
			 *  Check for Damage Value
			 */
			if(this.getConfig().getInt("Inventory.Item" + i + ".Damage") != 0)
			{
				damage = this.getConfig().getInt("Inventory.Item" + i + ".Damage");
				
				/**
				 *  Create and Add Item
				 */
				itemStack = new ItemStack(material, amount, (short) damage);
				p.getInventory().addItem(itemStack);
			}
			else
			{
				/**
				 *  Create and Add Item
				 */
				itemStack = new ItemStack(material, amount);
			}
			
			/**
			 *  Enchant Item
			 */
			configPrefix = "Inventory.Item" + i;
			enchantItem(configPrefix, itemStack);
							
			/**
			 *  Add Item to Inventory
			 */
			p.getInventory().addItem(itemStack);
		}
		
		
		
		/**
		 *  Potions
		 */
		
		int potionCount = this.getConfig().getInt("Inventory.NumPotions");
		
		for(int i=1 ; i <= potionCount; i++)
		{
			int amount = 1;
			int level = 1;
			Boolean splash = false;
			
			/**
			 *  Get Potion Type
			 */
			PotionType potionType = PotionType.valueOf(this.getConfig().getString("Inventory.Potion" + i + ".Type"));
			
			/**
			 *  Check for Amount
			 */
			if(this.getConfig().getInt("Inventory.Potion" + i + ".Amount") != 1 )
				amount = this.getConfig().getInt("Inventory.Potion" + i + ".Amount");
			
			/**
			 *  Check for Level
			 */
			if(this.getConfig().getInt("Inventory.Potion" + i + ".Level") != 1 )
				level = this.getConfig().getInt("Inventory.Potion" + i + ".Level");
			
			/**
			 *  Check for Splash
			 */
			if(this.getConfig().getString("Inventory.Potion" + i + ".Splash").equals("Y") )
				splash = true;
			
			/**
			 *  Create and Add Potion
			 */
			ItemStack itemPotion = new ItemStack(Material.POTION, amount);
			Potion potionAttributes = new Potion(potionType, level);
			potionAttributes.setSplash(splash);
			potionAttributes.apply(itemPotion);
			p.getInventory().addItem(itemPotion);
		}
		
		/**
		 *  Special:
		 *  
		 *  Compass
		 */
		
		int specialCount = this.getConfig().getInt("Inventory.NumSpecials");
		
		for(int i=1 ; i <= specialCount; i++)
		{
			if(this.getConfig().getString("Inventory.Special" + i).equals("COMPASS") )
			{
				Material material = Material.getMaterial(this.getConfig().getString("Inventory.Special" + i));
				ItemStack itemCompass = new ItemStack(material, 1);	
				ItemMeta compassMeta = itemCompass.getItemMeta();
				compassMeta.setDisplayName("Next Point Tracker");
				//May add lore in future update
				//im.setLore(Lore);	
				itemCompass.setItemMeta(compassMeta);	
				p.getInventory().addItem(itemCompass);
			}
		}	
		
		/**
		 *  Effects:
		 */
		
		int effectCount = this.getConfig().getInt("Inventory.NumEffects");
		
		for(int i=1 ; i <= effectCount; i++)
		{
			int effectDuration = 0;
			int effectAmplifier = 1;
			
			/**
			 *  Effect Name
			 */
			PotionEffectType effectType = PotionEffectType.getByName(this.getConfig().getString("Inventory.Effect" + i + ".Name"));
			
			/**
			 *  Effect Duration
			 */
			if(this.getConfig().getInt("Inventory.Effect" + i + ".Duration") != 0)
				effectDuration = this.getConfig().getInt("Inventory.Effect" + i + ".Duration");
			
			/**
			 *  Effect Amplifier
			 */
			if(this.getConfig().getInt("Inventory.Effect" + i + ".Amplifier") != 1)
				effectAmplifier = this.getConfig().getInt("Inventory.Effect" + i + ".Amplifier");
			
			/**
			 *  Create Potion Effect
			 */
			PotionEffect effect = new PotionEffect(effectType, effectDuration, effectAmplifier);
			
			p.addPotionEffect(effect);
		}
	}
	
	
	
	/**
	 *  Color Armor Piece - Input Team, Armor Piece
	 */
	public ItemStack colorArmor(Team team, ItemStack armorPiece){
		LeatherArmorMeta armorMeta = (LeatherArmorMeta)armorPiece.getItemMeta();
		armorMeta.setColor(team.getArmorColor());
		armorPiece.setItemMeta(armorMeta);
		return armorPiece;
	}
	
	/**
	 *  Enchant an Item - Input Team, Armor Piece
	 */
	public ItemStack enchantItem(String configPrefix, ItemStack itemToEnchant){
		/**
		 *  Check for Enchantments for this Item
		 */
		
		int numEnchants = this.getConfig().getInt(configPrefix + ".NumEnchants");
		
		for(int j=1 ; j <= numEnchants; j++)
		{
			/**
			 *  Create Enchantment by ID.
			 */
			Enchantment enchant = new EnchantmentWrapper(this.getConfig().getInt(configPrefix + ".Enchant" + j + ".ID"));
			
			/**
			 *  Add Enchantment to Item, if possible.  
			 */
			itemToEnchant.addUnsafeEnchantment(enchant, this.getConfig().getInt(configPrefix + ".Enchant" + j + ".Level"));
		}
		
		return itemToEnchant;
	}
	
	public double centerBlockValue(double value){
		/**
		 *  Move a coordinate by half a block, direction depending on positive
		 *  or negative coordinate space.
		 */
		
		if( value < 0 )
			value -= .5D;
		else
			value += .5D;
		
		return value;
	}
	
	public void fryPlayer( Player p){

		for (int h=0; h<2; h++){
			Location l = p.getLocation();
			l.setY(l.getY()+h);		

			getServer().getWorld("world").playEffect(l, Effect.MOBSPAWNER_FLAMES, 10);
			getServer().getWorld("world").playSound(l, Sound.FIZZ, 1, 1);
		}
	}
	
	public static int randInt(int min, int max) {

	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
	
	public void visualTicker() {
		if( shortTick > SHORT_TICK_LIMIT )
		{
			shortTick = 0;
		}
		else if( longTick > LONG_TICK_LIMIT )
		{
			longTick = 0;
		}
		else
		{
			shortTick++;
			longTick++;
		}
	}
	
	public void gameTimer()
	{
		/**
		 *  Game Length Timer - Count game length in seconds, i.e. every other time the TimeTicked() method runs.
		 */
		if(timeTickedTimer == 1)
			timeTickedTimer = 2;
		else
		{
			timeTickedTimer = 1;
			gameSecondsCount++;
			
			if( gameSecondsCount == (int) SECONDS_TO_END_GAME * 0.8 && GameState == GameStates.Running)
			{
				/**
				 *  Broadcast message and play sound when game timer nears the End of Game time limit.
				 */
				if( (SECONDS_TO_END_GAME - gameSecondsCount) < 60 )
					Bukkit.broadcastMessage("Warning - Remaining Time: " + (SECONDS_TO_END_GAME - gameSecondsCount) + " seconds.");
				else
				{
					Bukkit.broadcastMessage("Warning - Remaining Time: " + (int) (SECONDS_TO_END_GAME - gameSecondsCount)/60 + " minutes.");
				}
				
				for (Player p : Bukkit.getOnlinePlayers())
				{
					p.playSound(p.getLocation(), Sound.ENDERMAN_STARE, 1, (float) 1.5);
				}
			}
		}
	}
	
	public void TimeTicked() {
		/**
		 *  Game Length Timer
		 *   - TimeTicked() runs every 10 ticks, or 0.5 seconds.
		 */
		gameTimer();
		
		if (GameState == GameStates.Running){

			/**
			 *  Timer for showing Point Visualization
			 */
			visualTicker();
			
			/**
			 *  Show visuals at weapons and bonus effect locations
			 *  Process weapon entity if it doesn't have a respawn timer value.
			 */
			for( Weapon w : weapons )
			{
				w.weaponVisual(shortTick, SHORT_TICK_LIMIT, longTick, LONG_TICK_LIMIT);
			}
			
			/**
			 *  Process Kill Streak Timer
			 */
			gameStats.processKillStreakTimer();
			
			/**
			 *  Check for Game Over
			 */
			if( gameSecondsCount >= SECONDS_TO_END_GAME || leadingKillsScore >= KILLS_TO_WIN_GAME || GameState == GameStates.GameOver )
			{
				/**
				 *  Game Over
				 */
				GameState = GameStates.GameOver;
				
				if( gameMode.equals("freeforall") )
				{
					Bukkit.broadcastMessage(leadingPlayer.getName() + " Wins!");
					
					for (Player p: Bukkit.getOnlinePlayers()){
						if(p == leadingPlayer)
							p.playSound(p.getLocation(), Sound.ENDERDRAGON_DEATH, 1, 1);
						else
							p.playSound(p.getLocation(), Sound.ZOMBIE_IDLE, 10, (float) 0.25);
					}	
				}
				else if( gameMode.equals("teams") )
				{
					Bukkit.broadcastMessage(leadingTeam.getChatColor() +  leadingTeam.getName() + " Wins!");
					
					for (Team t: teamsArray){
						if(t == leadingTeam)
							playSoundForTeam(t.getName(), Sound.ENDERDRAGON_DEATH, 1, 1);
						else
							playSoundForTeam(t.getName(), Sound.ZOMBIE_IDLE, 10, (float) 0.25);
					}	
				}
				
				gameStats.showStats();
			}
				
		}else if  ( GameState == GameStates.Lobby){
			if (Bukkit.getOnlinePlayers().length > 3) {
				GameState = GameStates.GameWarmup;
				Bukkit.broadcastMessage("Min Player count reached ! game starting in "+ TimeTillStart);
			}
		}else if ( GameState == GameStates.GameWarmup){
			TimeTillStart=TimeTillStart-1;
			if ( TimeTillStart > 0 ) {
				if (TimeTillStart == 30 || TimeTillStart <=10 ){
					Bukkit.broadcastMessage(" Game starting in "+ TimeTillStart);
				}
			} else {
				StartGame();
			}
		}else if ( GameState == GameStates.GameOver){
			coolDownTimer=coolDownTimer-1;
			
			if (coolDownTimer==2){
				//		Bukkit.broadcastMessage("Server Restating...");
			}

			if (coolDownTimer==0){
				//	Bukkit.getServer().shutdown();

				//  Call command in the MapVote Plugin to start the vote !
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "startvote");
			}
		}
		
		/**
		 *  Clicked Block Respawn Timer
		 */
		if( respawningBlocks.size() > 0 )
		{
			Iterator<Map.Entry<Location, Integer>> it = respawningBlocks.entrySet().iterator();
			
			/**
			 *  Loop through HashMap, decrementing timers.
			 */
			while (it.hasNext()) {
				Map.Entry<Location, Integer> entry = it.next();

				Location location = entry.getKey();

				int respawnTime = respawningBlocks.get(location);
				if( ((int) (respawnTime % 5)) == 0 )
				respawnTime -= 10;

				if (respawnTime <= 0) {
					Block tempBlock = location.getBlock();  //location.getWorld().getBlockAt(location);
					tempBlock.setType(Material.EMERALD_BLOCK);
					respawningBlocks.remove(location);
				} else {
					respawningBlocks.put(location, respawnTime);
				}
			}
		}
		
		/**
		 *  Emerald Effect Timer
		 */
		if( effectItems.size() > 0 )
		{
			Iterator<Item> it = effectItems.iterator();
			
			/**
			 *  Loop through List, checking Time Lived.
			 */
			while (it.hasNext()) {
				Item entryItem = it.next();

				if( entryItem.getTicksLived() >= 30 )
				{
					entryItem.remove();
					
					effectItems.remove(entryItem);
				}
			}
		}
		
		/**
		 *  Check Clickable Blocks List for Loose Ends
		 *   - If it's on the list, but the block at this location is not Emerald, then 
		 *        somehow the block was broken before the counter reached Zero.
		 */
		if( ((int) (gameSecondsCount % 30)) == 0 )
		{
			if( !clickableBlocks.isEmpty() )
			{
				Iterator<Map.Entry<Location, Integer>> it = clickableBlocks.entrySet().iterator();

				/**
				 *  Loop through HashMap, decrementing timers.
				 */
				while (it.hasNext()) {
					Map.Entry<Location, Integer> entry = it.next();

					Location location = entry.getKey();

					Block tempBlock = location.getWorld().getBlockAt(location);
					
					if( tempBlock.getType() != Material.EMERALD_BLOCK )
					{
						clickableBlocks.remove(location);
						
						Bukkit.broadcastMessage("An Emerald Block has broken before its time.  So sad.");
					}
				}
			}
		}
		
		/**
		 *  Process Weapon Respawning
		 */
		if( respawningWeapons.size() > 0 )
		{
			Iterator<BlockRespawner> it = respawningWeapons.iterator();
			
			/**
			 *  Loop through List, checking Remaining Respawn Time.
			 */
			while (it.hasNext()) {
				BlockRespawner entryBlockRespawner = it.next();

				int remainingTime = entryBlockRespawner.processTimer();
				
				if( remainingTime <= 0 )
				{
					/**
					 *  Spawn Block and remove entry from Main List
					 */
					entryBlockRespawner.spawnBlock();
					
					respawningWeapons.remove(entryBlockRespawner);
				}
			}
		}
	}

	public void StartGame(){

		Bukkit.getServer().getWorld("World").playSound(lobbySpawn,  Sound.BAT_DEATH, 100, 1);
		
		gameStats = new Statistics();
		enlightener = new Enlightener();
		
		Player[] players = Bukkit.getOnlinePlayers();
		
		if( gameMode.equals("teams") )
		{
			assignPlayersToTeams(players);
		}

		GameState= GameStates.Running;

		for (Player p : players)
		{
			score = kills.getScore(p);
			
			score.setScore(0);
		}
		
		if( gameMode.equals("teams") )
		{
			for( Team t : teamsArray )
			{
				calculateTeamScore(t);
				
				/**
				 *  Update Teams Scoreboard
				 */
				String teamScoreboardName = getFormattedScorboardName(t.getChatColor(), t.getName());
				score = teamKills.getScore(Bukkit.getOfflinePlayer(teamScoreboardName));
				score.setScore(t.getTeamScore());
			}
		}
		
		for (Player p : players)
		{
			/**
			 *  Avoid death by falling.
			 */
			Vector vec = new Vector();
			p.setVelocity(vec);
			p.setFallDistance(0);
			
			/**
			 *  Clear all effects acquired in lobby.
			 */
			for (PotionEffect effect : p.getActivePotionEffects())
			{
				p.removePotionEffect(effect.getType());
			}
			
			/**
			 *  Show scoreboard.
			 */
			if( gameMode.equals("teams") )
			{
				p.setScoreboard(teamBoard);
			}
			else
				p.setScoreboard(killsBoard);
			
			/**
			 *  Join game.
			 */
			SpawnPlayerInGame(p, "no");
		}
		
		/**
		 *  Spawn Weapon Blocks on Map and Clear Respawn Queue
		 */
		for( Weapon w : weapons )
		{
			w.spawnBlock();
		}
		
		respawningWeapons.clear();
	}
}
