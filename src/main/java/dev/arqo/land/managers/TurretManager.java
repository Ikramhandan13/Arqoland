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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
                if (plugin.isFolia()) {
                    Bukkit.getRegionScheduler().run(plugin, loc, task -> processTurret(loc, claim));
                } else {
                    processTurret(loc, claim);
                }
            }
        }
    }

    private void processTurret(Location loc, ClaimData claim) {
        Block block = loc.getBlock();
        if (block.getType() != Material.DISPENSER) return;

        // Range dinamis berdasarkan level
        int level = claim.getTurretLevel();
        double range = switch (level) {
            case 0, 1 -> 16;
            case 2 -> 24;
            case 3 -> 32;
            case 4 -> 48;
            case 5 -> 64;
            default -> 16;
        };

        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, range, range, range);
        Player target = null;
        Player enemyTarget = null;

        for (Entity entity : nearby) {
            if (entity instanceof Player p) {
                if (isFriendly(p, claim)) continue;
                if (!p.hasLineOfSight(block.getLocation())) continue;
                
                // Prioritas musuh (Enemy Land)
                ClaimData pLand = plugin.getChunkManager().getClaimAt(p.getLocation().getChunk());
                if (pLand != null && claim.getEnemyLands().contains(pLand.getId())) {
                    enemyTarget = p;
                    break; 
                }
                
                if (target == null) target = p;
            }
        }

        Player finalTarget = (enemyTarget != null) ? enemyTarget : target;
        if (finalTarget != null) {
            shoot(block, finalTarget, claim);
        }
    }

    private boolean isFriendly(Player player, ClaimData claim) {
        if (claim.getOwner().equals(player.getUniqueId())) return true;
        if (claim.getMembers().stream().anyMatch(m -> m.getUuid().equals(player.getUniqueId()))) return true;
        
        // Cek apakah player adalah bagian dari Aliansi
        for (int allyId : claim.getAlliedLands()) {
            ClaimData allyLand = plugin.getChunkManager().getLandNameCache().values().stream()
                    .filter(c -> c.getId() == allyId).findFirst().orElse(null);
            if (allyLand != null) {
                if (allyLand.getOwner().equals(player.getUniqueId()) || 
                    allyLand.getMembers().stream().anyMatch(m -> m.getUuid().equals(player.getUniqueId()))) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private void shoot(Block dispenserBlock, Player target, ClaimData claim) {
        BlockState state = dispenserBlock.getState();
        if (!(state instanceof Container container)) return;

        Inventory inv = container.getInventory();
        int level = claim.getTurretLevel();

        // Check Ammo
        if (!claim.isTurretAmmoFree()) {
            if (!inv.contains(Material.ARROW)) return;
            inv.removeItem(new ItemStack(Material.ARROW, 1));
        }

        Location spawnLoc = dispenserBlock.getLocation().add(0.5, 0.5, 0.5);
        Vector direction = target.getLocation().add(0, 1.5, 0).toVector().subtract(spawnLoc.toVector()).normalize();

        Arrow arrow = spawnLoc.getWorld().spawn(spawnLoc, Arrow.class);
        arrow.setVelocity(direction.multiply(2));
        arrow.setShooter(null);

        // DAMAGE & EFFECTS BASED ON LEVEL
        double baseDamage = 4.0;
        
        if (level >= 3) {
            arrow.setFireTicks(200); // Panah Api
        }
        
        if (level == 4) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10)); // Freeze 2 detik
        }
        
        if (level == 5) {
            baseDamage = 12.0; // Damage sakit
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10)); // Freeze 5 detik
            arrow.setFireTicks(400);
        } else {
            baseDamage += (level * 1.5);
        }

        arrow.setDamage(baseDamage);
    }

    public void registerTurret(ClaimData claim, Location loc) {
        claim.getTurretLocations().add(loc);
        plugin.getDatabaseManager().insertTurret(claim.getId(), loc);
    }

    public void unregisterTurret(ClaimData claim, Location loc) {
        claim.getTurretLocations().remove(loc);
        plugin.getDatabaseManager().deleteTurret(loc);
    }
}
