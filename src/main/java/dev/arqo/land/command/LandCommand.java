package dev.arqo.land.command;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.core.ChunkManager;
import dev.arqo.land.core.EconomyManager;
import dev.arqo.land.model.ClaimData;
import dev.arqo.land.model.LandMember;
import dev.arqo.land.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import revxrsal.commands.bukkit.annotation.CommandPermission;
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

    @Subcommand("help")
    public void help(CommandSender sender) {
        sender.sendMessage("§e=== ArqoLand Perintah ===");
        sender.sendMessage("§f/al claimland <nama> §7- Klaim wilayah");
        sender.sendMessage("§f/al status [land] §7- Info wilayah & brankas");
        sender.sendMessage("§f/al setoran §7- Buka GUI Setoran Diamond");
        sender.sendMessage("§f/al setspawn §7- Atur titik spawn");
        sender.sendMessage("§f/al spawn [land] §7- Teleport ke spawn");
        sender.sendMessage("§f/al rename <id> §7- Ganti ID Wilayah");
        sender.sendMessage("§f/al setdisplayname <nama> §7- Ganti Nama Tampilan");
        sender.sendMessage("§f/al topSetoran/topBrankas §7- Leaderboard");
        sender.sendMessage("§f/al addmember/kickmember <player>");
        sender.sendMessage("§f/al pvp <on/off> §7- Toggle PvP");
        sender.sendMessage("§f/al ally/enemy <land>");
        sender.sendMessage("§f/al map/border §7- Visualisasi");
        sender.sendMessage("§f/al upgrade/withdraw §7- Manajemen Brankas");
    }

    @Subcommand("claimland")
    public void claim(Player player, String name) {
        Chunk chunk = player.getLocation().getChunk();
        if (chunkManager.getClaimAt(chunk) != null) {
            player.sendMessage("§cWilayah ini sudah diklaim!");
            return;
        }
        if (chunkManager.getClaimByName(name) != null) {
            player.sendMessage("§cNama wilayah tersebut sudah digunakan!");
            return;
        }

        long myLandsCount = chunkManager.getLandNameCache().values().stream()
                .filter(c -> c.getOwner().equals(player.getUniqueId())).count();
        int maxLands = plugin.getConfig().getInt("limits.default-max-lands", 1);
        if (myLandsCount >= maxLands && !player.hasPermission("arqoland.admin")) {
            player.sendMessage("§cLimit wilayah tercapai (§e" + maxLands + "§c)!");
            return;
        }

        int id = plugin.getDatabaseManager().createLand(name, player.getUniqueId(), name);
        if (id == -1) {
            player.sendMessage("§cError database!");
            return;
        }

        ClaimData claim = new ClaimData(id, name, player.getUniqueId());
        claim.setHealth(500);
        claim.setMaxHealth(500);
        claim.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        
        chunkManager.addClaim(chunk, claim);
        player.sendMessage("§a[Klaim] §fWilayah §e" + name + " §fberhasil dibuat!");
    }

    @Subcommand("status")
    public void status(Player player, @Optional String name) {
        ClaimData claim = (name == null) ? chunkManager.getClaimAt(player.getLocation().getChunk()) : chunkManager.getClaimByName(name);
        if (claim == null) {
            player.sendMessage("§cWilayah tidak ditemukan!");
            return;
        }
        player.sendMessage("§e=== Status: " + claim.getDisplayName() + " ===");
        player.sendMessage("§7Nama ID: §f" + claim.getName());
        player.sendMessage("§7Pemilik: §f" + Bukkit.getOfflinePlayer(claim.getOwner()).getName());
        player.sendMessage("§7HP: §c" + claim.getHealth() + "/" + claim.getMaxHealth());
        if (isAtLeast(player, claim, "ADMIN")) {
            player.sendMessage("§7Brankas: §b" + claim.getDiamondBalance() + " Diamond");
        }
        player.sendMessage("§7PvP: §f" + (claim.isPvpEnabled() ? "§cON" : "§aOFF"));
    }

    @Subcommand("setoran")
    public void setoran(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "MEMBER")) {
            player.sendMessage("§cKamu harus berada di wilayah sendiri!");
            return;
        }
        economyManager.openDepositGui(player, claim);
    }

    @Subcommand("setspawn")
    public void setSpawn(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "ADMIN")) {
            player.sendMessage("§cKamu tidak memiliki izin!");
            return;
        }
        claim.setSpawnLocation(player.getLocation());
        chunkManager.saveLandAsync(claim);
        player.sendMessage("§a[Spawn] §fLokasi spawn berhasil diatur!");
    }

    @Subcommand("spawn")
    public void spawn(Player player, @Optional String name) {
        ClaimData claim;
        if (name == null) {
            // Priority 1: Current land
            claim = chunkManager.getClaimAt(player.getLocation().getChunk());
            // Priority 2: Player's own land if not in a land or current land isn't theirs
            if (claim == null || (!claim.getOwner().equals(player.getUniqueId()) && 
                claim.getMembers().stream().noneMatch(m -> m.getUuid().equals(player.getUniqueId())))) {
                claim = chunkManager.getLandNameCache().values().stream()
                        .filter(c -> c.getOwner().equals(player.getUniqueId()))
                        .findFirst().orElse(claim);
            }
        } else {
            claim = chunkManager.getClaimByName(name);
        }

        if (claim == null) {
            player.sendMessage("§cWilayah tidak ditemukan!");
            return;
        }
        if (claim.getSpawnLocation() == null) {
            player.sendMessage("§cWilayah ini belum memiliki spawn!");
            return;
        }
        player.teleport(claim.getSpawnLocation());
        player.sendMessage("§a[Spawn] §fTeleportasi ke §e" + claim.getDisplayName());
    }

    @Subcommand("rename")
    public void rename(Player player, String newName) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "OWNER")) {
            player.sendMessage("§cHanya pemilik yang bisa mengubah ID!");
            return;
        }
        if (chunkManager.getClaimByName(newName) != null) {
            player.sendMessage("§cID sudah digunakan!");
            return;
        }

        chunkManager.getLandNameCache().remove(claim.getName().toLowerCase());
        claim.setName(newName);
        chunkManager.getLandNameCache().put(newName.toLowerCase(), claim);
        
        chunkManager.saveLandAsync(claim);
        player.sendMessage("§a[Rename] §fID Wilayah diubah menjadi §e" + newName);
    }

    @Subcommand("setdisplayname")
    public void setDisplayName(Player player, String displayName) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "ADMIN")) {
            player.sendMessage("§cIzin ditolak!");
            return;
        }
        claim.setDisplayName(ColorUtil.translate(displayName));
        chunkManager.saveLandAsync(claim);
        player.sendMessage("§a[Display] §fNama tampilan diubah menjadi " + claim.getDisplayName());
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
        player.sendMessage("§a[Member] §e" + targetName + " §fberhasil ditambah.");
    }

    @Subcommand("kickmember")
    public void kickMember(Player player, String targetName) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "ADMIN")) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        claim.getMembers().removeIf(m -> m.getUuid().equals(target.getUniqueId()));
        plugin.getDatabaseManager().removeMember(claim.getId(), target.getUniqueId());
        player.sendMessage("§a[Member] §e" + targetName + " §fdikeluarkan.");
    }

    @Subcommand("pvp")
    public void togglePvp(Player player, String toggle) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, claim, "ADMIN")) return;

        boolean enable = toggle.equalsIgnoreCase("on");
        claim.setPvpEnabled(enable);
        chunkManager.saveLandAsync(claim);
        player.sendMessage("§a[PvP] §fPvP diatur ke: §e" + (enable ? "ON" : "OFF"));
    }

    @Subcommand("ally")
    public void ally(Player player, String landName) {
        ClaimData myLand = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (myLand == null || !isAtLeast(player, myLand, "ADMIN")) return;

        ClaimData targetLand = chunkManager.getClaimByName(landName);
        if (targetLand == null) return;

        allyInvitations.computeIfAbsent(targetLand.getId(), k -> ConcurrentHashMap.newKeySet()).add(myLand.getId());
        player.sendMessage("§a[Ally] §fPermintaan aliansi dikirim ke §e" + targetLand.getDisplayName());
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
            player.sendMessage("§a[Ally] §fSekarang beraliansi dengan §e" + targetLand.getDisplayName());
        }
    }

    @Subcommand("enemy")
    public void enemy(Player player, String landName) {
        ClaimData myLand = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (!isAtLeast(player, myLand, "ADMIN")) return;
        ClaimData targetLand = chunkManager.getClaimByName(landName);
        if (targetLand == null || myLand.getId() == targetLand.getId()) return;

        myLand.getEnemyLands().add(targetLand.getId());
        plugin.getDatabaseManager().saveEnemy(myLand.getId(), targetLand.getId());
        player.sendMessage("§c[Enemy] §e" + targetLand.getDisplayName() + " §fkini adalah MUSUH!");
    }

    @Subcommand("withdraw")
    public void withdraw(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "OWNER")) return;
        economyManager.openWithdrawGui(player, claim);
    }

    @Subcommand("upgrade")
    public void upgrade(Player player) {
        ClaimData claim = chunkManager.getClaimAt(player.getLocation().getChunk());
        if (claim == null || !isAtLeast(player, claim, "OWNER")) return;
        economyManager.openUpgradeGui(player, claim);
    }

    @Subcommand("map")
    public void showMap(Player player) {
        dev.arqo.land.util.MapVisualizer.sendMap(player, chunkManager);
    }

    @Subcommand("topBrankas")
    public void topBrankas(CommandSender sender) {
        List<ClaimData> lands = new ArrayList<>(chunkManager.getLandNameCache().values());
        lands.sort((a, b) -> Integer.compare(b.getDiamondBalance(), a.getDiamondBalance()));
        
        sender.sendMessage("§e=== Top 10 Brankas (Diamond) ===");
        for (int i = 0; i < Math.min(10, lands.size()); i++) {
            ClaimData l = lands.get(i);
            sender.sendMessage("§f" + (i + 1) + ". §e" + l.getDisplayName() + " §7- §b" + l.getDiamondBalance() + " Diamond");
        }
    }

    @Subcommand("topSetoran")
    public void topSetoran(CommandSender sender) {
        Map<ClaimData, Integer> landContributions = new HashMap<>();
        for (ClaimData claim : chunkManager.getLandNameCache().values()) {
            int total = claim.getMembers().stream().mapToInt(LandMember::getTotalContributed).sum();
            landContributions.put(claim, total);
        }

        List<ClaimData> sorted = new ArrayList<>(landContributions.keySet());
        sorted.sort((a, b) -> Integer.compare(landContributions.get(b), landContributions.get(a)));

        sender.sendMessage("§e=== Top 10 Total Setoran Wilayah ===");
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            ClaimData l = sorted.get(i);
            sender.sendMessage("§f" + (i + 1) + ". §e" + l.getDisplayName() + " §7- §b" + landContributions.get(l) + " Diamond");
        }
    }

    @Subcommand("border")
    public void showBorder(Player player) {
        boolean enabled = chunkManager.toggleBorder(player);
        String status = enabled ? "§aON" : "§cOFF";
        player.sendActionBar(net.kyori.adventure.text.Component.text("§eVisualisasi Border: " + status));
        player.sendMessage("§e[Border] §fVisualisasi border diatur ke: " + status);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, enabled ? 1.2f : 0.8f);
    }

    @Subcommand("admin")
    @CommandPermission("arqoland.admin")
    public void adminMenu(Player player) {
        ChestGui gui = new ChestGui(6, "§8Admin: Land Management");
        PaginatedPane pages = new PaginatedPane(0, 0, 9, 5);
        List<GuiItem> items = new ArrayList<>();

        for (ClaimData land : plugin.getChunkManager().getLandNameCache().values()) {
            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + land.getName());
            meta.setLore(Arrays.asList("§7Owner: §f" + Bukkit.getOfflinePlayer(land.getOwner()).getName(), "§eClick to Manage"));
            item.setItemMeta(meta);
            items.add(new GuiItem(item, event -> openLandSettings(player, land)));
        }

        pages.populateWithGuiItems(items);
        gui.addPane(pages);
        gui.show(player);
    }

    private void openLandSettings(Player player, ClaimData land) {
        ChestGui gui = new ChestGui(3, "§8Manage: " + land.getName());
        OutlinePane pane = new OutlinePane(0, 0, 9, 3);

        ItemStack del = new ItemStack(Material.BARRIER);
        ItemMeta m = del.getItemMeta(); m.setDisplayName("§cDELETE LAND"); del.setItemMeta(m);
        pane.addItem(new GuiItem(del, event -> {
            plugin.getChunkManager().deleteLand(land);
            player.sendMessage("§cWilayah dihapus.");
            player.closeInventory();
        }));

        gui.addPane(pane);
        gui.show(player);
    }
}
