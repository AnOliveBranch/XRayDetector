package me.roboticplayer.xraydetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class XRayDetector extends JavaPlugin implements Listener {

	private FileConfiguration config;
	private List<Material> watched;
	private List<Integer> taskIDs;
	private Map<Material, Map<UUID, Integer>> trackerMap;

	@Override
	public void onEnable() {
		config = this.getConfig();
		trackerMap = new HashMap<Material, Map<UUID, Integer>>();
		saveDefaultConfig();
		populateMaterials();
		makeTimers();
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("XRayDetector has been enabled");
	}

	@Override
	public void onDisable() {
		for (int id : taskIDs)
			getServer().getScheduler().cancelTask(id);
		getLogger().info("XRayDetector has been disabled");
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.isCancelled())
			return;
		Player p = e.getPlayer();
		if (p.hasPermission("xraydetector.bypass"))
			return;

		Material mat = e.getBlock().getType();
		int max = config.getInt("blocks." + mat.toString() + ".amount");
		int remind = config.getInt("blocks." + mat.toString() + ".reminder");

		if (watched.contains(mat)) {
			if (trackerMap.get(mat).containsKey(p.getUniqueId()))
				trackerMap.get(mat).put(p.getUniqueId(), trackerMap.get(mat).get(p.getUniqueId()) + 1);
			else
				trackerMap.get(mat).put(p.getUniqueId(), 1);

			if (trackerMap.get(mat).get(p.getUniqueId()) >= max) {
				if (trackerMap.get(mat).get(p.getUniqueId()) == max
						|| (trackerMap.get(mat).get(p.getUniqueId()) % remind == 0)) {
					for (Player staff : getServer().getOnlinePlayers())
						if (staff.hasPermission("xraydetector.bypass"))
							staff.sendMessage(notifyMessage(p, mat, e.getBlock().getLocation()));
					getLogger().info(notifyMessage(p, mat, e.getBlock().getLocation()));
				}
			}
		}
	}

	private void populateMaterials() {
		watched = new ArrayList<Material>();
		for (String mat : config.getConfigurationSection("blocks").getValues(false).keySet()) {
			if (Material.getMaterial(mat) != null) {
				watched.add(Material.getMaterial(mat));
				trackerMap.put(Material.getMaterial(mat), new HashMap<UUID, Integer>());
			} else
				getLogger().info("Incorrect material: " + mat);
		}
	}

	private void makeTimers() {
		taskIDs = new ArrayList<Integer>();
		for (Material mat : watched) {
			int time = config.getInt("blocks." + mat.toString() + ".time");
			taskIDs.add(getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				public void run() {
					trackerMap.get(mat).clear();
				}
			}, time * 20, time * 20));
		}
	}

	private String notifyMessage(Player p, Material mat, Location loc) {
		String message = ChatColor.translateAlternateColorCodes('&', config.getString("notifyMessage"));
		message = message.replace("%PLAYER%", p.getName());
		message = message.replace("%NUMBER%", String.valueOf(trackerMap.get(mat).get(p.getUniqueId())));
		message = message.replace("%BLOCK%", mat.toString());
		message = message.replace("%TIME%", String.valueOf(config.getInt("blocks." + mat.toString() + ".time")));
		message = message.replace("%LOCATION%", formatLocation(loc));
		return message;
	}

	private String formatLocation(Location loc) {
		String message = config.getString("locationFormat");
		message = message.replace("%WORLD%", loc.getWorld().getName());
		message = message.replace("%XCoord%", String.valueOf(loc.getBlockX()));
		message = message.replace("%YCoord%", String.valueOf(loc.getBlockY()));
		message = message.replace("%ZCoord%", String.valueOf(loc.getBlockZ()));

		return message;
	}
}
