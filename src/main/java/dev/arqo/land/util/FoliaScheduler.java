package dev.arqo.land.util;

import dev.arqo.land.ArqoLand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class FoliaScheduler {
    
    public static void runAtLocation(Location loc, Runnable runnable) {
        if (ArqoLand.getInstance().isFolia()) {
            Bukkit.getRegionScheduler().execute(ArqoLand.getInstance(), loc, runnable);
        } else {
            Bukkit.getScheduler().runTask(ArqoLand.getInstance(), runnable);
        }
    }

    public static void runAtEntity(Entity entity, Runnable runnable) {
        if (ArqoLand.getInstance().isFolia()) {
            entity.getScheduler().execute(ArqoLand.getInstance(), runnable, null, 1L);
        } else {
            Bukkit.getScheduler().runTask(ArqoLand.getInstance(), runnable);
        }
    }

    public static void runAsync(Runnable runnable) {
        if (ArqoLand.getInstance().isFolia()) {
            Bukkit.getAsyncScheduler().runNow(ArqoLand.getInstance(), task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(ArqoLand.getInstance(), runnable);
        }
    }
}
