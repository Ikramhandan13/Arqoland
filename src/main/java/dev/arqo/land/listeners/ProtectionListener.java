package dev.arqo.land.listeners;

import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.FlagManager;
import dev.arqo.land.models.ClaimData;
import dev.arqo.land.models.LandMember;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class ProtectionListener implements Listener {
    private final ChunkManager chunkManager;
    private final FlagManager flagManager;

    public ProtectionListener(ChunkManager chunkManager, FlagManager flagManager) {
        this.chunkManager = chunkManager;
        this.flagManager = flagManager;
    }

    private boolean canBuild(Player player, ClaimData claim) {
        if (claim == null) return true;
        UUID uuid = player.getUniqueId();
        
        // 1. Bypass Admin
        if (player.hasPermission("arqoland.admin") || player.isOp()) return true;
        
        // 2. Owner Check
        if (claim.getOwner().equals(uuid)) return true;
        
        // 3. Member Check
        for (LandMember member : claim.getMembers()) {
            if (member.getUuid().equals(uuid)) return true;
        }
        
        // 4. Ally Check (Optional: Bisa dibatasi jika perlu)
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ClaimData claim = chunkManager.getClaimAt(event.getBlock().getChunk());
        
        if (!canBuild(player, claim)) {
            event.setCancelled(true);
            player.sendMessage("§cWilayah ini dilindungi oleh " + claim.getName() + ".");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ClaimData claim = chunkManager.getClaimAt(event.getBlock().getChunk());
        
        if (!canBuild(player, claim)) {
            event.setCancelled(true);
            player.sendMessage("§cWilayah ini dilindungi oleh " + claim.getName() + ".");
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
}
