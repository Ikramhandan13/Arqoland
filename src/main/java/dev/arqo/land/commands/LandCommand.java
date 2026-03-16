package dev.arqo.land.commands;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.EconomyManager;
import dev.arqo.land.models.ClaimData;
import dev.arqo.land.models.LandMember;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Command({"arqoland", "land", "al"})
public class LandCommand {
    private final ArqoLand plugin;
    private final ChunkManager chunkManager;
    private final EconomyManager economyManager;
    private final Map<UUID, Location[]> posCache = new ConcurrentHashMap<>();

    public LandCommand(ArqoLand plugin, ChunkManager chunkManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.economyManager = economyManager;
    }

    private String getRole(Player player, ClaimData claim) {
        if (claim.getOwner().equals(player.getUniqueId())) return "OWNER";
        for (LandMember m : claim.getMembers()) {
            if (m.getUuid().equals(player.getUniqueId())) return m.getRole();
        }
        return "NONE";
    }

    private boolean isAtLeast(Player player, ClaimData claim, String requiredRole) {
        if (player.isOp() || player.hasPermission("arqoland.admin")) return true;
        String role = getRole(player, claim);
        if (requiredRole.equals("OWNER")) return role.equals("OWNER");
        if (requiredRole.equals("ADMIN")) return role.equals("OWNER") || role.equals("ADMIN");
        if (requiredRole.equals("MEMBER")) return !role.equals("NONE");
        return false;
    }

    private int getUpgradeCost(int currentLevel) {
        if (currentLevel == 1) return 64;
        if (currentLevel == 2) return 128;
        if (currentLevel == 3) return 256;
        if (currentLevel == 4) return 512;
        return 0;
    }

    @DefaultFor({"arqoland", "land", "al"})
    public void help(Player player) {
        player.sendMessage("§e=== ArqoLand Enterprise Perintah ===");
        player.sendMessage("§f/al pos <1/2> §7- Tandai koordinat");
        player.sendMessage("§f/al claimland <nama> §7- Klaim wilayah");
        player.sendMessage("§f/al status <land> §7- Lihat info land");
        player.sendMessage("§f/al spawn §7- Teleport ke land");
        player.sendMessage("§f/al setoran §7- Donasi Diamond");
        player.sendMessage("§f/al upgrade <tipe> §7- Upgrade Wilayah via Brankas");
    }

    @Subcommand("claimland")
    public void claim(Player player, String name) {
        Chunk chunk = player.getLocation().getChunk();
        if (chunkManager.getClaimAt(chunk) != null) {
            player.sendMessage("§cChunk ini sudah diklaim.");
            return;
        }
        ClaimData claim = new ClaimData(0, name, player.getUniqueId());
        claim.setHealth(500);
        claim.setMaxHealth(500);
        chunkManager.addClaim(chunk, claim);
        player.sendMessage("§aWilayah §e" + name + " §aberhasil dibuat!");
    }

    @Subcommand("upgrade")
    public void upgrade(Player player, String type) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "OWNER")) return;

        int currentLevel = 0;
        type = type.toLowerCase();
        switch (type) {
            case "haste": currentLevel = claim.getPerkHaste(); break;
            case "speed": currentLevel = claim.getPerkSpeed(); break;
            case "strength": currentLevel = claim.getPerkStrength(); break;
            case "jump": currentLevel = claim.getPerkJump(); break;
            case "crop": currentLevel = claim.getPerkCrop(); break;
            case "heart": currentLevel = (claim.getMaxHealth() / 500) - 1; break;
            default: return;
        }

        if (currentLevel >= 5) return;

        int cost = getUpgradeCost(currentLevel + 1);
        if (claim.getDiamondBalance() < cost) {
            player.sendMessage("§cSaldo brankas tidak cukup! Butuh §b" + cost + " Diamond §fdi brankas.");
            return;
        }

        claim.setDiamondBalance(claim.getDiamondBalance() - cost);
        switch (type) {
            case "haste": claim.setPerkHaste(currentLevel + 1); break;
            case "speed": claim.setPerkSpeed(currentLevel + 1); break;
            case "strength": claim.setPerkStrength(currentLevel + 1); break;
            case "jump": claim.setPerkJump(currentLevel + 1); break;
            case "crop": claim.setPerkCrop(currentLevel + 1); break;
            case "heart": claim.setMaxHealth(claim.getMaxHealth() + 500); claim.setHealth(claim.getMaxHealth()); break;
        }
        
        chunkManager.saveLandAsync(claim);
        player.sendMessage("§a[Upgrade] §fUpgrade §e" + type.toUpperCase() + " §fberhasil ditarik dari brankas.");
    }

    @Subcommand("setoran")
    public void setoran(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "MEMBER")) return;
        
        int count = economyManager.countDiamonds(player);
        if (count > 0 && economyManager.takeDiamonds(player, count)) {
            claim.setDiamondBalance(claim.getDiamondBalance() + count);
            for (LandMember m : claim.getMembers()) {
                if (m.getUuid().equals(player.getUniqueId())) {
                    m.addContribution(count);
                    plugin.getDatabaseManager().saveMemberContribution(claim.getId(), m);
                }
            }
            chunkManager.saveLandAsync(claim);
            player.sendMessage("§a[Setoran] §fBerhasil menyumbang §b" + count + " Diamond.");
        }
    }

    @Subcommand("status")
    public void status(Player player, @Optional String name) {
        ClaimData claim = (name == null) ? chunkManager.getClaimAt(player.getLocation().getChunk()) : chunkManager.getClaimByName(name);
        if (claim == null) return;
        player.sendMessage("§e=== Status: " + claim.getName() + " ===");
        player.sendMessage("§7Pemilik: §f" + Bukkit.getOfflinePlayer(claim.getOwner()).getName());
        player.sendMessage("§7HP: §c" + claim.getHealth() + "/" + claim.getMaxHealth());
        player.sendMessage("§7Brankas: §b" + claim.getDiamondBalance() + " Diamond");
    }

    @Subcommand("map")
    public void showMap(Player player) {
        dev.arqo.land.utils.MapVisualizer.sendMap(player, chunkManager);
    }

    @Subcommand("border")
    public void showBorder(Player player) {
        chunkManager.showBorder(player);
        player.sendMessage("§aMenampilkan batas wilayah...");
    }
}
