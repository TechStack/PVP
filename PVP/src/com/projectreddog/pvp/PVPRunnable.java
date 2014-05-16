
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

import org.bukkit.scheduler.BukkitRunnable;

public class PVPRunnable extends BukkitRunnable {

    public PVPRunnable(PVP plugin) {
		super();
		this.plugin = plugin;
	}
	private final PVP plugin;
	@Override
	public void run() {
		
	this.plugin.TimeTicked();
		
	}

}
