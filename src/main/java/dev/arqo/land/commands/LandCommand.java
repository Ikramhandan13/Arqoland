package dev.arqo.land.commands;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.EconomyManager;
import dev.arqo.land.models.ClaimData;
import dev.arqo.land.models.LandMember;
import dev.arqo.land.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Command({"arqoland", "land", "al"})
public class LandCommand {
    private final ArqoLand plugin;
    private final ChunkManager chunkManager;
    private final EconomyManager economyManager;
    private final Map<UUID, Location[]> posCache = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> allyInvitations = new ConcurrentHashMap<>();

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
        if (claim == null) return false;
        String role = getRole(player, claim);
        if (requiredRole.equals("OWNER")) return role.equals("OWNER");
        if (requiredRole.equals("ADMIN")) return role.equals("OWNER") || role.equals("ADMIN");
        if (requiredRole.equals("MEMBER")) return !role.equals("NONE");
        return false;
    }

    @Default()
    public void help(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        player.sendMessage("§e=== ArqoLand Enterprise Perintah ===");
        player.sendMessage("§f/al claimland <nama> §7- Klaim wilayah");
        player.sendMessage("§f/al status [land] §7- Info wilayah & brankas");
        player.sendMessage("§f/al displayname <nama> [-c <warna>] §7- Ganti nama tampilan");
        player.sendMessage("§f/al addmember <player> §7- Tambah anggota");
        player.sendMessage("§f/al kickmember <player> §7- Tendang anggota");
        player.sendMessage("§f/al promote/demote <player> §7- Atur role");
        player.sendMessage("§f/al pvp <on/off> §7- Toggle PvP");
        player.sendMessage("§f/al ally <land> §7- Kirim permintaan aliansi");
        player.sendMessage("§f/al allyaccept <land> §7- Terima aliansi");
        player.sendMessage("§f/al allyout <land> §7- Keluar dari aliansi");
        player.sendMessage("§f/al spawn [land] §7- Teleport (Member Only)");
        player.sendMessage("§f/al topbrankas/topsetoran §7- Leaderboard");
    }

    @Subcommand("displayname")
    public void setDisplayName(Player player, String displayName, @Optional String colorFlag, @Optional String colorCode) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "OWNER")) return;

        String finalName = displayName;
        if (colorFlag != null && colorFlag.equalsIgnoreCase("-c") && colorCode != null) {
            finalName = "&" + colorCode + displayName;
        }
        
        claim.setDisplayName(ColorUtil.translate(finalName));
        chunkManager.saveLandAsync(claim);
        player.sendMessage("§a[Display] §fNama wilayah diatur menjadi: " + claim.getDisplayName());
    }

    @Subcommand("pvp")
    public void togglePvp(Player player, String toggle) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "ADMIN")) return;

        boolean enable = toggle.equalsIgnoreCase("on");
        claim.setPvpEnabled(enable);
        chunkManager.saveLandAsync(claim);
        player.sendMessage("§a[PvP] §fPvP di wilayah ini diatur ke: §e" + (enable ? "ON" : "OFF"));
    }

    @Subcommand("addmember")
    public void addMember(Player player, String targetName) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "ADMIN")) return;

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer tidak online!");
            return;
        }

        LandMember member = new LandMember(target.getUniqueId(), "MEMBER");
        claim.getMembers().add(member);
        plugin.getDatabaseManager().saveMember(claim.getId(), member);
        player.sendMessage("§a[Member] §fBerhasil menambah §e" + targetName + " §fsebagai member.");
    }

    @Subcommand("kickmember")
    public void kickMember(Player player, String targetName) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "ADMIN")) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        claim.getMembers().removeIf(m -> m.getUuid().equals(target.getUniqueId()));
        plugin.getDatabaseManager().removeMember(claim.getId(), target.getUniqueId());
        player.sendMessage("§a[Member] §fBerhasil mengeluarkan §e" + targetName + " §fdari wilayah.");
    }

    @Subcommand("promote")
    public void promote(Player player, String targetName) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "OWNER")) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        for (LandMember m : claim.getMembers()) {
            if (m.getUuid().equals(target.getUniqueId())) {
                m.setRole("ADMIN");
                plugin.getDatabaseManager().updateMemberRole(claim.getId(), m);
                player.sendMessage("§a[Role] §e" + targetName + " §fberhasil dipromosikan ke §bADMIN§f.");
                return;
            }
        }
    }

    @Subcommand("demote")
    public void demote(Player player, String targetName) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "OWNER")) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        for (LandMember m : claim.getMembers()) {
            if (m.getUuid().equals(target.getUniqueId())) {
                m.setRole("MEMBER");
                plugin.getDatabaseManager().updateMemberRole(claim.getId(), m);
                player.sendMessage("§a[Role] §e" + targetName + " §fberhasil diturunkan ke §7MEMBER§f.");
                return;
            }
        }
    }

    @Subcommand("ally")
    public void ally(Player player, String landName) {
        ClaimData myLand = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (myLand == null) {
            player.sendMessage("§cKamu tidak berada di wilayah manapun!");
            return;
        }
        if (!isAtLeast(player, myLand, "ADMIN")) return;

        ClaimData targetLand = chunkManager.getClaimByName(landName);
        if (targetLand == null) {
            player.sendMessage("§cWilayah target tidak ditemukan!");
            return;
        }

        allyInvitations.computeIfAbsent(targetLand.getId(), k -> ConcurrentHashMap.newKeySet()).add(myLand.getId());
        player.sendMessage("§a[Ally] §fPermintaan aliansi dikirim ke wilayah §e" + targetLand.getName());
        
        Player targetOwner = Bukkit.getPlayer(targetLand.getOwner());
        if (targetOwner != null) {
            targetOwner.sendMessage("§a[Ally] §fWilayah §e" + myLand.getName() + " §fingin beraliansi dengan kamu! §7(/al allyaccept " + myLand.getName() + ")");
        }
    }

    @Subcommand("allyaccept")
    public void allyAccept(Player player, String landName) {
        ClaimData myLand = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, myLand, "OWNER")) return;

        ClaimData targetLand = chunkManager.getClaimByName(landName);
        if (targetLand == null) return;

        if (allyInvitations.getOrDefault(myLand.getId(), Collections.emptySet()).contains(targetLand.getId())) {
            myLand.getAlliedLands().add(targetLand.getId());
            targetLand.getAlliedLands().add(myLand.getId());
            
            plugin.getDatabaseManager().saveAlly(myLand.getId(), targetLand.getId());
            allyInvitations.get(myLand.getId()).remove(targetLand.getId());
            
            player.sendMessage("§a[Ally] §fBerhasil beraliansi dengan wilayah §e" + targetLand.getName());
        } else {
            player.sendMessage("§cTidak ada permintaan aliansi dari wilayah tersebut!");
        }
    }

    @Subcommand("allyout")
    public void allyOut(Player player, String landName) {
        ClaimData myLand = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, myLand, "OWNER")) return;

        ClaimData targetLand = chunkManager.getClaimByName(landName);
        if (targetLand == null) return;

        myLand.getAlliedLands().remove(targetLand.getId());
        targetLand.getAlliedLands().remove(myLand.getId());
        plugin.getDatabaseManager().removeAlly(myLand.getId(), targetLand.getId());
        
        player.sendMessage("§a[Ally] §fKamu telah memutuskan aliansi dengan wilayah §e" + targetLand.getName());
    }

    @Subcommand("enemy")
    public void enemy(Player player, String landName) {
        ClaimData myLand = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, myLand, "ADMIN")) return;

        ClaimData targetLand = chunkManager.getClaimByName(landName);
        if (targetLand == null) {
            player.sendMessage("§cWilayah target tidak ditemukan!");
            return;
        }

        if (myLand.getId() == targetLand.getId()) return;

        myLand.getEnemyLands().add(targetLand.getId());
        // Simpan ke DB (akan buat method baru di DatabaseManager)
        plugin.getDatabaseManager().saveEnemy(myLand.getId(), targetLand.getId());
        
        player.sendMessage("§c[Enemy] §fWilayah §e" + targetLand.getName() + " §fkini dianggap sebagai MUSUH!");
    }

    @Subcommand("enemyremove")
    public void enemyRemove(Player player, String landName) {
        ClaimData myLand = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, myLand, "ADMIN")) return;

        ClaimData targetLand = chunkManager.getClaimByName(landName);
        if (targetLand == null) return;

        myLand.getEnemyLands().remove(targetLand.getId());
        plugin.getDatabaseManager().removeEnemy(myLand.getId(), targetLand.getId());
        
        player.sendMessage("§a[Enemy] §fWilayah §e" + targetLand.getName() + " §ftidak lagi dianggap sebagai musuh.");
    }

    @Subcommand("setspawn")
    public void setSpawn(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null) return;
        if (!isAtLeast(player, claim, "ADMIN")) return;
        claim.setSpawn(player.getLocation());
        chunkManager.saveLandAsync(claim);
        player.sendMessage("§a[Spawn] §fTitik spawn berhasil diatur.");
    }

    @Subcommand("spawn")
    public void spawn(Player player, @Optional String name) {
        ClaimData claim = (name == null) ? chunkManager.getClaimAt(player.getLocation().getChunk()) : chunkManager.getClaimByName(name);
        if (claim == null) return;
        if (!isAtLeast(player, claim, "MEMBER")) return;

        Location spawn = claim.getSpawn();
        if (spawn == null) return;
        player.teleport(spawn);
        player.sendMessage("§a[Spawn] §fBerteleportasi ke wilayah §e" + claim.getName() + "§f.");
    }

    @Subcommand("topbrankas")
    public void topBrankas(CommandSender sender) {
        sender.sendMessage("§e=== Top 10 Brankas Wilayah (Global) ===");
        List<ClaimData> lands = new ArrayList<>(chunkManager.getLandNameCache().values());
        lands.sort((a, b) -> Integer.compare(b.getDiamondBalance(), a.getDiamondBalance()));
        for (int i = 0; i < Math.min(10, lands.size()); i++) {
            ClaimData c = lands.get(i);
            sender.sendMessage("§f" + (i + 1) + ". §e" + c.getName() + " §7- §b" + c.getDiamondBalance() + " Diamond");
        }
    }

    @Subcommand("topsetoran")
    public void topSetoran(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "MEMBER")) return;
        player.sendMessage("§e=== Top 3 Penyumbang: " + claim.getName() + " ===");
        List<LandMember> members = new ArrayList<>(claim.getMembers());
        members.sort((a, b) -> Integer.compare(b.getTotalContributed(), a.getTotalContributed()));
        for (int i = 0; i < Math.min(3, members.size()); i++) {
            LandMember m = members.get(i);
            player.sendMessage("§f" + (i + 1) + ". §a" + Bukkit.getOfflinePlayer(m.getUuid()).getName() + " §7- §b" + m.getTotalContributed());
        }
    }

    @Subcommand("claimland")
    public void claim(Player player, String name) {
        Chunk chunk = player.getLocation().getChunk();
        
        // 1. Cek apakah chunk sudah diklaim
        if (chunkManager.getClaimAt(chunk) != null) {
            player.sendMessage("§cWilayah ini sudah diklaim oleh orang lain!");
            return;
        }

        // 2. Cek apakah nama wilayah sudah ada
        if (chunkManager.getClaimByName(name) != null) {
            player.sendMessage("§cNama wilayah tersebut sudah digunakan!");
            return;
        }

        // 3. Cek limit wilayah pemain (opsional, bisa ditambah sesuai config)
        long myLandsCount = chunkManager.getLandNameCache().values().stream()
                .filter(c -> c.getOwner().equals(player.getUniqueId())).count();
        int maxLands = plugin.getConfig().getInt("limits.default-max-lands", 1);
        if (myLandsCount >= maxLands && !player.hasPermission("arqoland.admin")) {
            player.sendMessage("§cKamu sudah mencapai batas maksimal wilayah (§e" + maxLands + "§c)!");
            return;
        }

        // 4. Simpan ke Database untuk dapatkan ID
        int id = plugin.getDatabaseManager().createLand(name, player.getUniqueId(), name);
        if (id == -1) {
            player.sendMessage("§cTerjadi kesalahan saat menyimpan wilayah ke database!");
            return;
        }

        // 5. Inisialisasi Model dan Cache
        ClaimData claim = new ClaimData(id, name, player.getUniqueId());
        claim.setHealth(500);
        claim.setMaxHealth(500);
        claim.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        
        chunkManager.addClaim(chunk, claim);
        player.sendMessage("§a[Klaim] §fWilayah §e" + name + " §fberhasil dibuat!");
    }

    @Subcommand("upgrade")
    public void upgrade(Player player, @Optional String type) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "OWNER")) return;
        if (type == null) { economyManager.openUpgradeGui(player, claim); return; }
    }

    @Subcommand("setoran")
    public void setoran(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "MEMBER")) return;
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

    @Subcommand("withdraw")
    public void withdraw(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "OWNER")) return;
        economyManager.openWithdrawGui(player, claim);
    }

    @Subcommand("status")
    public void status(Player player, @Optional String name) {
        ClaimData claim = (name == null) ? chunkManager.getClaimAt(player.getLocation().getChunk()) : chunkManager.getClaimByName(name);
        if (claim == null) return;
        player.sendMessage("§e=== Status: " + claim.getDisplayName() + " ===");
        player.sendMessage("§7Nama ID: §f" + claim.getName());
        player.sendMessage("§7Pemilik: §f" + Bukkit.getOfflinePlayer(claim.getOwner()).getName());
        player.sendMessage("§7HP: §c" + claim.getHealth() + "/" + claim.getMaxHealth());
        if (isAtLeast(player, claim, "OWNER")) player.sendMessage("§7Brankas: §b" + claim.getDiamondBalance() + " Diamond");
        player.sendMessage("§7PvP: §f" + (claim.isPvpEnabled() ? "§cON" : "§aOFF"));
    }

    @Subcommand("map")
    public void showMap(Player player) {
        dev.arqo.land.utils.MapVisualizer.sendMap(player, chunkManager);
    }

    @Subcommand("border")
    public void showBorder(Player player) {
        chunkManager.showBorder(player);
    }
}
