package dev.arqo.land.listeners;

import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.FlagManager;
import dev.arqo.land.managers.TurretManager;
import dev.arqo.land.models.ClaimData;
import dev.arqo.land.models.LandMember;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class ProtectionListener implements Listener {
    private final ChunkManager chunkManager;
    private final FlagManager flagManager;
    private final TurretManager turretManager;

    public ProtectionListener(ChunkManager chunkManager, FlagManager flagManager, TurretManager turretManager) {
        this.chunkManager = chunkManager;
        this.flagManager = flagManager;
        this.turretManager = turretManager;
    }

    private boolean canBuild(Player player, ClaimData claim) {
        if (claim == null) return true;
        UUID uuid = player.getUniqueId();
        
        if (player.hasPermission("arqoland.admin") || player.isOp()) return true;
        if (claim.getOwner().equals(uuid)) return true;
        
        for (LandMember member : claim.getMembers()) {
            if (member.getUuid().equals(uuid)) return true;
        }
        
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ClaimData claim = chunkManager.getClaimAt(event.getBlock().getChunk());
        
        if (!canBuild(player, claim)) {
            event.setCancelled(true);
            player.sendMessage("§cWilayah ini dilindungi oleh " + (claim != null ? claim.getName() : "sistem") + ".");
            return;
        }

        // Jika blok yang dihancurkan adalah turret
        if (claim != null && event.getBlock().getType() == Material.DISPENSER) {
            if (claim.getTurretLocations().contains(event.getBlock().getLocation())) {
                turretManager.unregisterTurret(claim, event.getBlock().getLocation());
                player.sendMessage("§e[Turret] §fPertahanan wilayah telah dilepas.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ClaimData claim = chunkManager.getClaimAt(event.getBlock().getChunk());
        
        if (!canBuild(player, claim)) {
            event.setCancelled(true);
            player.sendMessage("§cWilayah ini dilindungi oleh " + (claim != null ? claim.getName() : "sistem") + ".");
            return;
        }

        // Jika menaruh DISPENSER di wilayah sendiri, jadikan Turret
        if (claim != null && event.getBlock().getType() == Material.DISPENSER) {
            int max = dev.arqo.land.ArqoLand.getInstance().getConfig().getInt("turrets.max-turrets-per-land", 5);
            if (claim.getTurretLocations().size() >= max) {
                player.sendMessage("§cBatas maksimal turret wilayah (§e" + max + "§c) telah tercapai!");
                return;
            }
            turretManager.registerTurret(claim, event.getBlock().getLocation());
            player.sendMessage("§e[Turret] §fPertahanan otomatis wilayah diaktifkan di posisi ini.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        ClaimData claim = chunkManager.getClaimAt(event.getClickedBlock().getChunk());

        if (claim != null && !canBuild(player, claim)) {
            if (!flagManager.canInteract(claim)) {
                event.setCancelled(true);
                player.sendMessage("§cInteraksi dikunci oleh wilayah " + claim.getName() + ".");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) return;

        // Admin Bypass
        if (attacker.hasPermission("arqoland.admin") || attacker.isOp()) return;

        ClaimData claim = chunkManager.getClaimAt(victim.getLocation().getChunk());
        if (claim == null) return;

        if (!claim.isPvpEnabled()) {
            event.setCancelled(true);
            attacker.sendMessage("§cWilayah ini menonaktifkan PvP.");
        }
    }
}
