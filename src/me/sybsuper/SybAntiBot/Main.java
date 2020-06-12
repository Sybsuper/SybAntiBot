/*
 * Copyright (c) 2020 Sybsuper
 * All Rights Reserved
 *
 * Do not use this code without permission of the developer.
 */

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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {
	public FileConfiguration config;
	public List<String> isBeingChecked;
	public List<String> isBeingChecked2;
	public File logFile;
	public HashMap<UUID, Integer> violations = new HashMap<>();
	public BukkitRunnable clearViolations;

	@Override
	public void onEnable() {
		load();
		Bukkit.getPluginManager().registerEvents(new BreakOre(this), this);
	}

	private String applyStuff(String s, Player p, Float pitch, Float yaw) {
		return ChatColor.translateAlternateColorCodes('&', s.replaceAll("%player%", p.getName()).replaceAll("%yaw%", Float.toString(yaw)).replaceAll("%pitch%", Float.toString(pitch)));
	}

	public void logToFile(String message) {
		if (config.getBoolean("log.enable")) {
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
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender.hasPermission("sybantibot.isbot")) {
			if (args.length >= 1) {
				if (args[0].equalsIgnoreCase("reload")) {
					if (sender.hasPermission("sybantibot.reload")) {
						reloadConfig();
						load();
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.reloadSuccess")));
					} else {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.reloadNoPermission")));
					}
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
								if ((pitch <= config.getDouble("maxDifference") && pitch != 0 && locOld.getPitch() != locChanged.getPitch()) || (yaw <= config.getDouble("maxDifference") && yaw != 0)) {
									violations.putIfAbsent(p.getUniqueId(), 0);
									violations.put(p.getUniqueId(), violations.get(p.getUniqueId()) + 1);
									if (violations.get(p.getUniqueId()) >= config.getInt("violations")) {
										violations.put(p.getUniqueId(), 0);
										List<String> commands = config.getStringList("commands");
										for (String cmd : commands) {
											Bukkit.getServer().dispatchCommand(getServer().getConsoleSender(), applyStuff(cmd, p, pitch, yaw));
										}
										sender.sendMessage(applyStuff(config.getString("return.bot"), p, pitch, yaw));
										logToFile(applyStuff(config.getString("log.bot"), p, pitch, yaw));
									}
									if (config.getBoolean("notify.enable")) {
										for (Player player : Bukkit.getServer().getOnlinePlayers()) {
											if (player.hasPermission("sybantibot.notify")) {
												player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("notify.message")).replaceAll("%player%", p.getName()).replaceAll("%violations%", String.valueOf(violations.get(p.getUniqueId()))));
											}
										}
									}
								} else {
									if (config.getBoolean("setback")) {
										p.teleport(locOld);
									}
									sender.sendMessage(applyStuff(config.getString("return.noBot"), p, pitch, yaw));
									logToFile(applyStuff(config.getString("log.noBot"), p, pitch, yaw));
								}
								isBeingChecked2.remove(p.getUniqueId().toString());
							}
						}.runTaskLater(this, config.getInt("ticksBeforeCheck"));
						return true;
					} else {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.playerAlreadyBeingChecked")));
						return true;
					}
				} else {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.playerDoesNotExist")));
					return true;
				}
			} else {
				return false;
			}
		} else {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.noPermission")));
			return true;
		}
	}

	private void load() {
		isBeingChecked = new ArrayList<>();
		isBeingChecked2 = new ArrayList<>();
		config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		boolean isCorrect = config.isInt("delayBeforeRunBotCheck") && config.isDouble("default.yaw") && config.isDouble("default.pitch") && config.isDouble("maxDifference") && config.isInt("ticksBeforeCheck") && config.isList("commands");
		if (!isCorrect) {
			Logger l = Bukkit.getLogger();
			l.log(Level.SEVERE, "There's something wrong with the 'plugins/SybAntiBot/config.yml' file, please check it.");
		}
		if (clearViolations != null) {
			clearViolations.cancel();
		}
		clearViolations = new BukkitRunnable() {
			@Override
			public void run() {
				violations.clear();
			}
		};
		clearViolations.runTaskTimer(this, 20L, config.getInt("clearViolations") * 20);
		if (config.getBoolean("log.enable")) {
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
		}
	}
}
