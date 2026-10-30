/*
 * This file is part of spark.
 *
 * spark is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 3 or later.
 */
package me.lucko.spark.bukkit;

import me.lucko.spark.common.tick.AbstractTickReporter;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;

/** Reports the previous tick duration recorded by the Paper 1.12.2 server. */
final class LegacyTickReporter extends AbstractTickReporter implements Runnable {
    private final Plugin plugin;
    private final Object server;
    private final long[] durations;
    private final Method tickCount;
    private BukkitTask task;

    static boolean isSupported(Server server) {
        return server.getClass().getName().equals("org.bukkit.craftbukkit.v1_12_R1.CraftServer");
    }

    LegacyTickReporter(Plugin plugin) {
        this.plugin = plugin;
        try {
            this.server = plugin.getServer().getClass().getMethod("getServer").invoke(plugin.getServer());
            this.durations = (long[]) this.server.getClass().getField("h").get(this.server);
            this.tickCount = this.server.getClass().getMethod("aq");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access Paper 1.12.2 tick durations", e);
        }
    }

    @Override
    public void run() {
        try {
            int currentTick = (int) this.tickCount.invoke(this.server);
            long nanos = this.durations[Math.floorMod(currentTick - 1, this.durations.length)];
            if (nanos > 0) onTick(nanos / 1_000_000.0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read Paper 1.12.2 tick duration", e);
        }
    }

    @Override
    public void start() {
        this.task = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, this, 1L, 1L);
    }

    @Override
    public void close() {
        this.task.cancel();
    }
}
