package dev.arqo.land.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.arqo.land.ArqoLand;
import dev.arqo.land.models.ClaimData;
import dev.arqo.land.models.LandMember;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private final ArqoLand plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ArqoLand plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setJdbcUrl("jdbc:mariadb://" + plugin.getConfig().getString("database.host") + ":" + plugin.getConfig().getInt("database.port") + "/" + plugin.getConfig().getString("database.name"));
        config.setUsername(plugin.getConfig().getString("database.username"));
        config.setPassword(plugin.getConfig().getString("database.password"));
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size"));
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
                claim.setHealth(rs.getInt("health"));
                claim.setMaxHealth(rs.getInt("max_health"));
                claim.setDiamondBalance(rs.getInt("diamond_balance"));
                claim.setPvpEnabled(rs.getBoolean("pvp_enabled"));
                claim.setGreetingMessage(rs.getString("greeting_message"));
                claim.setPublic(rs.getBoolean("is_public"));
                claim.setCreatedAt(rs.getTimestamp("created_at"));
                
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
    }

    public void saveMemberContribution(int landId, LandMember member) {
        try (Connection conn = getConnection();
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
            stmt.setInt(1, claim.getHealth());
            stmt.setInt(2, claim.getMaxHealth());
            stmt.setInt(3, claim.getDiamondBalance());
            stmt.setBoolean(4, claim.isPvpEnabled());
            stmt.setString(5, claim.getGreetingMessage());
            stmt.setBoolean(6, claim.isPublic());
            
            stmt.setString(7, claim.getSpawnWorld());
            stmt.setDouble(8, claim.getSpawnX());
            stmt.setDouble(9, claim.getSpawnY());
            stmt.setDouble(10, claim.getSpawnZ());
            stmt.setFloat(11, claim.getSpawnYaw());
            stmt.setFloat(12, claim.getSpawnPitch());

            stmt.setBoolean(13, claim.isFlagMobSpawn());
            stmt.setBoolean(14, claim.isFlagFireSpread());
            stmt.setBoolean(15, claim.isFlagInteract());
            stmt.setBoolean(16, claim.isFlagPiston());
            
            stmt.setInt(17, claim.getPerkHaste());
            stmt.setInt(18, claim.getPerkSpeed());
            stmt.setInt(19, claim.getPerkStrength());
            stmt.setInt(20, claim.getPerkJump());
            stmt.setInt(21, claim.getPerkCrop());
            stmt.setInt(22, claim.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
