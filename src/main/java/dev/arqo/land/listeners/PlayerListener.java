package dev.arqo.land.listeners;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.PerkManager;
import dev.arqo.land.models.ClaimData;
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
        ClaimData toClaim = plugin.getChunkManager().getClaimAt(toChunk);
        ClaimData fromClaim = plugin.getChunkManager().getClaimAt(fromChunk);

        if (toClaim != null && fromClaim != toClaim) {
            if (titleEnabled) {
                String msg = toClaim.getGreetingMessage() != null ? toClaim.getGreetingMessage() : defaultGreeting;
                String greeting = msg.replace("{land_name}", toClaim.getName()).replace("&", "§");
                player.sendTitle("§e§l" + toClaim.getName(), greeting, 10, 70, 20);
            }
            
            // Berikan perk ke Owner dan Member
            boolean isMember = toClaim.getOwner().equals(player.getUniqueId()) || 
                               toClaim.getMembers().stream().anyMatch(m -> m.getUuid().equals(player.getUniqueId()));
            
            if (isMember) {
                plugin.getPerkManager().applyPerks(player, toClaim);
            }
        } else if (toClaim == null && fromClaim != null) {
            if (titleEnabled) {
                player.sendTitle("", "§7Memasuki Wilayah Liar", 10, 40, 10);
            }
        }
    }
}