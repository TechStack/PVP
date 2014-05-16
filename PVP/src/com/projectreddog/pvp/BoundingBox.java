
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

import org.bukkit.Bukkit;
import org.bukkit.Location;


public class BoundingBox {

	private double x1;
	private double y1;
	private double z1;
	private double x2;
	private double y2;
	private double z2;
	private double origR;
	private String boxName;
	private double timeOnPoint;
	private int timeForPoint;
	private boolean Capped;
	private int pointIndex;
	public BoundingBox(double x, double y , double z, double r , String boxName, int timeForPoint,int pointIndex) {
		x1= x-r;
		x2= x+r;	

		y1= y-r;
		y2= y+r;	


		z1= z-r;
		z2= z+r;	
		setOrigR(r);
		this.setBoxName(boxName);
		this.timeForPoint = timeForPoint;
		this.pointIndex =pointIndex;
	}

	public boolean locationInside(Location l){
		if ( l.getX() >x1 && l.getX() <x2 && l.getY()> y1 && l.getY() < y2 && l.getZ() > z1 && l.getZ() < z2 )
		{
			return true;
		}else {
			return false;
		}
	}

	public String getBoxName() {
		return boxName;
	}

	public void setBoxName(String boxName) {
		this.boxName = boxName;
	}

	public double getOrigR() {
		return origR;
	}

	public void setOrigR(double origR) {
		this.origR = origR;
	}
    public Location getLocation(){
    	return new Location(Bukkit.getWorld("World"), x1+origR, y1+origR, z1+origR);
    }
}
