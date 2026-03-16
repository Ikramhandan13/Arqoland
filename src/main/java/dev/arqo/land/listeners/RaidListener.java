package dev.arqo.land.listeners;

import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.DiscordWebhookManager;
import dev.arqo.land.models.ClaimData;
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
        
        // Ambil satu lokasi ledakan untuk visual feedback
        Location blastLoc = blocks.get(0).getLocation().add(0.5, 1, 0.5);
        
        java.util.Map<ClaimData, Integer> damagedLands = new java.util.HashMap<>();

        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            ClaimData claim = chunkManager.getClaimAt(block.getChunk());

            if (claim != null) {
                iterator.remove(); // Blok tidak hancur, HP yang berkurang
                damagedLands.put(claim, damagedLands.getOrDefault(claim, 0) + 1);
            }
        }

        damagedLands.forEach((claim, count) -> {
            int totalDamage = count * 2; // Misal 1 blok yang terlindungi = 2 HP damage
            boolean destroyed = claim.takeDamage(totalDamage);
            
            // 1. Kirim Title Alert ke Owner & Member yang online
            String alertMsg = "§c§lPERINGATAN: §fWilayah §e" + claim.getName() + " §fsedang DI-RAID!";
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> claim.getOwner().equals(p.getUniqueId()) || claim.getMembers().stream().anyMatch(m -> m.getUuid().equals(p.getUniqueId())))
                .forEach(p -> {
                    p.sendTitle("§c§lDI SERANG!", "§fHP: §e" + claim.getHealth() + "§7/§e" + claim.getMaxHealth(), 5, 20, 5);
                    p.sendMessage("§c[Raid] §fLedakan §e" + sourceName + " §fmenyebabkan §c-" + totalDamage + " HP§f!");
                });

            // 2. Munculkan Hologram HP di titik ledakan
            spawnHologram(blastLoc, "§c§l-" + totalDamage + " HP §7(" + claim.getHealth() + " Left)");

            if (webhookManager != null) {
                webhookManager.sendRaidAlert(claim.getName(), sourceName, claim.getHealth(), claim.getMaxHealth());
            }

            if (destroyed) {
                // Hapus semua chunk wilayah jika HP habis
                for (String key : claim.getChunkKeys()) {
                    String[] p = key.split(":");
                    org.bukkit.World w = Bukkit.getWorld(p[0]);
                    if (w != null) chunkManager.removeClaim(w.getChunkAt(Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                }
                Bukkit.broadcastMessage("§c§l[ArqoLand] §fWilayah §e" + claim.getName() + " §ftelah §4§lHANCUR §foleh serangan §e" + sourceName + "§f!");
            }
        });
    }

    private void spawnHologram(Location loc, String text) {
        org.bukkit.entity.ArmorStand as = loc.getWorld().spawn(loc, org.bukkit.entity.ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setCustomName(text);
            armorStand.setCustomNameVisible(true);
            armorStand.setMarker(true);
        });
        
        // Hapus hologram setelah 3 detik
        Bukkit.getRegionScheduler().runDelayed(ArqoLand.getInstance(), loc, task -> as.remove(), 60L);
    }
}