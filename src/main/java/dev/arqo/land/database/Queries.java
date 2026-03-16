package dev.arqo.land.database;

public class Queries {
    public static final String CREATE_LANDS_TABLE = "CREATE TABLE IF NOT EXISTS arqoland_lands (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(32) NOT NULL UNIQUE, owner_uuid VARCHAR(36) NOT NULL, health INT NOT NULL DEFAULT 500, max_health INT NOT NULL DEFAULT 500, diamond_balance INT NOT NULL DEFAULT 0, pvp_enabled BOOLEAN NOT NULL DEFAULT FALSE, greeting_message VARCHAR(255) DEFAULT NULL, is_public BOOLEAN NOT NULL DEFAULT FALSE, spawn_world VARCHAR(32) DEFAULT NULL, spawn_x DOUBLE DEFAULT 0, spawn_y DOUBLE DEFAULT 0, spawn_z DOUBLE DEFAULT 0, spawn_yaw FLOAT DEFAULT 0, spawn_pitch FLOAT DEFAULT 0, flag_mob_spawn BOOLEAN NOT NULL DEFAULT TRUE, flag_fire_spread BOOLEAN NOT NULL DEFAULT FALSE, flag_interact BOOLEAN NOT NULL DEFAULT FALSE, flag_piston BOOLEAN NOT NULL DEFAULT FALSE, perk_haste INT NOT NULL DEFAULT 0, perk_speed INT NOT NULL DEFAULT 0, perk_strength INT NOT NULL DEFAULT 0, perk_jump INT NOT NULL DEFAULT 0, perk_crop INT NOT NULL DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, INDEX (owner_uuid));";
    public static final String CREATE_CHUNKS_TABLE = "CREATE TABLE IF NOT EXISTS arqoland_chunks (land_id INT NOT NULL, world VARCHAR(32) NOT NULL, chunk_x INT NOT NULL, chunk_z INT NOT NULL, PRIMARY KEY (world, chunk_x, chunk_z), FOREIGN KEY (land_id) REFERENCES arqoland_lands(id) ON DELETE CASCADE);";
    public static final String CREATE_MEMBERS_TABLE = "CREATE TABLE IF NOT EXISTS arqoland_members (land_id INT NOT NULL, player_uuid VARCHAR(36) NOT NULL, role VARCHAR(10) NOT NULL DEFAULT 'MEMBER', total_contributed INT NOT NULL DEFAULT 0, PRIMARY KEY (land_id, player_uuid), FOREIGN KEY (land_id) REFERENCES arqoland_lands(id) ON DELETE CASCADE, INDEX (player_uuid));";
    public static final String CREATE_ALLIANCES_TABLE = "CREATE TABLE IF NOT EXISTS arqoland_alliances (land_id INT NOT NULL, ally_land_id INT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (land_id, ally_land_id), FOREIGN KEY (land_id) REFERENCES arqoland_lands(id) ON DELETE CASCADE, FOREIGN KEY (ally_land_id) REFERENCES arqoland_lands(id) ON DELETE CASCADE);";

    // CRUD Queries
    public static final String INSERT_LAND = "INSERT INTO arqoland_lands (name, owner_uuid) VALUES (?, ?);";
    public static final String UPDATE_LAND = "UPDATE arqoland_lands SET health = ?, max_health = ?, diamond_balance = ?, pvp_enabled = ?, greeting_message = ?, is_public = ?, spawn_world = ?, spawn_x = ?, spawn_y = ?, spawn_z = ?, spawn_yaw = ?, spawn_pitch = ?, flag_mob_spawn = ?, flag_fire_spread = ?, flag_interact = ?, flag_piston = ?, perk_haste = ?, perk_speed = ?, perk_strength = ?, perk_jump = ?, perk_crop = ? WHERE id = ?;";
    public static final String DELETE_LAND = "DELETE FROM arqoland_lands WHERE id = ?;";

    public static final String INSERT_CHUNK = "INSERT INTO arqoland_chunks (land_id, world, chunk_x, chunk_z) VALUES (?, ?, ?, ?);";
    public static final String DELETE_CHUNK = "DELETE FROM arqoland_chunks WHERE world = ? AND chunk_x = ? AND chunk_z = ?;";

    public static final String INSERT_MEMBER = "INSERT INTO arqoland_members (land_id, player_uuid, role) VALUES (?, ?, ?);";
    public static final String UPDATE_MEMBER_ROLE = "UPDATE arqoland_members SET role = ? WHERE land_id = ? AND player_uuid = ?;";
    public static final String UPDATE_MEMBER_CONTRIBUTION = "UPDATE arqoland_members SET total_contributed = ? WHERE land_id = ? AND player_uuid = ?;";
    public static final String DELETE_MEMBER = "DELETE FROM arqoland_members WHERE land_id = ? AND player_uuid = ?;";

    public static final String LOAD_ALL_LANDS = "SELECT * FROM arqoland_lands;";
    public static final String LOAD_LAND_CHUNKS = "SELECT world, chunk_x, chunk_z FROM arqoland_chunks WHERE land_id = ?;";
    public static final String LOAD_LAND_MEMBERS = "SELECT player_uuid, role, total_contributed FROM arqoland_members WHERE land_id = ?;";
    public static final String LOAD_LAND_ALLIES = "SELECT ally_land_id FROM arqoland_alliances WHERE land_id = ?;";
    
    // Stats & Leaderboard
    public static final String TOP_DIAMONDS = "SELECT name, diamond_balance FROM arqoland_lands ORDER BY diamond_balance DESC LIMIT 10;";
}
