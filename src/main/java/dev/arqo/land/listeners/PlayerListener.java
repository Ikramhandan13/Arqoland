package dev.arqo.land.listeners;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.PerkManager;
import dev.arqo.land.models.ClaimData;
import dev.arqo.land.utils.StatusHologramUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerListener implements Listener {
    private final ArqoLand plugin;
    private final ChunkManager chunkManager;
    private final PerkManager perkManager;
    private final boolean titleEnabled;
    private final String defaultGreeting;

    public PlayerListener(ArqoLand plugin, ChunkManager chunkManager, PerkManager perkManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.perkManager = perkManager;
        this.titleEnabled = plugin.getConfig().getBoolean("visuals.welcome-title-enabled");
        this.defaultGreeting = plugin.getConfig().getString("visuals.default-greeting").replace("&", "§");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) return;

        Player player = event.getPlayer();
        ClaimData toClaim = chunkManager.getClaimAt(toChunk);
        ClaimData fromClaim = chunkManager.getClaimAt(fromChunk);

        if (toClaim != null && fromClaim != toClaim) {
            // Action Bar: Nama Land & Owner
            String ownerName = Bukkit.getOfflinePlayer(toClaim.getOwner()).getName();
            
            // Cek status Musuh
            ClaimData playerLand = chunkManager.getClaimAt(player.getLocation().getChunk());
            boolean isEnemy = playerLand != null && toClaim.getEnemyLands().contains(playerLand.getId());
            
            if (isEnemy) {
                player.sendActionBar(net.kyori.adventure.text.Component.text("§c§lPERINGATAN: §eMemasuki Wilayah MUSUH! (" + toClaim.getDisplayName() + ")"));
                player.sendMessage("§c[Peringatan] Kamu memasuki wilayah musuh! Turret akan memprioritaskan kamu.");
            } else {
                player.sendActionBar(net.kyori.adventure.text.Component.text("§aMemasuki: §e" + toClaim.getDisplayName() + " §7| §aPemilik: §f" + ownerName));
            }

            if (titleEnabled) {
                String msg = toClaim.getGreetingMessage() != null ? toClaim.getGreetingMessage() : defaultGreeting;
                String greeting = msg.replace("{land_name}", toClaim.getName()).replace("&", "§");
                player.sendTitle("§e§l" + toClaim.getName(), greeting, 10, 70, 20);
            }
            
            // Spawn Hologram di depan player
            StatusHologramUtil.spawnStatusHologram(plugin, player.getLocation().add(player.getLocation().getDirection().multiply(4)), toClaim);
            
            boolean isMember = toClaim.getOwner().equals(player.getUniqueId()) || 
                               toClaim.getMembers().stream().anyMatch(m -> m.getUuid().equals(player.getUniqueId()));
            
            if (isMember) {
                perkManager.applyPerks(player, toClaim);
            }

            // Tampilkan border saat masuk
            chunkManager.showBorderOnce(player, toChunk);

        } else if (toClaim == null && fromClaim != null) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§7Keluar Wilayah - Memasuki Wilayah Liar"));
            if (titleEnabled) {
                player.sendTitle("", "§7Memasuki Wilayah Liar", 10, 40, 10);
            }
        }
    }
}
