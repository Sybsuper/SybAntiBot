package me.sybsuper.SybAntiBot;

import me.sybsuper.SybAntiBot.listeners.BreakOre;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {
	public FileConfiguration config;
	public List<String> isBeingChecked;
	public List<String> isBeingChecked2;
	public File logFile;
	@Override
	public void onEnable() {
		isBeingChecked = new ArrayList<>();
		isBeingChecked2 = new ArrayList<>();
		config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		boolean isCorrect = config.isInt("delayBeforeRunBotCheck") && config.isDouble("default.yaw") && config.isDouble("default.pitch") && config.isDouble("maxDifference") && config.isInt("ticksBeforeCheck") && config.isList("commands");
		if (!isCorrect) {
			Logger l = Bukkit.getLogger();
			l.log(Level.SEVERE, "There's something wrong in the 'plugins/SybAntiBot/config.yml' file, please check it.");
		}
		File dataFolder = getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdir();
		}
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		logFile = new File(getDataFolder(), "log " + timestamp.toString().split(" ")[0] + ".txt");
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Bukkit.getPluginManager().registerEvents(new BreakOre(this), this);
	}

	private String applyStuff(String s, Player p, Float pitch, Float yaw) {
		return ChatColor.translateAlternateColorCodes('&', s.replaceAll("%player%", p.getName()).replaceAll("%yaw%", Float.toString(yaw)).replaceAll("%pitch%", Float.toString(pitch)));
	}

	public void logToFile(String message) {
		try {
			FileWriter fw = new FileWriter(logFile, true);
			PrintWriter pw = new PrintWriter(fw);
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			pw.println("[" + timestamp + "] " + message);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender.hasPermission("sybantibot.rotate")) {
			if (args.length >= 1) {
				if (args[0].equalsIgnoreCase("reload")) {
					reloadConfig();
					config = getConfig();
					config.options().copyDefaults(true);
					saveConfig();
					sender.sendMessage(ChatColor.DARK_GREEN + "Successfully reloaded config files.");
					return true;
				}
				Player p = Bukkit.getPlayer(args[0]);
				if (p != null) {
					if (!isBeingChecked2.contains(p.getUniqueId().toString())) {
						isBeingChecked2.add(p.getUniqueId().toString());
						Location locOld = p.getLocation();
						Location locChanged = locOld.clone();
						if (args.length == 1) {
							double pitch = config.getDouble("default.pitch");
							double yaw = config.getDouble("default.yaw");
							locChanged.setPitch((float) (locChanged.getPitch() + pitch));
							locChanged.setYaw((float) (locChanged.getYaw() + yaw));
						} else {
							try {
								if (args.length > 2) {
									Float pitch = Float.parseFloat(args[2]);
									locChanged.setPitch(locChanged.getPitch() + pitch);
								}
								Float yaw = Float.parseFloat(args[1]);
								locChanged.setYaw(locChanged.getYaw() + yaw);
							} catch (NumberFormatException e) {
								return false;
							}
						}
						p.teleport(locChanged);
						new BukkitRunnable() {
							@Override
							public void run() {
								Location newLoc = p.getLocation();
								Float pitch = Math.abs(newLoc.getPitch() - locOld.getPitch());
								Float yaw = Math.abs(newLoc.getYaw() - locOld.getYaw()) % 360;
								if ((pitch <= config.getDouble("maxDifference") && locOld.getPitch() != locChanged.getPitch()) || yaw <= config.getDouble("maxDifference")) {
									List<String> commands = config.getStringList("commands");
									for (String cmd : commands) {
										Bukkit.getServer().dispatchCommand(getServer().getConsoleSender(), applyStuff(cmd, p, pitch, yaw));
									}
									sender.sendMessage(applyStuff(config.getString("returnTrue"), p, pitch, yaw));
									logToFile(applyStuff(config.getString("logTrue"), p, pitch, yaw));
								} else {
									p.teleport(locOld);
									sender.sendMessage(applyStuff(config.getString("returnFalse"), p, pitch, yaw));
									logToFile(applyStuff(config.getString("logFalse"), p, pitch, yaw));
								}
								isBeingChecked2.remove(p.getUniqueId().toString());
							}
						}.runTaskLater(this, config.getInt("ticksBeforeCheck"));
						return true;
					} else {
						sender.sendMessage(ChatColor.RED + "That player is already being checked.");
						return true;
					}
				} else {
					sender.sendMessage(ChatColor.RED + "That player does not exist.");
					return true;
				}
			} else {
				return false;
			}
		} else {
			sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command.");
			return true;
		}
	}
}
