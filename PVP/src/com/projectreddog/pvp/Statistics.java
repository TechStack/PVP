package com.projectreddog.pvp;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Statistics {
	
	/**
	 *  Statistics:
	 *   - Create one Statistics Object and initialize at startGame()
	 *   - Add more players onPlayerJoin()
	 *   - processKillStreakTimer() every timeTicked()
	 *   - updateStatistics() onDeath()
	 *   - 
	 * 
	 *  Global Variables
	 */
	private Map<Player, Integer> killStreakTimer;
	private Map<Player, Integer> killStreakMultiplier;
	private Map<Player, Integer> killsSinceLastDeath;
	private Map<Player, Integer> playerDeaths;
	
	private static final int KILL_STREAK_TIME = 10;
	
	private int mostDeaths = 0;
	private int longestSpree = 0;
	private int highestMultiKill = 0;
	
	private Player mostDeathsPlayer;
	private Player longestSpreePlayer;
	private Player highestMultiKillPlayer;
	
	public Statistics() {
		/**
		 *  Constructor
		 */
		for (Player p : Bukkit.getOnlinePlayers())
		{
			addPlayer(p);
		}
	}

	public int getMultiplier(Player p) {
		return killStreakMultiplier.get(p);
	}
	
	public int getMostDeaths() {
		return mostDeaths;
	}
	
	public String getMostDeathsPlayer() {
		if( mostDeathsPlayer != null )
			return mostDeathsPlayer.getName();
		else
			return "N/A";
	}
	
	public int getLongestSpree() {
		return longestSpree;
	}
	
	public String getLongestSpreePlayer() {
		if( longestSpreePlayer != null )
			return longestSpreePlayer.getName();
		else
			return "N/A";
	}
	
	public int getHighestMultiKill() {
		return highestMultiKill;
	}
	
	public String getHighestMultiKillPlayer() {
		if( highestMultiKillPlayer != null )
			return highestMultiKillPlayer.getName();
		else
			return "N/A";
	}
	
	public void addPlayer(Player p) {
		killStreakTimer.put(p, 0);
		killStreakMultiplier.put(p, 0);
		killsSinceLastDeath.put(p, 0);
		playerDeaths.put(p, 0);
	}
	
	public void showStats() {
		Bukkit.broadcastMessage("Most Deaths: " + getMostDeathsPlayer() + " - " + mostDeaths + " deaths.");
		Bukkit.broadcastMessage("Longest Spree: " + getLongestSpreePlayer() + " - " + longestSpree + " deaths.");
		Bukkit.broadcastMessage("Highest Multi-kill: " + getHighestMultiKillPlayer() + " - " + highestMultiKill + " deaths.");
	}
	
	public void updateStatistics(Player victim, Player killer) {
		/**
		 *  Track:
		 *   - Most Deaths
		 *   - Longest Spree
		 *   - Highest Multi-kill
		 */
		
		/**
		 *  Increment Deaths Count
		 */
		int tempDeathAmt = playerDeaths.get(victim);
		playerDeaths.put(victim, tempDeathAmt++);
		if( tempDeathAmt > mostDeaths )
		{
			mostDeaths = tempDeathAmt;
			mostDeathsPlayer = victim;
		}
		
		/**
		 *  Remove them from the Kill Streak Timer, if they're on it.
		 */
		if( killStreakTimer.containsKey(victim) ) {
			victim.setExp(0);
			killStreakTimer.put(victim, 0);
			killsSinceLastDeath.put(victim, 0);
		}
		
		if ( killer != null ){
			/**
			 *  A player killed another player.  Allow chance to enlighten the killer.
			 *  
			 *  Also, increment scoreboard (Kills).
			 */
			
			int runningKills = killsSinceLastDeath.get(killer);
			killsSinceLastDeath.put(killer, runningKills++);
			if( runningKills > longestSpree )
			{
				longestSpree = runningKills;
				longestSpreePlayer = killer;
			}
			
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
				if( multiplier > highestMultiKill )
				{
					highestMultiKill = multiplier;
					highestMultiKillPlayer = killer;
				}
				
				/**
				 *  Multiplier names.
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
				
				if( multiplier < 10 )
					((Player)killer).sendMessage(streakName);
				else
					((Player)killer).sendMessage(multiplier + "x Kill Streak!");
			}
			else
				killStreakMultiplier.put(killer, 1);
			
			/**
			 *  Check Kills Since Last Death
			 */
			if( runningKills >= 5 )
			{
				String spreeName = "";
				switch( runningKills ) {
				case 5:
					spreeName = "Killing Spree!";
					break;
				case 10:
					spreeName = "Killing Frenzy!";
					break;
				case 15:
					spreeName = "Running Riot!";
					break;
				case 20:
					spreeName = "Rampage!";
					break;
				case 25:
					spreeName = "Untouchable!";
					break;
				case 30:
					spreeName = "Invincible!";
					break;
				}
				
				if( runningKills <= 30 )
				{
					((Player)killer).sendMessage(spreeName);
					Bukkit.broadcastMessage(killer.getName() + " - " + spreeName);
				}
				else if( runningKills > 30 )
				{
					if( runningKills % 5 == 0 )
						((Player)killer).sendMessage(runningKills + " Kill Spree!");
				}
			}
			
			/**
			 *  Allow # seconds to continue kill streak.
			 */
			killStreakTimer.put(killer, KILL_STREAK_TIME*20);
		}
	}

	public void processKillStreakTimer() {
		/**
		 *  If any players are on a killing streak, decrement their timers.
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
	}
}
