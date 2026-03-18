package dev.arqo.land.listeners;

import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.DiscordWebhookManager;
import dev.arqo.land.models.ClaimData;
import dev.arqo.land.utils.StatusHologramUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import dev.arqo.land.ArqoLand;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.Iterator;

public class RaidListener implements Listener {
    private final ChunkManager chunkManager;
    private final DiscordWebhookManager webhookManager;

    public RaidListener(ChunkManager chunkManager, DiscordWebhookManager webhookManager) {
        this.chunkManager = chunkManager;
        this.webhookManager = webhookManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        processExplosion(event.blockList(), event.getEntity() != null ? event.getEntity().getName() : "TNT");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        processExplosion(event.blockList(), "Bed/Anchor");
    }

    private void processExplosion(java.util.List<Block> blocks, String sourceName) {
        if (blocks.isEmpty()) return;
        
        Location blastLoc = blocks.get(0).getLocation().add(0.5, 0, 0.5);
        java.util.Map<ClaimData, Integer> damagedLands = new java.util.HashMap<>();

        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            ClaimData claim = chunkManager.getClaimAt(block.getChunk());

            if (claim != null) {
                iterator.remove(); 
                damagedLands.put(claim, damagedLands.getOrDefault(claim, 0) + 1);
            }
        }

        damagedLands.forEach((claim, count) -> {
            int totalDamage = count * 2; 
            boolean destroyed = claim.takeDamage(totalDamage);
            
            // Alert Title & Message
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> claim.getOwner().equals(p.getUniqueId()) || claim.getMembers().stream().anyMatch(m -> m.getUuid().equals(p.getUniqueId())))
                .forEach(p -> {
                    p.sendTitle("§c§lDI SERANG!", "§fHP: §e" + claim.getHealth() + "§7/§e" + claim.getMaxHealth(), 5, 20, 5);
                    p.sendMessage("§c[Raid] §fLedakan §e" + sourceName + " §fmenyebabkan §c-" + totalDamage + " HP§f!");
                });

            // Baru: Spawn Status Hologram Besar di lokasi ledakan
            StatusHologramUtil.spawnStatusHologram(ArqoLand.getInstance(), blastLoc, claim);

            if (webhookManager != null) {
                webhookManager.sendRaidAlert(claim.getName(), sourceName, claim.getHealth(), claim.getMaxHealth());
            }

            if (destroyed) {
                chunkManager.deleteLand(claim);
                Bukkit.broadcastMessage("§c§l[ArqoLand] §fWilayah §e" + claim.getName() + " §ftelah §4§lHANCUR §foleh serangan §e" + sourceName + "§f!");
            }
        });
    }
}
