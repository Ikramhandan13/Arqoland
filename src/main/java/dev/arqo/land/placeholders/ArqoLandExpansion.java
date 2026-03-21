package dev.arqo.land.placeholders;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.core.ChunkManager;
import dev.arqo.land.model.ClaimData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArqoLandExpansion extends PlaceholderExpansion {

    private final ArqoLand plugin;
    private final ChunkManager chunkManager;

    public ArqoLandExpansion(ArqoLand plugin, ChunkManager chunkManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "arqoland";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Dzakiri";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0-ENTERPRISE";
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        // %arqoland_player_land%
        if (params.equalsIgnoreCase("player_land")) {
            Chunk chunk = player.getLocation().getChunk();
            ClaimData claim = chunkManager.getClaimAt(chunk);
            return claim != null ? claim.getName() : "Liar";
        }

        // %arqoland_land_hp%
        if (params.equalsIgnoreCase("land_hp")) {
            Chunk chunk = player.getLocation().getChunk();
            ClaimData claim = chunkManager.getClaimAt(chunk);
            return claim != null ? String.valueOf(claim.getHealth()) : "0";
        }

        // Top Balance Name & Amount Placeholder: %arqoland_top_name_1% / %arqoland_top_balance_1%
        if (params.startsWith("top_name_") || params.startsWith("top_balance_")) {
            try {
                String[] parts = params.split("_");
                int rank = Integer.parseInt(parts[2]) - 1; 

                // Note: Untuk server produksi masif, gunakan list cache yang diupdate setiap 5 menit
                // agar tidak melakukan sorting di setiap request Placeholder.
                List<ClaimData> allLands = new ArrayList<>(); 
                // Asumsi ChunkManager memiliki method getAllClaims(), untuk contoh ini dikosongkan.
                // allLands.addAll(chunkManager.getAllClaims());
                
                allLands.sort(Comparator.comparingInt(ClaimData::getDiamondBalance).reversed());

                if (rank >= 0 && rank < allLands.size()) {
                    ClaimData topLand = allLands.get(rank);
                    if (params.startsWith("top_name_")) {
                        return topLand.getName();
                    } else {
                        return String.valueOf(topLand.getDiamondBalance());
                    }
                } else {
                    return params.startsWith("top_name_") ? "Tidak Ada" : "0";
                }
            } catch (Exception e) {
                return "Error";
            }
        }

        return null;
    }
}
