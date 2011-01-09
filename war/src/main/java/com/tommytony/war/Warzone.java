package com.tommytony.war;
import org.bukkit.*;
import org.bukkit.block.Sign;

import com.tommytony.war.volumes.CenteredVolume;
import com.tommytony.war.volumes.VerticalVolume;
import com.tommytony.war.volumes.Volume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Warzone {
	private String name;
	private VerticalVolume volume;
	private Location northwest;
	private Location southeast;
	private final List<Team> teams = new ArrayList<Team>();
	private final List<Monument> monuments = new ArrayList<Monument>();
	
	private Location teleport;
	private boolean friendlyFire;
	private War war;
	private int lifePool;
	private HashMap<Integer, ItemStack> loadout; 
	
	private HashMap<String, HashMap<Integer, ItemStack>> inventories = new HashMap<String, HashMap<Integer, ItemStack>>();
	private World world;
	
	public Warzone(War war, World world, String name) {
		this.war = war;
		this.world = world;
		this.name = name;
		this.friendlyFire = war.getDefaultFriendlyFire();
		this.setLifePool(war.getDefaultLifepool());
		this.setLoadout(war.getDefaultLoadout());
	}
	
	public boolean ready() {
		if(getNorthwest() != null && getSoutheast() != null 
				&& !tooSmall() && !tooBig()) return true;
		return false;
	}
	
	public boolean tooSmall() {
		if((getSoutheast().getBlockX() - getNorthwest().getBlockX() < 20)
				|| (getNorthwest().getBlockZ() - getSoutheast().getBlockZ() < 20)) return true;
		return false;
	}
	
	public boolean tooBig() {
		if((getSoutheast().getBlockX() - getNorthwest().getBlockX() > 1000)
				|| (getNorthwest().getBlockZ() - getSoutheast().getBlockZ() > 1000)) return true;
		return false;
	}
	
	public List<Team> getTeams() {
		return teams;
	}
	
	public Team getPlayerTeam(String playerName) {
		for(Team team : teams) {
			for(Player player : team.getPlayers()) {
				if(player.getName().equals(playerName)) {
					return team;
				}
			}
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public void setNorthwest(Location northwest) {
		// remove old nw sign, if any (replace with air)
		if(this.northwest != null) {
			removeNorthwest();
		}
		this.northwest = northwest;
		this.volume.setCornerOne(world.getBlockAt(northwest.getBlockX(), northwest.getBlockY(), northwest.getBlockZ()));
		
		// add sign
		int x = northwest.getBlockX();
		int y = northwest.getBlockY();
		int z = northwest.getBlockZ();
		
		Block block = world.getBlockAt(x, y, z); 
		block.setType(Material.SignPost);
		block.setData((byte)10); // towards southeast
		
		Sign sign = (Sign)block;
		sign.setLine(0, "Northwest");
		sign.setLine(1, "corner of");
		sign.setLine(2, "warzone");
		sign.setLine(3, name);
	
		saveState();
	}
	
	public void removeNorthwest() {
		int x = northwest.getBlockX();
		int y = northwest.getBlockY();
		int z = northwest.getBlockZ();
		world.getBlockAt(x, y, z).setTypeID(0);
	}

	public Location getNorthwest() {
		return northwest;
	}

	public void setSoutheast(Location southeast) {
		// remove old se sign, if any (replace with air)
		if(this.southeast != null) {
			removeSoutheast();
		}
		this.southeast = southeast;
		this.volume.setCornerOne(world.getBlockAt(southeast.getBlockX(), southeast.getBlockY(), southeast.getBlockZ()));
		// add sign
		int x = southeast.getBlockX();
		int y = southeast.getBlockY();
		int z = southeast.getBlockZ();
		Block block = world.getBlockAt(x, y, z);	// towards northwest
		block.setType(Material.SignPost);
		block.setData((byte)2);;
	
		Sign sign = (Sign)block;
		sign.setLine(0, "Southeast");
		sign.setLine(1, "corner of");
		sign.setLine(2, "warzone");
		sign.setLine(3, name);
		
		saveState();
	}
	
	public void removeSoutheast() {
		int x = southeast.getBlockX();
		int y = southeast.getBlockY();
		int z = southeast.getBlockZ();
		world.getBlockAt(x, y, z).setTypeID(0);
	}

	public Location getSoutheast() {
		return southeast;
	}

	public void setTeleport(Location location) {
		this.teleport = location;
	}

	public Location getTeleport() {
		return this.teleport;
	}
	
	public int saveState() {
		if(ready()){
			return volume.saveBlocks();
		}
		return 0;
	}
	
	/**
	 * Goes back to the saved state of the warzone (resets only block types, not physics).
	 * Also teleports all players back to their respective spawns.
	 * @return
	 */
	public int resetState() {
		if(ready() && volume.isSaved()){
			int reset = volume.resetBlocks();
			
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player player : team.getPlayers()) {
					respawnPlayer(team, player);
				}
				team.setRemainingTickets(lifePool);
				team.resetSign();
			}
			
			// reset monuments
			for(Monument monument : monuments) {
				monument.remove();
				monument.addMonumentBlocks();
			}
			
			this.setNorthwest(this.getNorthwest());
			this.setSoutheast(this.getSoutheast());
			
			return reset;
		}
		return 0;
	}

	public void endRound() {
		
	}

	public void respawnPlayer(Team team, Player player) {
//		BUKKIT
//		Inventory playerInv = player.getInventory();
//		playerInv.getContents().clear();
//		for(Integer slot : loadout.keySet()) {
//			playerInv.getContents().add(loadout.get(slot));
//			// TODO set the proper slot index
//		}
		
		player.setHealth(20);
		
//		BUKKIT
//		player.setFireTicks(0);
		player.teleportTo(team.getTeamSpawn());
	}

	public boolean isMonumentCenterBlock(Block block) {
		for(Monument monument : monuments) {
			int x = monument.getLocation().getBlockX();
			int y = monument.getLocation().getBlockY();
			int z = monument.getLocation().getBlockZ();
			if(x == block.getX() && y == block.getY() && z == block.getZ()) {
				return true;
			}
		}
		return false;
	}
	
	public Monument getMonumentFromCenterBlock(Block block) {
		for(Monument monument : monuments) {
			int x = monument.getLocation().getBlockX();
			int y = monument.getLocation().getBlockY();
			int z = monument.getLocation().getBlockZ();
			if(x == block.getX() && y == block.getY() && z == block.getZ()) {
				return monument;
			}
		}
		return null;
	}

	public boolean nearAnyOwnedMonument(Location to, Team team) {
		for(Monument monument : monuments) {
			if(monument.isNear(to) && monument.isOwner(team)) {
				return true;
			}
		}
		return false;
	}
	
//	public void removeSpawnArea(Team team) {
//		// Reset spawn to what it was before the gold blocks
//		team.getVolume().resetBlocks();
//	}

//	public void addSpawnArea(Team team, Location location) {
//		// Save the spawn state
//		team.setTeamSpawn(location);
//	}
		

	public List<Monument> getMonuments() {
		return monuments;
	}

	public boolean getFriendlyFire() {
		// TODO Auto-generated method stub
		return this.friendlyFire;
	}

	public void setLoadout(HashMap<Integer, ItemStack> loadout) {
		this.loadout = loadout;
	}

	public HashMap<Integer, ItemStack> getLoadout() {
		return loadout;
	}

	public void setLifePool(int lifePool) {
		this.lifePool = lifePool;
	}

	public int getLifePool() {
		return lifePool;
	}

	public void setFriendlyFire(boolean ffOn) {
		this.friendlyFire = ffOn;
	}

	public boolean hasPlayerInventory(String playerName) {
		return inventories.containsKey(playerName);
	}

	public void keepPlayerInventory(Player player) {
		// BUKKIT
//		Inventory inventory = player.getInventory();
//		
//		inventories.put(player.getName(), inventory.getContents());
	}

	public void restorePlayerInventory(Player player) {
//		HashMap<Integer,ItemStack> originalContents = inventories.remove((player.getName());
//		Inventory playerInv = player.getInventory(); 
//		playerInv.clearContents();
//		playerInv.update();
//		for(Item item : originalContents) {
//			playerInv.addItem(item);
//		}
//		playerInv.update();
//		player.getInventory().update();
	}

	public boolean hasMonument(String monumentName) {
		boolean hasIt = false;
		for(Monument monument: monuments) {
			if(monument.getName().equals(monumentName)) {
				return true;
			}
		}
		return false;
	}
	
	public Monument getMonument(String monumentName) {
		boolean hasIt = false;
		for(Monument monument: monuments) {
			if(monument.getName().equals(monumentName)) {
				return monument;
			}
		}
		return null;
	}
	
	public boolean isImportantBlock(Block block) {
		block.getX();
		for(Monument m : monuments) {
			if(m.getVolume().contains(block)){
				return true;
			}
		}
		for(Team t : teams) {
			if(t.getVolume().contains(block)){
				return true;
			}
		}
		if(teleportNear(block)) {
			return true;
		}
		return false;
	}

	private boolean teleportNear(Block block) {
		int x = (int)this.teleport.getBlockX();
		int y = (int)this.teleport.getBlockY();
		int z = (int)this.teleport.getBlockZ();
		int bx = block.getX();
		int by = block.getY();
		int bz = block.getZ();
		if((bx == x && by == y && bz == z) || 
				(bx == x+1 && by == y-1 && bz == z+1) ||
				(bx == x+1 && by == y-1 && bz == z) ||
				(bx == x+1 && by == y-1 && bz == z-1) ||
				(bx == x && by == y-1 && bz == z+1) ||
				(bx == x && by == y-1 && bz == z) ||
				(bx == x && by == y-1 && bz == z-1) ||
				(bx == x-1 && by == y-1 && bz == z+1) ||
				(bx == x-1 && by == y-1 && bz == z) ||
				(bx == x-1 && by == y-1 && bz == z-1) ) {
			return true;
		}
		return false;
	}

	public World getWorld() {
		
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public Volume getVolume() {
		return volume;
	}

	public void setVolume(VerticalVolume zoneVolume) {
		this.volume = zoneVolume;
	}

	

}