/*
 * Copyright (c) 2020 Sybsuper
 * All Rights Reserved
 *
 * Do not use this code without permission of the developer.
 */

package me.sybsuper.SybAntiBot.listeners;

import me.sybsuper.SybAntiBot.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BreakOre implements Listener {
	private final Main plugin;

	public BreakOre(Main plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (!(e.getPlayer().hasPermission("sybantibot.bypass") && plugin.config.getBoolean("enableBypassPermission"))) {
			if (plugin.config.getStringList("ores").contains(e.getBlock().getType().toString())) {
				if (Math.random() < plugin.config.getDouble("checkChanceOnOreMine")) {
					if (!plugin.isBeingChecked.contains(e.getPlayer().getUniqueId().toString())) {
						Logger logger = Bukkit.getLogger();
						logger.log(Level.INFO, "Checking " + e.getPlayer().getName() + " for being a bot...");
						plugin.isBeingChecked.add(e.getPlayer().getUniqueId().toString());
						new BukkitRunnable() {
							@Override
							public void run() {
								Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "isbot " + e.getPlayer().getName());
							}
						}.runTaskLater(plugin, plugin.config.getInt("delayBeforeRunBotCheck"));
						for (int i = 0; i < plugin.config.getInt("delayedChecks"); i++) {
							new BukkitRunnable() {
								@Override
								public void run() {
									Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "isbot " + e.getPlayer().getName());
								}
							}.runTaskLater(plugin, plugin.config.getInt("delayBeforeRunBotCheck") * (i + 1));
						}
						new BukkitRunnable() {
							@Override
							public void run() {
								plugin.isBeingChecked.remove(e.getPlayer().getUniqueId().toString());
							}
						}.runTaskLater(plugin, plugin.config.getInt("delayBeforeRunBotCheck") * (plugin.config.getInt("delayedChecks") + 1));
					}
				}
			}
		}
	}
}