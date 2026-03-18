package dev.arqo.land.commands;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import dev.arqo.land.ArqoLand;
import dev.arqo.land.models.ClaimData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import revxrsal.commands.annotation.CommandPermission;
import revxrsal.commands.annotation.Subcommand;

import java.util.ArrayList;
import java.util.List;

@revxrsal.commands.annotation.Command({"arqoland", "land", "al"})
public class AdminLandCommand {
    private final ArqoLand plugin;

    public AdminLandCommand(ArqoLand plugin) {
        this.plugin = plugin;
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
            List<String> lore = new ArrayList<>();
            lore.add("§7Owner: §f" + org.bukkit.Bukkit.getOfflinePlayer(land.getOwner()).getName());
            lore.add("§7HP: §c" + land.getHealth() + "/" + land.getMaxHealth());
            lore.add("§7Brankas: §b" + land.getDiamondBalance() + " Diamond");
            lore.add("§7PvP: " + (land.isPvpEnabled() ? "§cON" : "§aOFF"));
            lore.add("");
            lore.add("§eClick to Manage Settings");
            meta.setLore(lore);
            item.setItemMeta(meta);

            items.add(new GuiItem(item, event -> openLandSettings(player, land)));
        }

        pages.populateWithGuiItems(items);
        gui.addPane(pages);

        // Navigation
        OutlinePane nav = new OutlinePane(0, 5, 9, 1);
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prev.getItemMeta();
        prevMeta.setDisplayName("§7Previous Page");
        prev.setItemMeta(prevMeta);
        nav.addItem(new GuiItem(prev, event -> {
            if (pages.getPage() > 0) {
                pages.setPage(pages.getPage() - 1);
                gui.update();
            }
        }));

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName("§7Next Page");
        next.setItemMeta(nextMeta);
        nav.addItem(new GuiItem(next, event -> {
            if (pages.getPage() < pages.getPages() - 1) {
                pages.setPage(pages.getPage() + 1);
                gui.update();
            }
        }));

        gui.addPane(nav);
        gui.show(player);
    }

    private void openLandSettings(Player player, ClaimData land) {
        ChestGui gui = new ChestGui(3, "§8Manage: " + land.getName());
        OutlinePane pane = new OutlinePane(0, 0, 9, 3);

        // PvP Toggle
        ItemStack pvp = new ItemStack(Material.IRON_SWORD);
        ItemMeta pvpMeta = pvp.getItemMeta();
        pvpMeta.setDisplayName("§eToggle PvP");
        List<String> pvpLore = new ArrayList<>();
        pvpLore.add("§7Current: " + (land.isPvpEnabled() ? "§cON" : "§aOFF"));
        pvpMeta.setLore(pvpLore);
        pvp.setItemMeta(pvpMeta);
        pane.addItem(new GuiItem(pvp, event -> {
            land.setPvpEnabled(!land.isPvpEnabled());
            plugin.getChunkManager().saveLandAsync(land);
            player.sendMessage("§a[Admin] §fPvP wilayah §e" + land.getName() + " §fdiatur ke: " + (land.isPvpEnabled() ? "§cON" : "§aOFF"));
            openLandSettings(player, land);
        }));

        // Reset HP
        ItemStack hp = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta hpMeta = hp.getItemMeta();
        hpMeta.setDisplayName("§eReset Health");
        hp.setItemMeta(hpMeta);
        pane.addItem(new GuiItem(hp, event -> {
            land.setHealth(land.getMaxHealth());
            plugin.getChunkManager().saveLandAsync(land);
            player.sendMessage("§a[Admin] §fHP wilayah §e" + land.getName() + " §fberhasil direset.");
            openLandSettings(player, land);
        }));

        // Add 100 Diamonds to Vault
        ItemStack diamonds = new ItemStack(Material.DIAMOND);
        ItemMeta dMeta = diamonds.getItemMeta();
        dMeta.setDisplayName("§bAdd 100 Diamonds to Vault");
        diamonds.setItemMeta(dMeta);
        pane.addItem(new GuiItem(diamonds, event -> {
            land.setDiamondBalance(land.getDiamondBalance() + 100);
            plugin.getChunkManager().saveLandAsync(land);
            player.sendMessage("§a[Admin] §fMenambah 100 diamond ke brankas §e" + land.getName());
            openLandSettings(player, land);
        }));

        // Delete Land
        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta delMeta = delete.getItemMeta();
        delMeta.setDisplayName("§c§lDELETE LAND");
        delete.setItemMeta(delMeta);
        pane.addItem(new GuiItem(delete, event -> {
            plugin.getChunkManager().deleteLand(land);
            player.sendMessage("§c[Admin] §fWilayah §e" + land.getName() + " §ftelah DIHAPUS.");
            player.closeInventory();
        }));

        gui.addPane(pane);
        gui.show(player);
    }
}
