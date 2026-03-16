package dev.arqo.land.utils;

import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.models.ClaimData;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class MapVisualizer {

    public static void sendMap(Player player, ChunkManager chunkManager) {
        Chunk center = player.getLocation().getChunk();
        player.sendMessage("§e=== Radar Wilayah (9x9 Chunks) ===");
        
        for (int z = -4; z <= 4; z++) {
            StringBuilder row = new StringBuilder("§7");
            for (int x = -4; x <= 4; x++) {
                Chunk current = center.getWorld().getChunkAt(center.getX() + x, center.getZ() + z);
                ClaimData claim = chunkManager.getClaimAt(current);
                
                if (x == 0 && z == 0) {
                    row.append("§b☺ "); // Lokasi pemain
                } else if (claim == null) {
                    row.append("§8- "); // Liar
                } else if (claim.getOwner().equals(player.getUniqueId())) {
                    row.append("§a█ "); // Milik sendiri
                } else {
                    row.append("§c█ "); // Milik orang lain
                }
            }
            player.sendMessage(row.toString());
        }
        player.sendMessage("§8- §7Liar, §a█ §7Milikmu, §c█ §7Milik Orang, §b☺ §7Lokasimu");
    }
}
