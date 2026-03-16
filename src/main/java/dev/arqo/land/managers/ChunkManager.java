package dev.arqo.land.managers;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.models.ClaimData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {
    private final ArqoLand plugin;
    private final Map<String, ClaimData> chunkCache;
    private final Map<String, ClaimData> landNameCache;

    public ChunkManager(ArqoLand plugin) {
        this.plugin = plugin;
        this.chunkCache = new ConcurrentHashMap<>();
        this.landNameCache = new ConcurrentHashMap<>();
    }

    public String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    public String getChunkKey(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }

    public ClaimData getClaimAt(Chunk chunk) {
        return chunkCache.get(getChunkKey(chunk));
    }

    public ClaimData getClaimByName(String name) {
        return landNameCache.get(name.toLowerCase());
    }

    public void loadClaimToCache(ClaimData claimData) {
        landNameCache.put(claimData.getName().toLowerCase(), claimData);
        for (String key : claimData.getChunkKeys()) {
            chunkCache.put(key, claimData);
        }
    }

    public void addClaim(Chunk chunk, ClaimData claimData) {
        String key = getChunkKey(chunk);
        chunkCache.put(key, claimData);
        landNameCache.put(claimData.getName().toLowerCase(), claimData);
        claimData.getChunkKeys().add(key);
        saveLandAsync(claimData);
    }

    public void removeClaim(Chunk chunk) {
        String key = getChunkKey(chunk);
        ClaimData data = chunkCache.remove(key);
        if (data != null) {
            data.getChunkKeys().remove(key);
            if (data.getChunkKeys().isEmpty()) {
                landNameCache.remove(data.getName().toLowerCase());
            }
            saveLandAsync(data);
        }
    }

    public void startTaxTask() {
        int costPerWeek = plugin.getConfig().getInt("taxes.diamond-cost-per-week", 70);
        
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> runTaxCycle(costPerWeek), 1, 7, java.util.concurrent.TimeUnit.DAYS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> runTaxCycle(costPerWeek), 20 * 60 * 60 * 24 * 7L, 20 * 60 * 60 * 24 * 7L);
        }
    }

    private void runTaxCycle(int cost) {
        plugin.getLogger().info("Memulai siklus penarikan setoran (Tax Cycle)...");
        for (ClaimData claim : landNameCache.values()) {
            if (claim.getDiamondBalance() >= cost) {
                claim.setDiamondBalance(claim.getDiamondBalance() - cost);
                plugin.getLogger().info("Wilayah " + claim.getName() + " telah membayar setoran mingguan.");
            } else {
                claim.setHealth(claim.getHealth() / 2);
                plugin.getLogger().warning("Wilayah " + claim.getName() + " GAGAL membayar setoran! HP dikurangi 50%.");
            }
            saveLandAsync(claim);
        }
    }

    public void saveLandAsync(ClaimData claimData) {
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> plugin.getDatabaseManager().saveLand(claimData));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().saveLand(claimData));
        }
    }

    public void showBorder(org.bukkit.entity.Player player) {
        Chunk chunk = player.getLocation().getChunk();
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        double y = player.getLocation().getY();

        for (int i = 0; i < 15; i++) {
            Bukkit.getRegionScheduler().runDelayed(plugin, player.getLocation(), task -> {
                for (double x = minX; x <= maxX; x++) {
                    player.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, x, y + 1, minZ, 1);
                    player.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, x, y + 1, maxZ, 1);
                }
                for (double z = minZ; z <= maxZ; z++) {
                    player.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, minX, y + 1, z, 1);
                    player.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, maxX, y + 1, z, 1);
                }
            }, i * 40L);
        }
    }

    public Map<String, ClaimData> getLandNameCache() {
        return landNameCache;
    }
}
