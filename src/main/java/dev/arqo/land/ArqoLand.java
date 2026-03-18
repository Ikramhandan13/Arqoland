package dev.arqo.land;

import dev.arqo.land.commands.AdminLandCommand;
import dev.arqo.land.commands.LandCommand;
import dev.arqo.land.database.DatabaseManager;
import dev.arqo.land.listeners.PlayerListener;
import dev.arqo.land.listeners.ProtectionListener;
import dev.arqo.land.listeners.RaidListener;
import dev.arqo.land.managers.ChunkManager;
import dev.arqo.land.managers.DiscordWebhookManager;
import dev.arqo.land.managers.EconomyManager;
import dev.arqo.land.managers.FlagManager;
import dev.arqo.land.managers.PerkManager;
import dev.arqo.land.managers.TurretManager;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitLamp;

public class ArqoLand extends JavaPlugin {

    private static ArqoLand instance;
    private DatabaseManager databaseManager;
    private boolean isFolia;

    // Deklarasi Managers
    private ChunkManager chunkManager;
    private EconomyManager economyManager;
    private FlagManager flagManager;
    private PerkManager perkManager;
    private DiscordWebhookManager webhookManager;
    private TurretManager turretManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        checkFolia();

        // 1. Inisialisasi Database
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();
        this.databaseManager.initializeTables();

        // 2. Inisialisasi Managers (Logika Sistem)
        this.chunkManager = new ChunkManager(this);
        this.databaseManager.loadAllData();
        this.chunkManager.startTaxTask(); // Mulai pajak mingguan

        this.economyManager = new EconomyManager(this);
        this.flagManager = new FlagManager();
        this.perkManager = new PerkManager(this);
        this.webhookManager = new DiscordWebhookManager(this);
        this.turretManager = new TurretManager(this); // Mulai defense system
        // 3. Registrasi Perintah & Event
        registerCommands();
        registerListeners();
        
        getLogger().info("ArqoLand Enterprise diaktifkan secara masif.");
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
        getLogger().info("ArqoLand Enterprise dimatikan.");
    }

    private void checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            this.isFolia = true;
            getLogger().info("Arsitektur Folia terdeteksi. Multi-threading diaktifkan.");
        } catch (ClassNotFoundException e) {
            this.isFolia = false;
            getLogger().info("Arsitektur Paper terdeteksi. Berjalan dalam Single-thread.");
        }
    }

    private void registerCommands() {
        // Menggunakan Revxrsal Lamp v4
        BukkitLamp lamp = BukkitLamp.builder(this).build();
        
        lamp.register(new LandCommand(this, this.chunkManager, this.economyManager));
        lamp.register(new AdminLandCommand(this));
    }

    private void registerListeners() {
        // Mendaftarkan perlindungan dan event pemain ke server
        getServer().getPluginManager().registerEvents(new ProtectionListener(this.chunkManager, this.flagManager, this.turretManager), this);
        getServer().getPluginManager().registerEvents(new RaidListener(this.chunkManager, this.webhookManager), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, this.chunkManager, this.perkManager), this);
        getServer().getPluginManager().registerEvents(this.perkManager, this); // Baru: Untuk Crop Boost
    }

    public static ArqoLand getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ChunkManager getChunkManager() { return chunkManager; }
    public TurretManager getTurretManager() { return turretManager; }
    public PerkManager getPerkManager() { return perkManager; }
    public boolean isFolia() { return isFolia; }
}
