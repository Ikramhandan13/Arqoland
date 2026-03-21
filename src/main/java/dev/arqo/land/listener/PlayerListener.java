package dev.arqo.land.listener;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.core.ChunkManager;
import dev.arqo.land.core.PerkManager;
import dev.arqo.land.model.ClaimData;
import dev.arqo.land.util.StatusHologramUtil;
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

        // Jika berpindah ke wilayah yang berbeda
        if (toClaim != null && fromClaim != toClaim) {
            String ownerName = Bukkit.getOfflinePlayer(toClaim.getOwner()).getName();
            String landDisplayName = toClaim.getDisplayName();
            
            // Logika Deteksi Musuh/Member
            boolean isMember = toClaim.getOwner().equals(player.getUniqueId()) || 
                               toClaim.getMembers().stream().anyMatch(m -> m.getUuid().equals(player.getUniqueId()));
            
            ClaimData playerLand = chunkManager.getClaimAt(player.getLocation().getChunk());
            boolean isEnemy = playerLand != null && toClaim.getEnemyLands().contains(playerLand.getId());
            
            // 1. Action Bar Notification
            if (isEnemy) {
                player.sendActionBar(net.kyori.adventure.text.Component.text("§c§lPERINGATAN: §eMemasuki Wilayah MUSUH! (" + landDisplayName + ")"));
                player.sendMessage("§c[Peringatan] §fKamu memasuki wilayah musuh! Hati-hati terhadap Turret.");
            } else {
                player.sendActionBar(net.kyori.adventure.text.Component.text("§aMemasuki: " + landDisplayName + " §7| §aPemilik: §f" + ownerName));
            }

            // 2. Title Notification
            if (titleEnabled) {
                String subtitle = (toClaim.getGreetingMessage() != null ? toClaim.getGreetingMessage() : defaultGreeting)
                        .replace("{land_name}", landDisplayName).replace("&", "§");
                player.sendTitle(landDisplayName, subtitle, 10, 60, 20);
            }

            // 3. Chat Notification
            player.sendMessage("§8[§a!§8] §fSelamat datang di §e" + landDisplayName + " §f(Pemilik: §a" + ownerName + "§f)");

            // 4. Sound Notification
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            // 5. Apply Perks for Members
            if (isMember) {
                perkManager.applyPerks(player, toClaim);
            }

            // 6. Hologram & Border
            StatusHologramUtil.spawnStatusHologram(plugin, player.getLocation().add(player.getLocation().getDirection().multiply(4)), toClaim);
            chunkManager.showBorderOnce(player, toChunk);

        } else if (toClaim == null && fromClaim != null) {
            // Keluar dari wilayah ke wilayah liar
            player.sendActionBar(net.kyori.adventure.text.Component.text("§7Keluar Wilayah - Memasuki Wilayah Liar"));
            if (titleEnabled) {
                player.sendTitle("", "§7Memasuki Wilayah Liar", 10, 40, 10);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
        }
    }
}
