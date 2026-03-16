package dev.arqo.land.managers;

import dev.arqo.land.ArqoLand;
import dev.arqo.land.utils.FoliaScheduler;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookManager {
    private final ArqoLand plugin;
    private final boolean enabled;
    private final String webhookUrl;

    public DiscordWebhookManager(ArqoLand plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("discord-webhook.enabled");
        this.webhookUrl = plugin.getConfig().getString("discord-webhook.url");
    }

    public void sendRaidAlert(String landName, String attackerName, int currentHealth, int maxHealth) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) return;

        FoliaScheduler.runAsync(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonPayload = "{\"content\": null, \"embeds\": [{\"title\": \"🚨 PERINGATAN RAID 🚨\", \"description\": \"Markas **" + landName + "** sedang diserang oleh **" + attackerName + "**!\\nSisa Darah: " + currentHealth + "/" + maxHealth + "\", \"color\": 16711680}]}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Gagal mengirim webhook Discord: " + e.getMessage());
            }
        });
    }
}
