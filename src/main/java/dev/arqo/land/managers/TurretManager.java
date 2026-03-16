package dev.arqo.land.managers;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.models.ClaimData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;

public class TurretManager {
    private final ArqoLand plugin;

    public TurretManager(ArqoLand plugin) {
        this.plugin = plugin;
        startTurretTicker();
    }

    private void startTurretTicker() {
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> tickTurrets(), 1, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, this::tickTurrets, 10L, 10L);
        }
    }

    private void tickTurrets() {
        for (ClaimData claim : plugin.getChunkManager().getLandNameCache().values()) {
            if (claim.getTurretLocations().isEmpty()) continue;

            for (Location loc : claim.getTurretLocations()) {
                Block block = loc.getBlock();
                if (block.getType() != Material.DISPENSER) continue;

                Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, 16, 16, 16);
                Player target = null;

                for (Entity entity : nearby) {
                    if (entity instanceof Player p) {
                        if (isFriendly(p, claim)) continue;
                        target = p;
                        break;
                    }
                }

                if (target != null) {
                    shoot(block, target);
                }
            }
        }
    }

    private boolean isFriendly(Player player, ClaimData claim) {
        if (claim.getOwner().equals(player.getUniqueId())) return true;
        return claim.getMembers().stream().anyMatch(m -> m.getUuid().equals(player.getUniqueId()));
    }

    private void shoot(Block dispenserBlock, Player target) {
        BlockState state = dispenserBlock.getState();
        if (!(state instanceof Container container)) return;

        Inventory inv = container.getInventory();
        if (!inv.contains(Material.ARROW)) return;

        inv.removeItem(new ItemStack(Material.ARROW, 1));

        Location spawnLoc = dispenserBlock.getLocation().add(0.5, 0.5, 0.5);
        Vector direction = target.getLocation().add(0, 1, 0).toVector().subtract(spawnLoc.toVector()).normalize();

        Bukkit.getRegionScheduler().run(plugin, spawnLoc, task -> {
            Arrow arrow = spawnLoc.getWorld().spawn(spawnLoc, Arrow.class);
            arrow.setVelocity(direction.multiply(2));
            arrow.setShooter(null);
        });
    }
}
