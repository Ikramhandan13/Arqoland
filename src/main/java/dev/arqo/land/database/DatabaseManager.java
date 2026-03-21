package dev.arqo.land.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.arqo.land.ArqoLand;
import dev.arqo.land.model.ClaimData;
import dev.arqo.land.model.LandMember;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private final ArqoLand plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ArqoLand plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        String type = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
        HikariConfig config = new HikariConfig();

        if (type.equals("SQLITE")) {
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/" + plugin.getConfig().getString("database.sqlite.file-name", "database.db"));
            config.setConnectionTestQuery("SELECT 1");
        } else {
            String host = plugin.getConfig().getString("database.mysql.host");
            int port = plugin.getConfig().getInt("database.mysql.port");
            String name = plugin.getConfig().getString("database.mysql.name");
            String username = plugin.getConfig().getString("database.mysql.username");
            String password = plugin.getConfig().getString("database.mysql.password");

            if (type.equals("MARIADB")) {
                config.setDriverClassName("org.mariadb.jdbc.Driver");
                config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + name);
            } else {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name);
            }
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }

        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setConnectionTimeout(10000);
        dataSource = new HikariDataSource(config);
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void initializeTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(Queries.CREATE_LANDS_TABLE);
            stmt.execute(Queries.CREATE_CHUNKS_TABLE);
            stmt.execute(Queries.CREATE_MEMBERS_TABLE);
            stmt.execute(Queries.CREATE_ALLIANCES_TABLE);
            stmt.execute(Queries.CREATE_ENEMIES_TABLE);
            stmt.execute(Queries.CREATE_TURRETS_TABLE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadAllData() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.LOAD_ALL_LANDS);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                UUID owner = UUID.fromString(rs.getString("owner_uuid"));

                ClaimData claim = new ClaimData(id, name, owner);
                claim.setDisplayName(rs.getString("display_name"));
                claim.setHealth(rs.getInt("health"));
                claim.setMaxHealth(rs.getInt("max_health"));
                claim.setDiamondBalance(rs.getInt("diamond_balance"));
                claim.setPvpEnabled(rs.getBoolean("pvp_enabled"));
                claim.setGreetingMessage(rs.getString("greeting_message"));
                claim.setPublic(rs.getBoolean("is_public"));
                claim.setCreatedAt(rs.getTimestamp("created_at"));
                claim.setLastActive(rs.getTimestamp("last_active"));
                
                String sWorld = rs.getString("spawn_world");
                if (sWorld != null) {
                    claim.setSpawnData(sWorld, rs.getDouble("spawn_x"), rs.getDouble("spawn_y"), rs.getDouble("spawn_z"), rs.getFloat("spawn_yaw"), rs.getFloat("spawn_pitch"));
                }

                claim.setFlagMobSpawn(rs.getBoolean("flag_mob_spawn"));
                claim.setFlagFireSpread(rs.getBoolean("flag_fire_spread"));
                claim.setFlagInteract(rs.getBoolean("flag_interact"));
                claim.setFlagPiston(rs.getBoolean("flag_piston"));
                
                claim.setPerkHaste(rs.getInt("perk_haste"));
                claim.setPerkSpeed(rs.getInt("perk_speed"));
                claim.setPerkStrength(rs.getInt("perk_strength"));
                claim.setPerkJump(rs.getInt("perk_jump"));
                claim.setPerkCrop(rs.getInt("perk_crop"));

                loadLandDetails(conn, claim);
                plugin.getChunkManager().loadClaimToCache(claim);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadLandDetails(Connection conn, ClaimData claim) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(Queries.LOAD_LAND_CHUNKS)) {
            stmt.setInt(1, claim.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    claim.addChunkKey(rs.getString("world"), rs.getInt("chunk_x"), rs.getInt("chunk_z"));
                }
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement(Queries.LOAD_LAND_MEMBERS)) {
            stmt.setInt(1, claim.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    claim.getMembers().add(new LandMember(
                        UUID.fromString(rs.getString("player_uuid")), 
                        rs.getString("role"),
                        rs.getInt("total_contributed")
                    ));
                }
            }
        }
        // Baru: Load Turret
        try (PreparedStatement stmt = conn.prepareStatement(Queries.LOAD_LAND_TURRETS)) {
            stmt.setInt(1, claim.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(rs.getString("world"));
                    if (world != null) {
                        claim.getTurretLocations().add(new org.bukkit.Location(world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
                    }
                }
            }
        }
        // Baru: Load Enemy
        try (PreparedStatement stmt = conn.prepareStatement(Queries.LOAD_LAND_ENEMIES)) {
            stmt.setInt(1, claim.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    claim.getEnemyLands().add(rs.getInt("enemy_land_id"));
                }
            }
        }
    }

    public int createLand(String name, UUID owner, String displayName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.INSERT_LAND, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, owner.toString());
            stmt.setString(3, displayName);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void insertChunk(int landId, String world, int x, int z) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.INSERT_CHUNK)) {
            stmt.setInt(1, landId);
            stmt.setString(2, world);
            stmt.setInt(3, x);
            stmt.setInt(4, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteChunk(String world, int x, int z) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.DELETE_CHUNK)) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertTurret(int landId, org.bukkit.Location loc) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.INSERT_TURRET)) {
            stmt.setInt(1, landId);
            stmt.setString(2, loc.getWorld().getName());
            stmt.setInt(3, loc.getBlockX());
            stmt.setInt(4, loc.getBlockY());
            stmt.setInt(5, loc.getBlockZ());
            stmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteTurret(org.bukkit.Location loc) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.DELETE_TURRET)) {
            stmt.setString(1, loc.getWorld().getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());
            stmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveMember(int landId, LandMember member) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.INSERT_MEMBER)) {
            stmt.setInt(1, landId);
            stmt.setString(2, member.getUuid().toString());
            stmt.setString(3, member.getRole());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeMember(int landId, UUID playerUuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.DELETE_MEMBER)) {
            stmt.setInt(1, landId);
            stmt.setString(2, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateMemberRole(int landId, LandMember member) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.UPDATE_MEMBER_ROLE)) {
            stmt.setString(1, member.getRole());
            stmt.setInt(2, landId);
            stmt.setString(3, member.getUuid().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveMemberContribution(int landId, LandMember member) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.UPDATE_MEMBER_CONTRIBUTION)) {
            stmt.setInt(1, member.getTotalContributed());
            stmt.setInt(2, landId);
            stmt.setString(3, member.getUuid().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void saveLand(ClaimData claim) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.UPDATE_LAND)) {
            stmt.setString(1, claim.getName());
            stmt.setInt(2, claim.getHealth());
            stmt.setInt(3, claim.getMaxHealth());
            stmt.setInt(4, claim.getDiamondBalance());
            stmt.setBoolean(5, claim.isPvpEnabled());
            stmt.setString(6, claim.getGreetingMessage());
            stmt.setBoolean(7, claim.isPublic());
            
            stmt.setString(8, claim.getSpawnWorld());
            stmt.setDouble(9, claim.getSpawnX());
            stmt.setDouble(10, claim.getSpawnY());
            stmt.setDouble(11, claim.getSpawnZ());
            stmt.setFloat(12, claim.getSpawnYaw());
            stmt.setFloat(13, claim.getSpawnPitch());

            stmt.setBoolean(14, claim.isFlagMobSpawn());
            stmt.setBoolean(15, claim.isFlagFireSpread());
            stmt.setBoolean(16, claim.isFlagInteract());
            stmt.setBoolean(17, claim.isFlagPiston());
            
            stmt.setInt(18, claim.getPerkHaste());
            stmt.setInt(19, claim.getPerkSpeed());
            stmt.setInt(20, claim.getPerkStrength());
            stmt.setInt(21, claim.getPerkJump());
            stmt.setInt(22, claim.getPerkCrop());
            stmt.setString(23, claim.getDisplayName());
            stmt.setInt(24, claim.getTurretLevel());
            stmt.setBoolean(25, claim.isTurretAmmoFree());
            stmt.setInt(26, claim.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAlly(int landId, int allyLandId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.INSERT_ALLY)) {
            stmt.setInt(1, landId);
            stmt.setInt(2, allyLandId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeAlly(int landId, int allyLandId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.DELETE_ALLY)) {
            stmt.setInt(1, landId);
            stmt.setInt(2, allyLandId);
            stmt.setInt(3, allyLandId);
            stmt.setInt(4, landId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveEnemy(int landId, int enemyLandId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.INSERT_ENEMY)) {
            stmt.setInt(1, landId);
            stmt.setInt(2, enemyLandId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeEnemy(int landId, int enemyLandId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.DELETE_ENEMY)) {
            stmt.setInt(1, landId);
            stmt.setInt(2, enemyLandId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteLand(int id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.DELETE_LAND)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteInactiveLands(int days) {
            long threshold = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
            Timestamp timestamp = new Timestamp(threshold);
            try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.DELETE_INACTIVE_LANDS)) {
            stmt.setTimestamp(1, timestamp);
            stmt.executeUpdate();
            } catch (SQLException e) {
            e.printStackTrace();
            }
            }

}
