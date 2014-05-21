
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

import org.bukkit.ChatColor;
import org.bukkit.Color;

public class Team {
    
	private String name;
	
	private ForceField forcefield;
	
	private Color armorColor;
	private ChatColor chatColor;
	
	private Boolean bypassForceField = false;
	
	private int playerCount = 0;
	
	private int teamScore = 0;

	
	public Team(String name, Color armorColor, ChatColor chatColor) {
		this.name = name;
		this.armorColor = armorColor;
		this.chatColor = chatColor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Color getArmorColor() {
		return armorColor;
	}

	public void setArmorColor(Color armorColor) {
		this.armorColor = armorColor;
	}

	public ChatColor getChatColor() {
		return chatColor;
	}

	public void setChatColor(ChatColor chatColor) {
		this.chatColor = chatColor;
	}

	public ForceField getForcefield() {
		return forcefield;
	}

	public void setForcefield(ForceField forcefield) {
		this.forcefield = forcefield;
	}
	
	public void setCanBypassForceField(Boolean canBypass) {
		bypassForceField = canBypass;
	}
	
	public Boolean getCanBypassForceField() {
		return bypassForceField;
	}
	
	public void setPlayerCount(int count) {
		playerCount = count;
	}
	
	public int getPlayerCount() {
		return playerCount;
	}
	
	public void setTeamScore(int scoreValue) {
		teamScore = scoreValue;
	}
	
	public int getTeamScore() {
		return teamScore;
	}
}
