
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

import org.bukkit.Location;

public class ForceField {



	private Double minX;
	private Double minY;
	private Double minZ;

	private Double maxX;
	private Double maxY;
	private Double maxZ;
	public ForceField(Double minX, Double minY, Double minZ, Double maxX,
			Double maxY, Double maxZ) {
		super();
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	public boolean locationInside(Location l){

		if ( l.getX() >= minX && l.getX() <= maxX && l.getY() >= minY && l.getY() <= maxY  && l.getZ() >= minZ && l.getZ() <= maxZ  )
		{
			return true;
		}

		return false;
	}

}
