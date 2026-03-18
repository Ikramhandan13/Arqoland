package dev.arqo.land.utils;

import dev.arqo.land.models.ClaimData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.plugin.Plugin;

public class StatusHologramUtil {

    public static void spawnStatusHologram(Plugin plugin, Location loc, ClaimData claim) {
        // Hologram Baris 1: Nama Land (Besar)
        ArmorStand nameHolo = loc.getWorld().spawn(loc.clone().add(0, 2.5, 0), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setCustomName("§e§l" + claim.getName().toUpperCase());
            as.setCustomNameVisible(true);
            as.setMarker(true);
        });

        // Hologram Baris 2: Darah (HP)
        ArmorStand hpHolo = loc.getWorld().spawn(loc.clone().add(0, 2.15, 0), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            double percent = (double) claim.getHealth() / claim.getMaxHealth();
            String color = percent > 0.5 ? "§a" : (percent > 0.2 ? "§e" : "§c");
            
            // HP Bar visual
            int bars = 10;
            int filled = (int) (percent * bars);
            StringBuilder barStr = new StringBuilder(color + "[");
            for (int i = 0; i < bars; i++) {
                if (i < filled) barStr.append("■");
                else barStr.append("§7□");
            }
            barStr.append(color + "] §f" + claim.getHealth() + " HP");
            
            as.setCustomName(barStr.toString());
            as.setCustomNameVisible(true);
            as.setMarker(true);
        });

        // Hapus setelah duration di config (default 5s)
        int duration = plugin.getConfig().getInt("visuals.hologram-duration-seconds", 5);
        Bukkit.getRegionScheduler().runDelayed(plugin, loc, task -> {
            nameHolo.remove();
            hpHolo.remove();
        }, duration * 20L);
    }
}
