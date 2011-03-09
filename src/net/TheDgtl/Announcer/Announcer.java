package net.TheDgtl.Announcer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;

public class Announcer extends JavaPlugin
{
	// Config Values
	private long delay = 120L; // Delay in seconds
	ArrayList<String> announcements = new ArrayList<String>();

	public static Logger log;
	private PluginManager pm;
	private Permissions permissions = null;
	private double permVersion = 0;

	public void onEnable()
	{
		log = getServer().getLogger();
		pm = getServer().getPluginManager();
		if (loadAnnounce()) {
			log.info("[Announcer] Enabled Announcer v" + getDescription().getVersion());
		} else {
			pm.disablePlugin(this);
			return;
		}
        if (setupPermissions()) {
        	if (permissions != null)
        		log.info("[Announcer] Using Permissions " + permVersion + " (" + Permissions.version + ") for permissions");
        } else {
        	log.info("[Announcer] No permissions plugin found, using default permission settings");
        }
	}

	public void onDisable()
	{
		announcements.clear();
		log.info("[Announcer] Disabled Announcer");
	}
	
	private boolean loadAnnounce() {
		this.announcements.clear();
		getServer().getScheduler().cancelTasks(this);
		try {
			File fh = new File(this.getDataFolder(), "Announce.txt");
			if (!fh.exists()) {
				log.info("[Announcer] Could not find Announce.txt file");
				return false;
			}
			boolean firstLine = true;
			Scanner scanner;
			scanner = new Scanner(fh);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (firstLine) {
					this.delay = Long.parseLong(line) * 20L;
					firstLine = false;
					continue;
				}
				this.announcements.add(parseColor(line));
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			log.info("[Announcer] Could not load Announce.txt");
			return false;
		}
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new AnnounceThread(announcements, getServer()), delay, delay);
		return true;
	}
	
	private String parseColor(String line) {
		return line.replaceAll("(&([a-f0-9]))", "\u00A7$2");
	}
	
	/*
	 * Find what Permissions plugin we're using and enable it.
	 */
	private boolean setupPermissions() {
		Plugin perm;
		// Apparently GM isn't a new permissions plugin, it's Permissions "2.0.1"
		// API change broke my plugin.
		perm = pm.getPlugin("Permissions");
		// We're running Permissions
		if (perm != null) {
			if (!perm.isEnabled()) {
				pm.enablePlugin(perm);
			}
			permissions = (Permissions)perm;
			try {
				String[] permParts = Permissions.version.split("\\.");
				permVersion = Double.parseDouble(permParts[0] + "." + permParts[1]);
			} catch (Exception e) {
				log.info("Could not determine Permissions version: " + Permissions.version);
				return true;
			}
			return true;
		}
		// Permissions not loaded
		return false;
	}
    
	/*
	 * Check whether the player has the given permissions.
	 */
	public boolean hasPerm(Player player, String perm, boolean def) {
		if (permissions != null) {
			return permissions.getHandler().has(player, perm);
		} else {
			return def;
		}
	}
	
    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
    	String comName = command.getName().toLowerCase();
    	Player player = null;
    	if (sender instanceof Player) player = (Player)sender;
    	
    	if (comName.equals("announcer")) {
    		if (player != null && !hasPerm(player, "announcer.reload", player.isOp())) {
    			player.sendMessage("[Announcer] Permission Denied");
    			return true;
    		}
    		if (args.length != 1) {
    			sender.sendMessage("[Announcer] Usage: /announcer reload");
    			return true;
    		}
    		if (args[0].equalsIgnoreCase("reload")) {
    			loadAnnounce();
    			sender.sendMessage("[Announcer] Reloaded announcement file.");
    			return true;
    		}
    		return false;
    	}
    	
    	return false;
    }

	class AnnounceThread implements Runnable {
		ArrayList<String> announcements;
		Server server;
		AnnounceThread(ArrayList<String> announcements, Server server) {
			this.announcements = announcements;
			this.server = server;
		}
		
		public void run() {
			Random randomise = new Random();
			int index = randomise.nextInt(announcements.size());
			server.broadcastMessage(announcements.get(index));
			log.info("[Announcer] " + announcements.get(index));
		}
	}
}