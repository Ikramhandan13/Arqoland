package dev.arqo.land.commands;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.models.ClaimData;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"arqoland admin", "land admin", "al admin"})
public class AdminLandCommand {
    private final ArqoLand plugin;
    private final ChunkManager chunkManager;

    public AdminLandCommand(ArqoLand plugin, ChunkManager chunkManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
    }

    @Subcommand("reload")
    @CommandPermission("arqoland.admin")
    public void reloadConfig(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage("§aKonfigurasi ArqoLand berhasil dimuat ulang.");
    }

    @Subcommand("delete")
    @CommandPermission("arqoland.admin")
    public void forceDelete(Player player) {
        Chunk currentChunk = player.getLocation().getChunk();
        ClaimData claimData = chunkManager.getClaimAt(currentChunk);

        if (claimData == null) {
            player.sendMessage("§cTidak ada klaim di chunk ini.");
            return;
        }

        chunkManager.removeClaim(currentChunk);
        player.sendMessage("§aBerhasil menghapus klaim wilayah §e" + claimData.getName() + " §asecara paksa.");
    }

    @Subcommand("sethealth")
    @CommandPermission("arqoland.admin")
    public void setHealth(Player player, int amount) {
        Chunk currentChunk = player.getLocation().getChunk();
        ClaimData claimData = chunkManager.getClaimAt(currentChunk);

        if (claimData == null) {
            player.sendMessage("§cTidak ada klaim di chunk ini.");
            return;
        }

        claimData.setHealth(amount);
        player.sendMessage("§aDarah wilayah §e" + claimData.getName() + " §adiatur menjadi §c" + amount + " HP§a.");
    }
}
