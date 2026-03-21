package dev.arqo.land.core;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import dev.arqo.land.ArqoLand;
import dev.arqo.land.model.ClaimData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class EconomyManager {
    private final ArqoLand plugin;

    public EconomyManager(ArqoLand plugin) {
        this.plugin = plugin;
    }

    public int countDiamonds(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public boolean takeDiamonds(Player player, int amount) {
        if (countDiamonds(player) < amount) return false;

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.DIAMOND) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                    break;
                }
            }
            if (remaining <= 0) break;
        }
        return true;
    }

    public void openDepositGui(Player player, ClaimData claimData) {
        ChestGui gui = new ChestGui(3, "Setoran Diamond: " + claimData.getDisplayName());
        
        // Pane kosong agar pemain bisa menaruh barang di slot manapun
        StaticPane pane = new StaticPane(0, 0, 9, 3);
        gui.addPane(pane);

        gui.setOnClose(event -> {
            int totalDiamonds = 0;
            org.bukkit.inventory.Inventory inv = event.getInventory();
            
            for (ItemStack item : inv.getContents()) {
                if (item == null) continue;
                if (item.getType() == Material.DIAMOND) {
                    totalDiamonds += item.getAmount();
                } else {
                    // Kembalikan barang bukan diamond ke inventory pemain
                    player.getInventory().addItem(item).values().forEach(remaining -> 
                        player.getWorld().dropItemNaturally(player.getLocation(), remaining));
                }
            }

            if (totalDiamonds > 0) {
                final int finalDiamonds = totalDiamonds;
                claimData.setDiamondBalance(claimData.getDiamondBalance() + finalDiamonds);
                
                // Update kontribusi member
                for (dev.arqo.land.model.LandMember m : claimData.getMembers()) {
                    if (m.getUuid().equals(player.getUniqueId())) {
                        m.addContribution(finalDiamonds);
                        plugin.getDatabaseManager().saveMemberContribution(claimData.getId(), m);
                        break;
                    }
                }
                
                plugin.getChunkManager().saveLandAsync(claimData);

                // Notifikasi Action Bar, Chat, dan Sound
                String msg = "§a+§b" + finalDiamonds + " Diamond §fdisetorkan ke §e" + claimData.getDisplayName();
                player.sendMessage("§b§l[SETORAN] §f" + msg);
                player.sendActionBar(net.kyori.adventure.text.Component.text(msg));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
        });

        gui.show(player);
    }

    public void openWithdrawGui(Player player, ClaimData claimData) {
        ChestGui gui = new ChestGui(3, "Tarik Diamond: " + claimData.getName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);
        addWithdrawItem(pane, 16, 2, 1, player, claimData);
        addWithdrawItem(pane, 32, 4, 1, player, claimData);
        addWithdrawItem(pane, 64, 6, 1, player, claimData);

        gui.addPane(pane);
        gui.show(player);
    }

    public void openUpgradeGui(Player player, ClaimData claimData) {
        if (claimData == null) {
            player.sendMessage("§cKamu tidak berada di wilayah manapun!");
            return;
        }

        ChestGui gui = new ChestGui(3, "Upgrade Wilayah: " + claimData.getName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        addUpgradeItem(pane, Material.GOLDEN_PICKAXE, "Haste", claimData.getPerkHaste(), 0, 0, player, claimData);
        addUpgradeItem(pane, Material.SUGAR, "Speed", claimData.getPerkSpeed(), 1, 0, player, claimData);
        addUpgradeItem(pane, Material.DIAMOND_SWORD, "Strength", claimData.getPerkStrength(), 2, 0, player, claimData);
        addUpgradeItem(pane, Material.RABBIT_FOOT, "Jump", claimData.getPerkJump(), 3, 0, player, claimData);
        addUpgradeItem(pane, Material.WHEAT, "Crop", claimData.getPerkCrop(), 4, 0, player, claimData);
        addUpgradeItem(pane, Material.ENCHANTED_GOLDEN_APPLE, "Heart", (claimData.getMaxHealth() / 500) - 1, 6, 0, player, claimData);
        addUpgradeItem(pane, Material.DISPENSER, "Turret", claimData.getTurretLevel(), 8, 0, player, claimData);
        
        // Upgrade Infinity Ammo
        int infCost = plugin.getConfig().getInt("upgrades.turret.infinity-ammo", 7000);
        ItemStack infinityItem = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta infMeta = infinityItem.getItemMeta();
        if (infMeta != null) {
            infMeta.setDisplayName("§b§lInfinity Turret Ammo");
            infMeta.setLore(Arrays.asList(
                "§7Status: " + (claimData.isTurretAmmoFree() ? "§aAKTIF" : "§cNONAKTIF"),
                "§7Kegunaan: §fTurret tidak butuh isi panah lagi.",
                "§7Biaya: §b" + infCost + " Diamond (Brankas)",
                "",
                claimData.isTurretAmmoFree() ? "§aSudah dimiliki" : "§eKlik untuk beli!"
            ));
            infinityItem.setItemMeta(infMeta);
        }
        pane.addItem(new GuiItem(infinityItem, event -> {
            if (claimData.isTurretAmmoFree()) return;
            if (claimData.getDiamondBalance() < infCost) {
                player.sendMessage("§cSaldo brankas tidak cukup!");
                return;
            }
            claimData.setDiamondBalance(claimData.getDiamondBalance() - infCost);
            claimData.setTurretAmmoFree(true);
            plugin.getChunkManager().saveLandAsync(claimData);
            player.sendMessage("§a[Upgrade] §fTurret sekarang memiliki §bInfinity Ammo§f!");
            player.closeInventory();
        }), 4, 2);

        gui.addPane(pane);
        gui.show(player);
    }

    private void addUpgradeItem(StaticPane pane, Material material, String type, int currentLevel, int x, int y, Player player, ClaimData claimData) {
        int nextLevel = currentLevel + 1;
        int maxLvl = type.equals("Heart") ? 6 : 5;
        int cost = (nextLevel > maxLvl) ? 0 : getUpgradeCost(nextLevel, type);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eUpgrade " + type);
            if (nextLevel > maxLvl) {
                meta.setLore(Arrays.asList("§7Level saat ini: §f" + (type.equals("Heart") ? claimData.getMaxHealth() + " HP" : currentLevel), "§a§lMAX LEVEL"));
            } else {
                meta.setLore(Arrays.asList(
                        "§7Saat ini: §f" + (type.equals("Heart") ? claimData.getMaxHealth() + " HP" : currentLevel),
                        "§7Berikutnya: §f" + (type.equals("Heart") ? getHeartValue(nextLevel) + " HP" : nextLevel),
                        "§7Biaya: §b" + cost + " Diamond (Brankas)",
                        "",
                        "§eKlik untuk upgrade!"
                ));
            }
            item.setItemMeta(meta);
        }

        pane.addItem(new GuiItem(item, event -> {
            if (nextLevel > maxLvl) return;
            if (claimData.getDiamondBalance() < cost) {
                player.sendMessage("§cSaldo brankas tidak cukup!");
                return;
            }

            claimData.setDiamondBalance(claimData.getDiamondBalance() - cost);
            
            switch (type.toLowerCase()) {
                case "haste": claimData.setPerkHaste(nextLevel); break;
                case "speed": claimData.setPerkSpeed(nextLevel); break;
                case "strength": claimData.setPerkStrength(nextLevel); break;
                case "jump": claimData.setPerkJump(nextLevel); break;
                case "crop": claimData.setPerkCrop(nextLevel); break;
                case "turret": claimData.setTurretLevel(nextLevel); break;
                case "heart": 
                    claimData.setMaxHealth(getHeartValue(nextLevel)); 
                    claimData.setHealth(claimData.getMaxHealth());
                    break;
            }

            plugin.getChunkManager().saveLandAsync(claimData);
            player.sendMessage("§a[Upgrade] §fUpgrade §e" + type.toUpperCase() + " §fberhasil!");
            player.closeInventory();
            openUpgradeGui(player, claimData);
        }), x, y);
    }

    private int getHeartValue(int level) {
        return switch (level) {
            case 1 -> 750;
            case 2 -> 1000;
            case 3 -> 1500;
            case 4 -> 2000;
            case 5 -> 3000;
            case 6 -> 5000;
            default -> 500;
        };
    }

    private int getUpgradeCost(int level, String type) {
        String path = switch (type.toLowerCase()) {
            case "heart" -> "upgrades.heart.level-" + level;
            case "turret" -> "upgrades.turret.level-" + level;
            default -> "upgrades.perks.level-" + level;
        };
        return plugin.getConfig().getInt(path, 0);
    }

    private void addWithdrawItem(StaticPane pane, int amount, int x, int y, Player player, ClaimData claimData) {
        ItemStack item = new ItemStack(Material.DIAMOND, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bTarik " + amount + " Diamond");
            meta.setLore(Arrays.asList("§7Klik untuk menarik fisik Diamond", "§7Saldo saat ini: §f" + claimData.getDiamondBalance()));
            item.setItemMeta(meta);
        }

        pane.addItem(new GuiItem(item, event -> {
            if (claimData.getDiamondBalance() >= amount) {
                claimData.setDiamondBalance(claimData.getDiamondBalance() - amount);
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, amount));
                player.sendMessage("§a[Brankas] §fBerhasil menarik §b" + amount + " Diamond§f.");
                plugin.getChunkManager().saveLandAsync(claimData);
                player.closeInventory();
            } else {
                player.sendMessage("§cSaldo brankas tidak mencukupi!");
            }
        }), x, y);
    }
}
