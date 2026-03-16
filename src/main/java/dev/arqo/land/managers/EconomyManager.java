package dev.arqo.land.managers;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import dev.arqo.land.ArqoLand;
import dev.arqo.land.models.ClaimData;
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

    public void openWithdrawGui(Player player, ClaimData claimData) {
        ChestGui gui = new ChestGui(3, "Tarik Diamond: " + claimData.getName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        // Opsi Tarik 16, 32, 64
        addWithdrawItem(pane, 16, 2, 1, player, claimData);
        addWithdrawItem(pane, 32, 4, 1, player, claimData);
        addWithdrawItem(pane, 64, 6, 1, player, claimData);

        gui.addPane(pane);
        gui.show(player);
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
