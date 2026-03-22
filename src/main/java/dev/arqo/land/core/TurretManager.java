package dev.arqo.land.core;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.model.ClaimData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Animals;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TurretManager {
    private final ArqoLand plugin;
    private final Map<Location, Long> notificationCooldown = new HashMap<>();

    public TurretManager(ArqoLand plugin) {
        this.plugin = plugin;
        startTurretTicker();
    }

    private void startTurretTicker() {
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> tickTurrets(), 1, 1, java.util.concurrent.TimeUnit.SECONDS);
        } else {
             Bukkit.getScheduler().runTaskTimer(plugin, this::tickTurrets, 20L, 20L);
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

        int level = claim.getTurretLevel();
        double range = 16 + (level * 4); 

        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, range, range, range);
        LivingEntity finalTarget = null;

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.isDead()) continue;

            if (living instanceof Player p) {
                if (plugin.getChunkManager().isFriendly(p, claim)) continue;
                if (!p.hasLineOfSight(block.getLocation().add(0.5, 0.5, 0.5))) continue;
                finalTarget = p;
                break;
            }

            if (living instanceof Monster) {
                if (finalTarget == null) finalTarget = living;
            }
            
            if (living instanceof Villager || living instanceof Animals || living instanceof Tameable) {
                continue;
            }
        }

        if (finalTarget != null) {
            // DETEKSI HALANGAN BLOK
            Location dispenserCenter = loc.clone().add(0.5, 0.5, 0.5);
            Vector direction = finalTarget.getEyeLocation().toVector().subtract(dispenserCenter.toVector()).normalize();
            Block nextBlock = dispenserCenter.clone().add(direction.clone().multiply(1.0)).getBlock();

            if (nextBlock.getType().isSolid() && !nextBlock.isPassable()) {
                notifyBlocked(claim, loc);
                return;
            }

            shoot(block, finalTarget, claim);
        }
    }

    private void notifyBlocked(ClaimData claim, Location loc) {
        long now = System.currentTimeMillis();
        if (notificationCooldown.getOrDefault(loc, 0L) > now) return;

        notificationCooldown.put(loc, now + 10000); // Cooldown 10 detik

        Player owner = Bukkit.getPlayer(claim.getOwner());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage("§c§l[PERINGATAN] §fTurret di §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " §cterhalang blok! Segera cari tempat terbuka.");
            owner.sendActionBar(Component.text("§c§lTURRET TERHALANG! Cari tempat lain!"));
            owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }

    private void shoot(Block dispenserBlock, LivingEntity target, ClaimData claim) {
        BlockState state = dispenserBlock.getState();
        if (!(state instanceof Container container)) return;

        Inventory inv = container.getInventory();
        int level = claim.getTurretLevel();

        if (!claim.isTurretAmmoFree()) {
            if (!inv.contains(Material.ARROW)) return;
            inv.removeItem(new ItemStack(Material.ARROW, 1));
        }

        Location dispenserCenter = dispenserBlock.getLocation().add(0.5, 0.5, 0.5);
        Vector direction = target.getEyeLocation().toVector().subtract(dispenserCenter.toVector()).normalize();
        Location spawnLoc = dispenserCenter.clone().add(direction.clone().multiply(1.2));

        Arrow arrow = spawnLoc.getWorld().spawn(spawnLoc, Arrow.class);
        arrow.setVelocity(direction.multiply(2.5)); 
        arrow.setShooter(null);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);

        double baseDamage = 6.0 + (level * 2.0); 
        arrow.setDamage(baseDamage);
        
        arrow.setFireTicks(200); 
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (arrow.isDead() || arrow.isOnGround()) {
                task.cancel();
                return;
            }
            arrow.getWorld().spawnParticle(Particle.FLAME, arrow.getLocation(), 2, 0.02, 0.02, 0.02, 0.01);
            arrow.getWorld().spawnParticle(Particle.SMOKE, arrow.getLocation(), 1, 0, 0, 0, 0);
        }, 1L, 1L);

        applyStatusEffects(target, level);
    }

    private void applyStatusEffects(LivingEntity target, int level) {
        if (level <= 0) return;

        switch (level) {
            case 1:
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                break;
            case 2:
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
                break;
            case 3:
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 1));
                break;
            case 4:
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                break;
            case 5:
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 4));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                break;
        }
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
