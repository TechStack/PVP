
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
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
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
 *    - Weapons on Map (includes Items)  TODO  In Progress...
 *       -- Customizable via Config
 *           --- Item Abilities
 *           --- Item Locations
 *           --- Item Respawn (and Duration?)
 *    - Potions  TODO
 *    - Special Points  TODO
 *       -- Armor Upgrade
 *       -- Effect
 *       -- Enchanting
 *    - Cooldown for all item and special points.  TODO 
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
	private static final int KILL_STREAK_TIME = 10;
	private static final int WEAPON_SPAWN_INTERVAL = 30;
	private Map<Player, Integer> killStreakTimer;
	private Map<Player, Integer> killStreakMultiplier;
	public int enlightenChance; 
	private int numSpawnPoints;
	private int numWeapons;
	private int gameSecondsCount = 0;
	private int timeTickedTimer = 1;
	private int leadingKillsScore = 0;
	private Player leadingPlayer;
	private Team leadingTeam;
	
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
	private static final int VISUAL_POINT_TICK_LIMIT = 1;
	private static final int VISUAL_POINT_TICK_LIMIT2 = 3;
	private int visualPointTick = VISUAL_POINT_TICK_LIMIT;
	private int visualPointTick2 = VISUAL_POINT_TICK_LIMIT2;
	
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
		killStreakTimer = new HashMap<Player, Integer>();
		killStreakMultiplier = new HashMap<Player, Integer>();

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
			 *  Create Weapon Object
			 */
			weapons[i-1] = new Weapon(weaponLocation, material, amount, damage, displayName);
			
			/**
			 *  Enchant Item
			 */
			String configPrefix = "Weapon" + i;
			ItemStack tempIS = weapons[i-1].getWeaponItemStack();
			enchantItem(configPrefix, tempIS);
			weapons[i-1].setWeaponItemStack(tempIS);
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
	public void onDeath(PlayerDeathEvent e) {  //  TODO Track Player Deaths for end-game statistic?
		
		Player victim = e.getEntity();
		
		/**
		 *  Remove them from the Kill Streak Timer, if they're on it.
		 */
		if( killStreakTimer.containsKey(victim) ) {
			victim.setExp(0);
			killStreakTimer.put(victim, 0);
		}
		
		if (e.getEntity() instanceof Player){
			
			Player killer = e.getEntity().getKiller();
			
			if (killer instanceof Player){
				/**
				 *  A player killed another player.  Allow chance to enlighten the killer.
				 *  
				 *  Also, increment scoreboard (Kills).
				 */
				
				enlighten(killer);
				
				updateScore(killer, 1);
				
				/**
				 *  Check Kill Streak
				 */
				int streakTime = killStreakTimer.get(killer);
				
				if( streakTime > 0 )
				{
					/**
					 *  Already on Killing Streak; increase enlightenment chance or give effect?
					 */
					int multiplier = killStreakMultiplier.get(killer) + 1;
					killStreakMultiplier.put(killer, multiplier);
					
					/**
					 *  TODO Count kills since last respawn.  Killing Spree, Killing Frenzy, etc.
					 */
					String streakName = "";
					switch( multiplier ) {
					case 2:
						streakName = "Double Kill!";
						break;
					case 3:
						streakName = "Triple Kill!";
						break;
					case 4:
						streakName = "Overkill!";
						break;
					case 5:
						streakName = "Killtacular!";
						break;
					case 6:
						streakName = "Killtrocity!";
						break;
					case 7:
						streakName = "Killimanjaro!";
						break;
					case 8:
						streakName = "Killtastrophe!";
						break;
					case 9:
						streakName = "Killpocalypse!";
						break;
					case 10:
						streakName = "Killionaire!";
						break;
					}
					((Player)killer).sendMessage(streakName);
					
					Bukkit.broadcastMessage(killer.getName() + " - " + streakName);  //  Debug
				}
				else
					killStreakMultiplier.put(killer, 1);
				
				/**
				 *  Allow # seconds to continue kill streak.
				 */
				killStreakTimer.put(killer, KILL_STREAK_TIME*20);
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
		}
		
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
	public void onItemPickup( PlayerPickupItemEvent e ) {  //  TODO Item Pickup
		Item item = e.getItem();
		ItemStack itemStack = item.getItemStack();
		ItemMeta itemMeta;
		String displayName;
		
		if( itemStack.hasItemMeta() )
		{
			itemMeta = itemStack.getItemMeta();
			
			if( itemMeta.hasDisplayName() )
			{
				displayName = itemMeta.getDisplayName();
				
				for( Weapon w : weapons )
				{
					if(w.getWeaponItemStack().hasItemMeta())
					{
						if( displayName.equals(w.getWeaponItemStack().getItemMeta().getDisplayName()) )
						{
							/**
							 *  Weapon was picked up.
							 *   - Start a respawn timer.
							 *   - Remove cobweb block from weapon location.
							 */
							w.setRespawnTimer(WEAPON_SPAWN_INTERVAL*20);
							w.getWeaponLocation().getBlock().setType(Material.AIR);
						}
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
				
				killStreakTimer.put(e.getPlayer(), 0);
				killStreakMultiplier.put(e.getPlayer(), 0);
				
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
				KILLS_TO_WIN_GAME = Integer.parseInt(args[0]);
				Bukkit.broadcastMessage("Kills to Win Set: " + KILLS_TO_WIN_GAME);
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
			getLogger().warning("CHAT COLOR in Config file unsupported TECH GET ON THAT!" + stringColor);
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
	
	public void enlighten(Player p){
		// this method will enchant a players weapon (sword or bow) 
		if (willEnlighten(p)){
			Enchantment tmpHoldEnchantment = getEchantmentByWeaponType(p.getItemInHand().getType());
			if( tmpHoldEnchantment != null){			
				addEnchantLevel(tmpHoldEnchantment,p,5);
			}
		}
	}
	
	public boolean willEnlighten(Player p){
		// get a random number between 0 and 100
		double tmp =Math.random()*100;
		// if the number is less than the enlighten chance then we return true ( should enlighten)
		if( tmp <= (enlightenChance + 5*killStreakMultiplier.get(p)) ){
			return true;
		}else 
		{
			return false;
		}
	}
	
	public Enchantment getEchantmentByWeaponType(Material m){
		// Will need updated if we add other weapons to the mix 
		switch (m) {
		case STONE_SWORD:
			return Enchantment.DAMAGE_ALL;
		case IRON_SWORD:
			return Enchantment.DAMAGE_ALL;
		case GOLD_SWORD:
			return Enchantment.DAMAGE_ALL;
		case DIAMOND_SWORD:
			return Enchantment.DAMAGE_ALL;
		case BOW:
			return Enchantment.ARROW_DAMAGE;
		default:
			return null;
		}
	}
	
	public void addEnchantLevel(Enchantment e, Player p, int maxlevel){
		int currLevel;
		currLevel= p.getItemInHand().getEnchantmentLevel(e);
		if (currLevel< maxlevel){
			//player is under max level safe to enchant.
			try {
				p.getItemInHand().addEnchantment(e, currLevel+1);
				p.playSound(p.getLocation(), Sound.ANVIL_USE, 1, 1);
				p.sendMessage("Your skill as a warrior has been proven. You have been granted a higher enchantment!");
			}
			finally {
				// do nothing if an error only put try catch in to prevent errors in case user switches to another weapon in the time
				//between picking the enchant type till the enchantment is added or if somehow the player killed 2 players close together 
			}
		}
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
		Boolean safePoint = false;
		Location tempPoint = spawnPoints[0];
		double radius = 5D;
		
		while( !safePoint )
		{
			tempPoint = spawnPoints[randInt(0, numSpawnPoints-1)];
			
			List<Entity> nearbyEntities = tempPoint.getWorld().getEntities();
			for(Entity e : nearbyEntities) {
			    if( e instanceof Player )
			    {
			    	if(e.getLocation().distance(tempPoint) <= radius)
			    		safePoint = false;
			    	else
			    		safePoint = true;
			    }
			}
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
	
	private void weaponGravity() {
		/**
		 *  TODO Make weapon float?  Set y-velocity to 0 constantly + reset position?
		 */
		
	}
	
	public void showVisual(Location location, String effectType, String effectType2){
		/**
		 *  Create visual effect for weapons or bonus effect locations.
		 *  TODO  This can be made more efficient by pulling the inner if-statements out to be the 
		 *  		first thing evaluated.
		 */
		Location tempLocation = location;
		
		Effect effect1 = Effect.ENDER_SIGNAL;
		Effect effect2 = Effect.SMOKE;
		
		Effect showEffectMore, showEffectLess;
		
		if( effectType.equals("weapon") )
			showEffectMore = effect2;
		else if( effectType.equals("effect") )
			showEffectMore = effect2;
		else
			showEffectMore = null;
		
		if( effectType2.equals("weapon") )
			showEffectLess = null;
		if( effectType2.equals("effect") )
			showEffectLess = effect1;
		else
			showEffectLess = null;
		
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if( visualPointTick2 >= VISUAL_POINT_TICK_LIMIT2 && showEffectLess != null )
			{
				p.playEffect(tempLocation, showEffectLess, null);
			}
			
			if( visualPointTick >= VISUAL_POINT_TICK_LIMIT && showEffectMore != null )
			{
				p.playEffect(tempLocation, showEffectMore, 4);
				tempLocation.add(0, 1, 0);
				p.playEffect(tempLocation, showEffectMore, 4);
			}
		}
	}
	
	public void visualTicker() {
		if( visualPointTick > VISUAL_POINT_TICK_LIMIT )
		{
			visualPointTick = 0;
		}
		else if( visualPointTick2 > VISUAL_POINT_TICK_LIMIT2 )
		{
			visualPointTick2 = 0;
		}
		else
		{
			visualPointTick++;
			visualPointTick2++;
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
			
			if( gameSecondsCount == (int) SECONDS_TO_END_GAME * 0.8)
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
		 *  Runs every 10 ticks.
		 *    - 20 ticks = 1 second
		 */
		if (GameState == GameStates.Running){

			/**
			 *  Timer for showing Point Visualization
			 */
			visualTicker();
			
			/**
			 *  Show visuals at weapons and bonus effect locations
			 */
			for( Weapon w : weapons )
			{
				showVisual(w.getWeaponLocation(), "weapon", "none");
				
				/**
				 *  If respawn timer has a value > 0.
				 *   - Decrement Weapon Respawn timer.
				 *   - Flag that it needs respawned.
				 */
				int respawnTimeRemaining = w.getRespawnTimer();
				if( respawnTimeRemaining > 0 )
				{
					respawnTimeRemaining -= 10;
					w.setRespawnTimer(respawnTimeRemaining);
					
					/**
					 *  If respawn timer gets to 0, respawn weapon in cobweb.
					 */
					if( respawnTimeRemaining <= 0 )
					{
						w.getWeaponLocation().getBlock().setType(Material.WEB);
						Bukkit.getWorld("World").dropItem(w.getWeaponLocation(), w.getWeaponItemStack()).setVelocity(new Vector());
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
					double radius = 2D;
					
					List<Entity> nearbyEntities = w.getWeaponLocation().getWorld().getEntities();
					for(Entity e : nearbyEntities) {
					    if( e instanceof ItemStack )
					    {
					    	if(e.getLocation().distance(w.getWeaponLocation()) <= radius)
					    	{
					    		if( ((ItemStack)e).hasItemMeta())
					    		{
					    			if( ((ItemStack) e).getItemMeta().hasDisplayName() )
					    			{
					    				if( ((ItemStack) e).getItemMeta().getDisplayName().equals(w.getWeaponItemStack().getItemMeta().getDisplayName()) )
					    				{
					    					e.setTicksLived(0);
					    					e.setVelocity(new Vector());
					    					//e.setLocation();  //  TODO  No method to set ItemStack Location ????
					    				}
					    			}
					    		}
					    	}
					    }
					}
				}
			}
			
			/**
			 *  Game Length Timer
			 */
			gameTimer();
			
			/**
			 *  Process Kill Streak Timer
			 */
			if( killStreakTimer.size() > 0 )
			{
				Iterator<Map.Entry<Player, Integer>> it = killStreakTimer.entrySet().iterator();

				while (it.hasNext()) {
					Map.Entry<Player, Integer> entry = it.next();

					Player p = entry.getKey();

					int killStreakWindow = killStreakTimer.get(p);
					killStreakWindow -= 10;

					if (killStreakWindow <= 0) {
						p.setExp(0);
						killStreakTimer.put(p, 0);
						killStreakMultiplier.put(p, 0);
					} else {
						float percentRemaining = (float) killStreakWindow / (KILL_STREAK_TIME*20);
						p.setExp(percentRemaining);
						killStreakTimer.put(p, killStreakWindow);
					}
				}
			}
			
			/**
			 *  Check for Game Over
			 */
			if( gameSecondsCount >= SECONDS_TO_END_GAME || leadingKillsScore >= KILLS_TO_WIN_GAME )
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
	}

	public void StartGame(){

		Bukkit.getServer().getWorld("World").playSound(lobbySpawn,  Sound.BAT_DEATH, 100, 1);
		
		gameModeSetup();
		
		if( gameMode.equals("teams") )
		{
			assignPlayersToTeams(Bukkit.getOnlinePlayers());
		}

		GameState= GameStates.Running;

		for (Player p : Bukkit.getOnlinePlayers())
		{
			score = kills.getScore(p);
			
			score.setScore(0);
		}
		
		if( gameMode.equals("teams") )
		{
			for( Team t : teamsArray )
			{
				calculateTeamScore(t);
			}
		}
		
		for (Player p : Bukkit.getOnlinePlayers())
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
			 *  Initialize Kill Streak Time
			 */
			killStreakTimer.put(p, 0);
			killStreakMultiplier.put(p, 0);
			
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
		 *  Spawn Weapons
		 *   - Set velocity to zero
		 *   - Spawn in cobweb
		 *   - Track Entity at location
		 *      -- Reset time lived
		 *      -- Respawn timer when picked up
		 */ 
		for( Weapon w : weapons )
		{
			w.getWeaponLocation().getBlock().setType(Material.WEB);
			Bukkit.getWorld("World").dropItem(w.getWeaponLocation(), w.getWeaponItemStack()).setVelocity(new Vector());
		}
	}
}
