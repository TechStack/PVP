
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

package com.projectreddog.pvp;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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
 *    - Team PVP Combat (?)
 *    - Power-ups
 *       -- Add Effects or Enchantments
 *       -- Visual Effect / Collectible
 *    - Weapons
 *       -- Customizable via Config
 *           --- Item Abilities
 *           --- Item Locations
 *           --- Item Respawn (and Duration?)
 *    - Scoring
 *       -- Scoreboard
 *       -- Game Over Limits
 *           --- Time
 *           --- Kills
 *           --- Default (and customizable in-game?)
 *           --- Penalties?
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
	private String MapName;
	private String gameMode;

	/**
	 *  Capture Point Visualization Variables
	 */
	private static final int VISUAL_POINT_TICK_LIMIT = 1;
	private static final int VISUAL_POINT_TICK_LIMIT2 = 3;
	private int visualPointTick = VISUAL_POINT_TICK_LIMIT;
	private int visualPointTick2 = VISUAL_POINT_TICK_LIMIT2;
	
	/**
	 *  General Game Variables
	 */
	private static int SECONDS_TO_END_GAME;
	private static int KILLS_TO_WIN_GAME;
	private int numSpawnPoints;
	private int gameSecondsCount = 0;
	private int timeTickedTimer = 1;
	
	/**
	 *  Team Variables
	 */
	public Team[] teamsArray;
	public int numTeams;
	public ArrayList<Team> Teams = new ArrayList<Team>() ;
	private   Map<String, String> playerTeam ;
	public static Random rand = new Random();
	
	/**
	 *  TimeTicked() Variables
	 */
	private GameStates GameState;
	private int TimeTillStart=300;
	private int coolDownTimer=10;
	private BukkitTask task ;

	/**
	 *  Scoreboard Variables
	 */
	Objective kills;
	ScoreboardManager manager;
	Scoreboard board;
	org.bukkit.scoreboard.Team[] SBTeam;
	private Score score;
	
	/**
	 *  Enlighten Enchantment Variables
	 *   - 10 would be 10%
	 */
	public int enlightenChance; 
	
	public void onEnable() {

		/**
		 * ======================================  Main Game Setup ======================================
		 */
		GameState =GameStates.Lobby;
		playerTeam = new HashMap<String, String>();

		getServer().getPluginManager().registerEvents(this, this);
		logger= this.getLogger();

		this.saveDefaultConfig();

		/**
		 *  Bounding box for the game force field		
		 */
		cubeMaxX = this.getConfig().getInt("cubeMaxX");
		cubeMinX = this.getConfig().getInt("cubeMinX");
		cubeMaxZ = this.getConfig().getInt("cubeMaxZ");
		cubeMinZ = this.getConfig().getInt("cubeMinZ");
		getLogger().info("MAX X" + cubeMaxX);
		getLogger().info("Min X" + cubeMinX);
		getLogger().info("MAX Z" + cubeMaxZ);
		getLogger().info("Min Z" + cubeMinZ);
		
		double lobbyX, lobbyY, lobbyZ;
		lobbyX = centerBlockValue(this.getConfig().getInt("LobbySawn.X"));
		lobbyY = this.getConfig().getInt("LobbySawn.Y");
		lobbyZ = centerBlockValue(this.getConfig().getInt("LobbySawn.Z"));
		lobbySpawn = new Location(Bukkit.getWorld("World"), lobbyX, lobbyY, lobbyZ);
		
		/**
		 *  Game Options Setup
		 */
		SECONDS_TO_END_GAME = this.getConfig().getInt("SecondsToEndGame");
		KILLS_TO_WIN_GAME = this.getConfig().getInt("KillsToWinGame");
		gameMode = this.getConfig().getString("GameMode");
		enlightenChance = this.getConfig().getInt("EnlightenChance");
		
		
		/**
		 * ======================================  Spawn Points Setup ======================================
		 */
		numSpawnPoints = this.getConfig().getInt("NumSpawnPoints");
		spawnPoints = new Location[numSpawnPoints];
		double pointX, pointY, pointZ, pointYaw;
		
		for( int i=1; i <= numSpawnPoints; i++ ) {
			pointX = centerBlockValue(this.getConfig().getDouble("SpawnPoints.Point" + i + ".X"));
			pointY = this.getConfig().getDouble("SpawnPoints.Point" + i + ".Y");
			pointZ = centerBlockValue(this.getConfig().getDouble("SpawnPoints.Point" + i + ".Z"));
			pointYaw = this.getConfig().getDouble("SpawnPoints.Point" + i + ".Yaw");
			spawnPoints[i-1] = new Location(Bukkit.getWorld("World"), pointX, pointY, pointZ);
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
		 *  ======================================  Scoring Setup ======================================
		 *   - Scoreboard Setup
		 */
		manager = Bukkit.getScoreboardManager();
		board = manager.getNewScoreboard();
		
		kills = board.registerNewObjective("kills", "playerKillCount");
		kills.setDisplaySlot(DisplaySlot.SIDEBAR);
		kills.setDisplayName("Kills:");

		MapName = this.getConfig().getString("MapName");
		logger.info("Map Name: " + MapName);
		task= new PVPRunnable(this).runTaskTimer(this, 10, 10);
	}
	
	public void onDisable(){
		task.cancel();
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (e.getEntity() instanceof Player){
			
			/**
			 *  A player killed another player.  Allow chance to enlighten the killer.
			 *  
			 *  Also, increment scoreboard (Kills).
			 */
			if (e.getEntity().getKiller() instanceof Player){
				/**
				 *  Enlighten
				 */
				enlighten(e.getEntity().getKiller());
				
				/**
				 *  TODO Increment Winning Kills tracking
				 */
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

		}else{
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
				//  Cobblestone is permitted.
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
	public void onServerPing ( ServerListPingEvent e ){
		e.setMotd(MapName );
		if (GameState== GameStates.Running){
			e.setMaxPlayers( Bukkit.getOnlinePlayers().length);
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e ){
		// player attempted to move

		if( GameState == GameStates.Running){

			/**
			 *  DeathCube perimeter check.  Fry player if outside of cube.
			 */
			if( e.getTo().getX()>cubeMaxX   || e.getTo().getX()<cubeMinX || e.getTo().getZ()> cubeMaxZ || e.getTo().getZ() <cubeMinZ){
				// the player should be outside the bounds of the death cube  

				//getLogger().info(e.getTo().toString());
				e.getPlayer().damage(5);


				fryPlayer(e.getPlayer());
			}
		}
	}

	@EventHandler
	public void onPlayerJoin ( PlayerJoinEvent e){
		if (!(playerTeam.containsKey(e.getPlayer().getName()))){
			//new player set them up under NONE team and tp them to the lobby first
			SetPlayersTeam(e.getPlayer(),"NONE");
			e.getPlayer().teleport(lobbySpawn );
			if (GameState == GameStates.Running){
				Player[] onePlayer = {e.getPlayer()};
				
				assignPlayersToTeams(onePlayer);
				
				SpawnPlayerInGame(e.getPlayer());
			}
		}else{
			/**
			 *  Player is on a team already; need to setup his / her scoreboard 
			 */
			SetPlayersTeam (e.getPlayer(), getTeamByPlayer(e.getPlayer()).getName() );
		}
	}

	@EventHandler
	public void onPlayerRespawn( PlayerRespawnEvent e){
		if( GameState == GameStates.Running){
			SpawnPlayerInGame(e.getPlayer());
			//debug clear inventory 
			e.getPlayer().getInventory().clear();
			final PlayerRespawnEvent f =e;
			e.setRespawnLocation(getTeamByPlayer(e.getPlayer()).getDeathPenalty() );
			Bukkit.getScheduler().scheduleSyncDelayedTask(this , new Runnable() {

				@Override
				public void run() {
					f.getPlayer().teleport( getTeamByPlayer(f.getPlayer()).getSpawn());
					//debug give them the gear after they spawn in the base so they can't attack each other in the penalty box
					GivePlayerGear(f.getPlayer());
				}
			},deathPenaltyAmt);
			
		}else 
		{
			/**
			 *  If game is not Running, set player respawn to the lobby.
			 */
			e.setRespawnLocation(lobbySpawn);
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("startgame")){
			StartGame();
			return true;  // Return true when a command is executed successfully.
		}
		
		if(cmd.getName().equalsIgnoreCase("penalty")){
			
			if (( sender instanceof ConsoleCommandSender) || ( sender instanceof BlockCommandSender))
			{
				Bukkit.broadcastMessage("Current Penalty Time: " + (int) deathPenaltyAmt/20 + "seconds.");
			}
			else if( sender instanceof Player)
			{
				((Player)sender).sendMessage("Current Penalty Time: " + (int) deathPenaltyAmt/20 + " seconds.");
			}
			
			return true;  // Return true when a command is executed successfully.
		}
		
		if(cmd.getName().equalsIgnoreCase("gamestats")){
			
			if (( sender instanceof ConsoleCommandSender) || ( sender instanceof BlockCommandSender))
			{
				Bukkit.broadcastMessage("DeathCube - " + MapName + " - Statistics:\n - Game Time: " + gameSecondsCount + " seconds.\n - Game Time Limit: " + SECONDS_TO_END_GAME + " seconds.\n - Current Penalty Time: " + (int) deathPenaltyAmt/20 + " seconds.");
			}
			else if( sender instanceof Player)
			{
				((Player)sender).sendMessage("DeathCube - " + MapName + " - Statistics:\n - Game Time: " + gameSecondsCount + " seconds.\n - Game Time Limit: " + SECONDS_TO_END_GAME + " seconds.\n - Current Penalty Time: " + (int) deathPenaltyAmt/20 + " seconds.");
			}
			
			return true;  // Return true when a command is executed successfully.
		}
		
		return false;  // Otherwise, return false. 
	}
	
	public void gameModeSetup() {
		if( gameMode.equals("freeforall") ) {
			
		}
		else if( gameMode.equals("team") ) {
			/**
			 *  Set up Teams
			 */
			numTeams = this.getConfig().getInt("NumTeams");
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
				
				Teams.add(teamsArray[i-1]);
			}
		}
		
		/**
		 *  Team Scoreboard Setup
		 */
		for(int i=0; i <= numTeams-1; i++)
		{
			SBTeam[i] = board.registerNewTeam(teamsArray[i].getName());
			SBTeam[i].setDisplayName(teamsArray[i].getName());
			SBTeam[i].setCanSeeFriendlyInvisibles(true);
			SBTeam[i].setAllowFriendlyFire(false);
			SBTeam[i].setPrefix(teamsArray[i].getChatColor() +"");
			SBTeam[i].setSuffix(ChatColor.WHITE+"");
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
				p.setScoreboard(board);
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
			p.setScoreboard(board);
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
				
		// Logger
		//getLogger().info("Start Team Assignment");
		
		/**
		 *  Initially populate each team with zero players.
		 */
		for( Team t : teamsArray )
		{
			t.setPlayerCount(0);
		}
		
		// Logger
		//getLogger().info("Team Player Counts set to zero.");
		
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
		
		// Logger
		//getLogger().info("Counted players on each team.");
		
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
		
		// Logger
		//getLogger().info("Found best team match.");
		
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
	
	public String getFormattedScorboardName(ChatColor chatcolor, String PointName)
	{
		if (PointName.length() > 14)
		{
			PointName= PointName.substring(0, 14);
		}
		return chatcolor + PointName+ ":";
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

	public boolean willEnlighten(){
		// get a random number between 0 and 100
		double tmp =Math.random()*100;
		// if the number is less than the enlighten chance then we return true ( should enlighten)
		if( tmp <= enlightenChance){
			return true;
		}else 
		{
			return false;
		}

	}

	public void enlighten(Player p){
		// this method will enchant a players weapon (sword or bow) 
		if (willEnlighten()){
			Enchantment tmpHoldEnchantment = getEchantmentByWeaponType(p.getItemInHand().getType());
			if( tmpHoldEnchantment != null){			
				addEnchantLevel(tmpHoldEnchantment,p,5);

			}
		}

	}
	
	public void playSoundForTeam( String teamName, Sound snd, float volume, float pitch){
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (playerTeam.get(p.getName()).equals(teamName)){
				p.playSound(p.getLocation(), snd, volume, pitch);
			}
		}
	}
	
	public void SpawnPlayerInGame (Player p){
		if (GameState == GameStates.Running){
			p.setHealth(p.getMaxHealth());
			p.setFoodLevel(20);
			GivePlayerGear (p);
			PutPlayerInBase(p);
		}
	}
	
	public void PutPlayerInBase(Player p){
		// need to find spawn location
		p.teleport(getTeamByPlayer(p).getSpawn() );
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
		 *   ---
		 *  Helmet - If "LEATHER", set color
		 */
		ItemStack helmet = new ItemStack(Material.getMaterial(this.getConfig().getString("Inventory.Armor.Helmet.Name")), 1);
		if((this.getConfig().getString("Inventory.Armor.Helmet.Name")).contains("LEATHER"))
		{   // Color Helmet
			colorArmor(getTeamByPlayer(p), helmet);
		}
		// Enchant Helmet
		configPrefix = "Inventory.Armor.Helmet";
		enchantItem(configPrefix, helmet);
		// Add Helmet
		p.getInventory().setHelmet(helmet);
		
		/**
		 *  ChestPlate - If "LEATHER", set color
		 */
		ItemStack chestPlate = new ItemStack(Material.getMaterial(this.getConfig().getString("Inventory.Armor.ChestPlate.Name")), 1);
		if((this.getConfig().getString("Inventory.Armor.ChestPlate.Name")).contains("LEATHER"))
		{
			colorArmor(getTeamByPlayer(p), chestPlate);
		}
		// Enchant ChestPlate
		configPrefix = "Inventory.Armor.ChestPlate";
		enchantItem(configPrefix, chestPlate);
		// Add ChestPlate
		p.getInventory().setChestplate(chestPlate);
		
		/**
		 *  Leggings - If "LEATHER", set color
		 */
		ItemStack leggings = new ItemStack(Material.getMaterial(this.getConfig().getString("Inventory.Armor.Leggings.Name")), 1);
		if((this.getConfig().getString("Inventory.Armor.Leggings.Name")).contains("LEATHER"))
		{
			colorArmor(getTeamByPlayer(p), leggings);
		}
		// Enchant Leggings
		configPrefix = "Inventory.Armor.Leggings";
		enchantItem(configPrefix, leggings);
		// Add Leggings
		p.getInventory().setLeggings(leggings);
		
		/**
		 *  Boots - If "LEATHER", set color
		 */
		ItemStack boots = new ItemStack(Material.getMaterial(this.getConfig().getString("Inventory.Armor.Boots.Name")), 1);
		if((this.getConfig().getString("Inventory.Armor.Boots.Name")).contains("LEATHER"))
		{
			colorArmor(getTeamByPlayer(p), boots);
		}
		// Enchant Boots
		configPrefix = "Inventory.Armor.Boots";
		enchantItem(configPrefix, boots);
		// Add Boots
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
			if(enchant.canEnchantItem(itemToEnchant))
			{
				itemToEnchant.addUnsafeEnchantment(enchant, this.getConfig().getInt(configPrefix + ".Enchant" + j + ".Level"));
			}
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
	
	public void showPointVisual(){
		/**
		 *  Create visual effect for players on active point.
		 */
		for (Player p : Bukkit.getOnlinePlayers())
		{
			Team t = getTeamByPlayer(p);
			if ( !(t==null)){
				/**
				 *  Get location of active point.
				 */
				int index = t.getActivePointIndex();
				BoundingBox activePoint = t.getPointByIndex(index);
				Location loc = activePoint.getLocation();
				
				/**
				 *  Play visual capture point effect for player
				 */
				if( visualPointTick2 >= VISUAL_POINT_TICK_LIMIT2 )
				{
					p.playEffect(loc, Effect.ENDER_SIGNAL, null);
				}
					
				p.playEffect(loc, Effect.SMOKE, 4);
				loc.add(0, 1, 0);
				p.playEffect(loc, Effect.SMOKE, 4);
			}
		}
		
		/**
		 *  Reset the timer for the longer interval effect (ENDER_SIGNAL).
		 */
		if( visualPointTick2 >= VISUAL_POINT_TICK_LIMIT2 )
		{
			visualPointTick2 = 0;
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
			
			if(gameSecondsCount >= SECONDS_TO_END_GAME)
			{
				/**
				 *  Broadcast message and play sound when game timer reaches End Game time limit.
				 */
				if(gameSecondsCount == SECONDS_TO_END_GAME)
				{
					Bukkit.broadcastMessage("Time: " + (int) SECONDS_TO_END_GAME/60 + " minutes. Increasing death penalty time every minute starting now!");
					
					for (Team t: Teams){
						playSoundForTeam(t.getName(), Sound.ENDERMAN_STARE, 1, 1);
					}
				}
				
				/**
				 *  Increase Death Penalty value after set time limit.
				 *    - Default time = 600 seconds = 10 minutes
				 */
				if( (gameSecondsCount - SECONDS_TO_END_GAME) % 60 == 0 )
				{
					/**
					 *  Every minute after the end game time limit, increase death penalty by 2 seconds (40 ticks).
					 *  
					 *  Limit to a max death penalty time.
					 */
					if( (deathPenaltyAmt/20) < MAX_DEATH_PENALTY )
					{
						deathPenaltyAmt += DEATH_PENALTY_INCREASE;
						
						/**
						 *  Periodic Update on Penalty Time Amount
						 */
						if( (deathPenaltyAmt/20) % 10 == 0 || (deathPenaltyAmt/20) % 15 == 0 )
						{
							Bukkit.broadcastMessage("Current Penalty Time: " + (int) deathPenaltyAmt/20 + "seconds.");
						}
					}
				}
			}
		}
	}
	
	/**
	 *  Runs every 10 ticks.
	 *    - 20 ticks = 1 second
	 */
	public void TimeTicked() {
		//Bukkit.broadcastMessage("TESTING !");
		if (GameState == GameStates.Running){
			for (Team t: Teams){
				//logger.info(t.getName());
				for (BoundingBox point : t.getPoints()){
					//t.isPrevPointCapped(point.getPointIndex())==false
					boolean processPoint= false;

					if (point.getPointIndex() == 0)
					{
						if (point.isCapped()==false){
							processPoint =true;
							

							score = objective.getScore(Bukkit.getOfflinePlayer( getFormattedScorboardName(t.getChatColor() , point.getBoxName()  )));
							if (score.getScore()==0){
								score.setScore((int) (point.getTimeForPoint() - point.getTimeOnPoint()));
							}

						}
					}else 	if (  t.getPoints().get(point.getPointIndex()-1).isCapped()   ){

						if (point.isCapped()==false){
							processPoint =true;
							score = objective.getScore(Bukkit.getOfflinePlayer( getFormattedScorboardName(t.getChatColor() , point.getBoxName()  )));
							if (score.getScore()==0){
								score.setScore((int) (point.getTimeForPoint() - point.getTimeOnPoint()));
							}
						}
					}
					if (    processPoint)
					{
						setTeamCompass(t.getName(),point.getLocation());
						//logger.info ("Processing Point " + point.getBoxName());
						if (teamHasPlayerOnPoint(t, point.getBoxName())){

							/**
							 *  Initial message that point capture has begun.
							 */
							if ( Math.abs(point.getTimeOnPoint() - 0.0) < 0.00001 ){
								Bukkit.broadcastMessage( t.getChatColor() + t.getName() + ChatColor.WHITE + " has Started to Capture Point " + point.getBoxName());
							}
							
							/**
							 *  Increment time on point.
							 *    - Adding 0.5 will count seconds rather than half-seconds.
							 */
							point.setTimeOnPoint(point.getTimeOnPoint() + 0.5D);

							/**
							 *  Update Scoreboard with time on point.
							 */
							score = objective.getScore(Bukkit.getOfflinePlayer( getFormattedScorboardName(t.getChatColor() , point.getBoxName()  )));
							score.setScore((int) (point.getTimeForPoint() - point.getTimeOnPoint()));

							/**
							 *  The point has been captured!
							 */
							if (point.getTimeOnPoint()>= point.getTimeForPoint() ){
								/**
								 *  Message and sound about point being captured.
								 */
								Bukkit.broadcastMessage( t.getChatColor() + t.getName() + ChatColor.WHITE + " has Captured Point " +point.getBoxName());
								playSoundForTeam(t.getName(), Sound.LEVEL_UP, 1, 1);
								
								/**
								 *  Update Scoreboard by removing the scoreboard from the team that has captured the point
								 */
								board.resetScores(Bukkit.getOfflinePlayer(  t.getChatColor() + point.getBoxName()  + ":"));

								//	logger.info("the Point " + point.getBoxName() + " just was capped it flag before update was " + point.isCapped() );
								/**
								 *  Mark point as being captured
								 */
								point.setCapped(true );
								
								/**
								 *  Point Visualization
								 *    - Set timer to limit so that next point is given a visual effect immediately
								 *    - Next Point Index should be less than or equal to the Total Number of Points minus 1 (because it's an index)
								 */
								visualPointTick = VISUAL_POINT_TICK_LIMIT;
								
								if(point.getPointIndex() + 1 <= pointCount-1)
									t.setActivePointIndex(point.getPointIndex() + 1);
								
								//	logger.info("the Point " + point.getBoxName() + " just was capped it flag after update was " + point.isCapped() );
								/**
								 *  If second-to-last point captured, disable team force field.
								 */
								if (pointCount-2 == point.getPointIndex()){
									t.setCanBypassForceField(true);
									Bukkit.broadcastMessage(t.getChatColor() +  t.getName() + ChatColor.WHITE + " Team can now penetrate the Force Field! ");
								}
								
								/**
								 *  Last point captured; Game Over!
								 */
								if (pointCount-1 == point.getPointIndex()){
									GameState = GameStates.GameOver;
									Bukkit.broadcastMessage(t.getChatColor() +  t.getName() + " Wins!");
									
									/**
									 *  Play separate sounds for winning and losing teams.
									 */
									for (Team t2: Teams){
										if(t2 == t)
											playSoundForTeam(t2.getName(), Sound.ENDERDRAGON_DEATH, 1, 1);
										else
											playSoundForTeam(t2.getName(), Sound.ZOMBIE_IDLE, 10, (float) 0.25);
									}

								}
							}

						}else
						{
							/**
							 *  A point is not being processed, so set time to 0 and update scoreboard.
							 */
							point.setTimeOnPoint(0);

							score = objective.getScore(Bukkit.getOfflinePlayer( getFormattedScorboardName(t.getChatColor() , point.getBoxName()  )));
							score.setScore((int) (point.getTimeForPoint() - point.getTimeOnPoint()));
						}
					}
				}
			}
			
			/**
			 *  Timer for showing Point Visualization
			 */
			if(visualPointTick >= VISUAL_POINT_TICK_LIMIT){				
				showPointVisual();
				visualPointTick = 0;
			}
			else
			{
				visualPointTick++;
				visualPointTick2++;
			}
				
			/**
			 *  Game Length Timer
			 */
			gameTimer();
				
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

			/**
			 *  Change visible scoreboard.
			 */
			if(resetEndScoreboard)
			{
				for (Player p : Bukkit.getOnlinePlayers())
				{
					p.setScoreboard(postGameBoard);
				}
				
				Bukkit.broadcastMessage("Changing to Kills Scoreboard...");
				resetEndScoreboard = false;
			}
			
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
		assignPlayersToTeams(Bukkit.getOnlinePlayers());

		GameState= GameStates.Running;

		for (Player p : Bukkit.getOnlinePlayers())
		{
			/**
			 *  Avoid death by falling.
			 */
			Vector vec = new Vector();
			p.setVelocity(vec);
			
			/**
			 *  Clear all effects acquired in lobby.
			 */
			for (PotionEffect effect : p.getActivePotionEffects())
			{
				p.removePotionEffect(effect.getType());
			}
			
			/**
			 *  Join game.
			 */
			SpawnPlayerInGame(p);
		}
	}
}
