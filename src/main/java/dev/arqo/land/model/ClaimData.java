package dev.arqo.land.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Timestamp;

public class ClaimData {
    private final int id;
    private String name;
    private String displayName;
    private final UUID owner;
    private volatile int health;
    private volatile int maxHealth;
    private volatile int diamondBalance;
    private volatile boolean pvpEnabled;
    private String greetingMessage;
    private volatile boolean isPublic;
    private Timestamp createdAt;
    private Timestamp lastActive;

    // Spawn Location
    private String spawnWorld;
    private double spawnX, spawnY, spawnZ;
    private float spawnYaw, spawnPitch;
    
    private boolean flagMobSpawn;
    private boolean flagFireSpread;
    private boolean flagInteract;
    private boolean flagPiston;

    private int perkHaste;
    private int perkSpeed;
    private int perkStrength;
    private int perkJump;
    private int perkCrop;

    private int turretLevel;
    private boolean turretAmmoFree;

    private final Set<LandMember> members;
    private final Set<Integer> alliedLands;
    private final Set<Integer> enemyLands;
    private final Set<String> chunkKeys;
    private final Set<Location> turretLocations;

    public ClaimData(int id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.members = ConcurrentHashMap.newKeySet();
        this.alliedLands = ConcurrentHashMap.newKeySet();
        this.enemyLands = ConcurrentHashMap.newKeySet();
        this.chunkKeys = ConcurrentHashMap.newKeySet();
        this.turretLocations = ConcurrentHashMap.newKeySet();
        this.lastActive = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName == null ? name : displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UUID getOwner() { return owner; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public int getDiamondBalance() { return diamondBalance; }
    public void setDiamondBalance(int diamondBalance) { this.diamondBalance = diamondBalance; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public String getGreetingMessage() { return greetingMessage; }
    public void setGreetingMessage(String greetingMessage) { this.greetingMessage = greetingMessage; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getLastActive() { return lastActive; }
    public void setLastActive(Timestamp lastActive) { this.lastActive = lastActive; }
    public void updateLastActive() { this.lastActive = new Timestamp(System.currentTimeMillis()); }

    public void setSpawnLocation(Location loc) {
        if (loc == null) return;
        this.spawnWorld = loc.getWorld().getName();
        this.spawnX = loc.getX();
        this.spawnY = loc.getY();
        this.spawnZ = loc.getZ();
        this.spawnYaw = loc.getYaw();
        this.spawnPitch = loc.getPitch();
    }

    public Location getSpawnLocation() {
        if (spawnWorld == null || Bukkit.getWorld(spawnWorld) == null) return null;
        return new Location(Bukkit.getWorld(spawnWorld), spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    // New Getters for DB Sync
    public String getSpawnWorld() { return spawnWorld; }
    public double getSpawnX() { return spawnX; }
    public double getSpawnY() { return spawnY; }
    public double getSpawnZ() { return spawnZ; }
    public float getSpawnYaw() { return spawnYaw; }
    public float getSpawnPitch() { return spawnPitch; }
    
    public void setSpawnData(String world, double x, double y, double z, float yaw, float pitch) {
        this.spawnWorld = world;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.spawnYaw = yaw;
        this.spawnPitch = pitch;
    }

    public boolean isFlagMobSpawn() { return flagMobSpawn; }
    public void setFlagMobSpawn(boolean flagMobSpawn) { this.flagMobSpawn = flagMobSpawn; }
    public boolean isFlagFireSpread() { return flagFireSpread; }
    public void setFlagFireSpread(boolean flagFireSpread) { this.flagFireSpread = flagFireSpread; }
    public boolean isFlagInteract() { return flagInteract; }
    public void setFlagInteract(boolean flagInteract) { this.flagInteract = flagInteract; }
    public boolean isFlagPiston() { return flagPiston; }
    public void setFlagPiston(boolean flagPiston) { this.flagPiston = flagPiston; }

    public int getPerkHaste() { return perkHaste; }
    public void setPerkHaste(int perkHaste) { this.perkHaste = perkHaste; }
    public int getPerkSpeed() { return perkSpeed; }
    public void setPerkSpeed(int perkSpeed) { this.perkSpeed = perkSpeed; }
    public int getPerkStrength() { return perkStrength; }
    public void setPerkStrength(int perkStrength) { this.perkStrength = perkStrength; }
    public int getPerkJump() { return perkJump; }
    public void setPerkJump(int perkJump) { this.perkJump = perkJump; }
    public int getPerkCrop() { return perkCrop; }
    public void setPerkCrop(int perkCrop) { this.perkCrop = perkCrop; }

    public int getTurretLevel() { return turretLevel; }
    public void setTurretLevel(int turretLevel) { this.turretLevel = turretLevel; }
    public boolean isTurretAmmoFree() { return turretAmmoFree; }
    public void setTurretAmmoFree(boolean turretAmmoFree) { this.turretAmmoFree = turretAmmoFree; }

    public Set<LandMember> getMembers() { return members; }
    public Set<Integer> getAlliedLands() { return alliedLands; }
    public Set<Integer> getEnemyLands() { return enemyLands; }
    public Set<String> getChunkKeys() { return chunkKeys; }
    public Set<Location> getTurretLocations() { return turretLocations; }

    public void addChunkKey(String world, int x, int z) {
        this.chunkKeys.add(world + ":" + x + ":" + z);
    }

    public synchronized boolean takeDamage(int damage) {
        this.health -= damage;
        return this.health <= 0;
    }
}
