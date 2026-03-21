package dev.arqo.land.core;

import dev.arqo.land.ArqoLand;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookManager {
    private final ArqoLand plugin;

    public DiscordWebhookManager(ArqoLand plugin) {
        this.plugin = plugin;
    }

    public void sendRaidAlert(String landName, String attacker, int currentHp, int maxHp) {
        if (!plugin.getConfig().getBoolean("discord-webhook.enabled")) return;

        String urlString = plugin.getConfig().getString("discord-webhook.url");
        if (urlString == null || urlString.isEmpty() || urlString.contains("xxxx")) return;

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                double percent = ((double) currentHp / maxHp) * 100;
                String colorStr = plugin.getConfig().getString("discord-webhook.embed-color", "#FF0000").replace("#", "");
                int color = Integer.parseInt(colorStr, 16);

                String jsonPayload = "{"
                        + "\"embeds\": [{"
                        + "\"title\": \"🚨 WILAYAH SEDANG DI-RAID! 🚨\","
                        + "\"color\": " + color + ","
                        + "\"fields\": ["
                        + "{\"name\": \"Wilayah\", \"value\": \"**" + landName + "**\", \"inline\": true},"
                        + "{\"name\": \"Penyerang\", \"value\": \"`" + attacker + "`\", \"inline\": true},"
                        + "{\"name\": \"Kesehatan (HP)\", \"value\": \"**" + currentHp + " / " + maxHp + "** (" + String.format("%.1f", percent) + "%)\", \"inline\": false}"
                        + "],"
                        + "\"footer\": {\"text\": \"ArqoLand Enterprise Monitoring System\"},"
                        + "\"timestamp\": \"" + java.time.Instant.now().toString() + "\""
                        + "}]"
                        + "}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Gagal mengirim Discord Webhook: " + e.getMessage());
            }
        });
    }
}
