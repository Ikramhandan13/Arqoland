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

        // Simpan chunk ke DB secara async
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> plugin.getDatabaseManager().insertChunk(claimData.getId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().insertChunk(claimData.getId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
        }
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

            // Hapus chunk dari DB secara async
            if (plugin.isFolia()) {
                Bukkit.getAsyncScheduler().runNow(plugin, task -> plugin.getDatabaseManager().deleteChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().deleteChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
            }
            saveLandAsync(data);
        }
    }

    public void startTaxTask() {
        int costPerWeek = plugin.getConfig().getInt("taxes.diamond-cost-per-week", 70);
        
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> runTaxCycle(costPerWeek), 1, 7, java.util.concurrent.TimeUnit.DAYS);
            // Inactivity Check every midnight (approx 24h)
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> plugin.getDatabaseManager().deleteInactiveLands(30), 1, 1, java.util.concurrent.TimeUnit.DAYS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> runTaxCycle(costPerWeek), 20 * 60 * 60 * 24 * 7L, 20 * 60 * 60 * 24 * 7L);
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> plugin.getDatabaseManager().deleteInactiveLands(30), 20 * 60 * 60 * 24L, 20 * 60 * 60 * 24L);
        }
    }

    private void runTaxCycle(int cost) {
        plugin.getLogger().info("Memulai siklus penarikan setoran (Tax Cycle)...");
        long gracePeriodMillis = 7L * 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();

        for (ClaimData claim : landNameCache.values()) {
            // Cek Grace Period (7 Hari pertama bebas pajak)
            if (claim.getCreatedAt() != null && (now - claim.getCreatedAt().getTime()) < gracePeriodMillis) {
                plugin.getLogger().info("Wilayah " + claim.getName() + " masih dalam masa Grace Period (Baru dibuat).");
                continue;
            }

            if (claim.getDiamondBalance() >= cost) {
                claim.setDiamondBalance(claim.getDiamondBalance() - cost);
                plugin.getLogger().info("Wilayah " + claim.getName() + " telah membayar setoran mingguan.");
            } else {
                int newHealth = claim.getHealth() / 2;
                claim.setHealth(Math.max(1, newHealth));
                plugin.getLogger().warning("Wilayah " + claim.getName() + " GAGAL membayar setoran! HP dikurangi 50%.");
            }
            saveLandAsync(claim);
        }
    }

    public void deleteLand(ClaimData claim) {
        // Hapus dari cache nama
        landNameCache.remove(claim.getName().toLowerCase());
        
        // Hapus setiap chunk dari cache
        for (String key : claim.getChunkKeys()) {
            chunkCache.remove(key);
        }
        
        // Hapus dari database (async)
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> plugin.getDatabaseManager().deleteLand(claim.getId()));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().deleteLand(claim.getId()));
        }
    }

    public void saveLandAsync(ClaimData claimData) {
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> plugin.getDatabaseManager().saveLand(claimData));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().saveLand(claimData));
        }
    }

    public void showBorderOnce(org.bukkit.entity.Player player, Chunk chunk) {
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        double y = player.getLocation().getY();

        // Tampilkan partikel border (Flame/Blue) sesuai tipe wilayah
        org.bukkit.Particle particle = org.bukkit.Particle.SOUL_FIRE_FLAME;
        
        for (double x = minX; x <= maxX; x += 0.5) {
            player.spawnParticle(particle, x, y + 1, minZ, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, x, y + 1, maxZ, 1, 0, 0, 0, 0);
        }
        for (double z = minZ; z <= maxZ; z += 0.5) {
            player.spawnParticle(particle, minX, y + 1, z, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, maxX, y + 1, z, 1, 0, 0, 0, 0);
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
                    player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, x, y + 1, minZ, 1);
                    player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, x, y + 1, maxZ, 1);
                }
                for (double z = minZ; z <= maxZ; z++) {
                    player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, minX, y + 1, z, 1);
                    player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, maxX, y + 1, z, 1);
                }
            }, i * 40L);
        }
    }

    public Map<String, ClaimData> getLandNameCache() {
        return landNameCache;
    }
}
