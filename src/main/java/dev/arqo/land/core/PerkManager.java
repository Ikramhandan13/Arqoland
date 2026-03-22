package dev.arqo.land.core;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.model.ClaimData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.block.data.Ageable;

import java.util.Random;

public class PerkManager implements Listener {
    private final ArqoLand plugin;
    private final Random random = new Random();

    public PerkManager(ArqoLand plugin) {
        this.plugin = plugin;
        startPerkTicker();
    }

    private void startPerkTicker() {
        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> tickPerks(), 1, 5, java.util.concurrent.TimeUnit.SECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, this::tickPerks, 100L, 100L);
        }
    }

    private void tickPerks() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ClaimData claim = plugin.getChunkManager().getClaimAt(player.getLocation().getChunk());
            if (claim == null) continue;

            if (plugin.getChunkManager().isFriendly(player, claim)) {
                applyPerks(player, claim);
            } else {
                // Intruder Effects (Poison / Slowness if land is upgraded)
                if (claim.getTurretLevel() >= 2) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 1));
                }
                if (claim.getTurretLevel() >= 4) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 0));
                }
            }
        }
    }

    public void applyPerks(Player player, ClaimData claim) {
        if (claim.getPerkSpeed() > 0) 
            applyEffect(player, PotionEffectType.SPEED, claim.getPerkSpeed());
        if (claim.getPerkHaste() > 0) 
            applyEffect(player, PotionEffectType.HASTE, claim.getPerkHaste());
        if (claim.getPerkStrength() > 0) 
            applyEffect(player, PotionEffectType.STRENGTH, claim.getPerkStrength());
        if (claim.getPerkJump() > 0) 
            applyEffect(player, PotionEffectType.JUMP_BOOST, claim.getPerkJump());
    }

    private void applyEffect(Player player, PotionEffectType type, int level) {
        // Durasi 20 detik (400 tick) agar kontinu saat berada di wilayah
        player.addPotionEffect(new PotionEffect(type, 400, level - 1, true, false));
    }

    @EventHandler(ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        ClaimData claim = plugin.getChunkManager().getClaimAt(event.getBlock().getChunk());
        if (claim == null || claim.getPerkCrop() <= 0) return;

        // Peluang: Level * 25% (Lvl 4 = 100% Boost)
        int chance = claim.getPerkCrop() * 25;
        if (random.nextInt(100) < chance) {
            if (event.getNewState().getBlockData() instanceof Ageable ageable) {
                int maxAge = ageable.getMaximumAge();
                int currentAge = ageable.getAge();
                
                if (currentAge < maxAge) {
                    // Gandakan pertumbuhan: Tambah 1 stage lagi jika memungkinkan
                    ageable.setAge(Math.min(maxAge, currentAge + 1));
                    event.getBlock().setBlockData(ageable);
                }
            }
        }
    }
}
