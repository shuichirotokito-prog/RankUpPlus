package com.kazumiii.rankupplus.storage;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MySQL-backed DataStore. Uses a single guarded JDBC connection with auto-reconnect
 * rather than a full connection pool — rankup plugins generate DB traffic on join,
 * quit, rankup, and periodic saves, which is low-frequency enough that a single
 * synchronized connection is sufficient and far simpler to get right without a
 * dedicated pooling library.
 *
 * Uses MariaDB Connector/J rather than MySQL's own Connector/J, despite the
 * class name — MariaDB's connector is LGPL-licensed and explicitly designed
 * (per MariaDB's own licensing FAQ) to be freely bundled with any application
 * regardless of that application's own license, whereas MySQL's official
 * connector is GPLv2, which is a much murkier fit for bundling into a
 * separately-licensed open-source project's distributed jar. Functionally
 * equivalent for this class's purposes: MariaDB Connector/J connects to real
 * MySQL servers (not just MariaDB ones) using the same JDBC API, just with
 * "jdbc:mariadb://" instead of "jdbc:mysql://" as the URL scheme.
 */
public class MySQLDataStore implements DataStore {

    private final RankUpPlus plugin;
    private final String tableName;
    private Connection connection;

    public MySQLDataStore(RankUpPlus plugin) {
        this.plugin = plugin;
        this.tableName = plugin.getConfigManager().getMySQLTablePrefix() + "players";
    }

    @Override
    public void init() {
        try {
            connect();
            try (Statement st = connection.createStatement()) {
                st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                    "`uuid` VARCHAR(36) NOT NULL PRIMARY KEY," +
                    "`name` VARCHAR(32)," +
                    "`rank_id` VARCHAR(64) NOT NULL DEFAULT 'DEFAULT'," +
                    "`prestige` INT NOT NULL DEFAULT 0," +
                    "`playtime` BIGINT NOT NULL DEFAULT 0," +
                    "`kills` INT NOT NULL DEFAULT 0," +
                    "`deaths` INT NOT NULL DEFAULT 0," +
                    "`blocks_broken` BIGINT NOT NULL DEFAULT 0," +
                    "`blocks_placed` BIGINT NOT NULL DEFAULT 0," +
                    "`last_rankup` BIGINT NOT NULL DEFAULT 0," +
                    "`first_join` BIGINT NOT NULL DEFAULT 0," +
                    "`booster_type` VARCHAR(32)," +
                    "`booster_multiplier` DOUBLE NOT NULL DEFAULT 1.0," +
                    "`booster_expires_at` BIGINT NOT NULL DEFAULT 0" +
                    ")"
                );
            }
            plugin.getLogger().info("Connected to MySQL database (table: " + tableName + ").");
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to MySQL database: " + e.getMessage());
            plugin.getLogger().severe("Falling back is NOT automatic — fix storage.mysql settings in config.yml and reload, or switch storage.type back to YAML.");
        }
    }

    private void connect() throws SQLException {
        String host = plugin.getConfigManager().getMySQLHost();
        int port = plugin.getConfigManager().getMySQLPort();
        String database = plugin.getConfigManager().getMySQLDatabase();
        boolean useSsl = plugin.getConfigManager().isMySQLUseSSL();
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database
            + "?useSSL=" + useSsl
            + "&autoReconnect=true"
            + "&characterEncoding=utf8"
            + "&useUnicode=true";
        connection = DriverManager.getConnection(url,
            plugin.getConfigManager().getMySQLUsername(),
            plugin.getConfigManager().getMySQLPassword());
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connect();
        }
        return connection;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing MySQL connection: " + e.getMessage());
        }
        deregisterOwnDriver();
    }

    /**
     * Deregisters any JDBC driver that was loaded by this plugin's own classloader.
     * Without this, reloading/disabling the plugin can leak a classloader reference
     * inside the JVM-wide DriverManager registry.
     */
    private void deregisterOwnDriver() {
        java.util.Enumeration<java.sql.Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            java.sql.Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == this.getClass().getClassLoader()) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    plugin.getLogger().warning("Could not deregister MySQL driver: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public PlayerData load(UUID uuid, String name) {
        String sql = "SELECT * FROM `" + tableName + "` WHERE `uuid` = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromResultSet(uuid, rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load data for " + uuid + " from MySQL: " + e.getMessage());
        }
        return new PlayerData(uuid, name);
    }

    @Override
    public CompletableFuture<PlayerData> loadAsync(UUID uuid, String name) {
        CompletableFuture<PlayerData> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(load(uuid, name));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public void save(PlayerData data) {
        String sql = "INSERT INTO `" + tableName + "` " +
            "(`uuid`,`name`,`rank_id`,`prestige`,`playtime`,`kills`,`deaths`,`blocks_broken`,`blocks_placed`,`last_rankup`,`first_join`,`booster_type`,`booster_multiplier`,`booster_expires_at`) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
            "ON DUPLICATE KEY UPDATE `name`=?,`rank_id`=?,`prestige`=?,`playtime`=?,`kills`=?,`deaths`=?,`blocks_broken`=?,`blocks_placed`=?,`last_rankup`=?,`first_join`=?,`booster_type`=?,`booster_multiplier`=?,`booster_expires_at`=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, data.getUuid().toString());
            i = bindValues(ps, i, data);
            // Repeat the same values for the ON DUPLICATE KEY UPDATE half
            bindValues(ps, i, data);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save data for " + data.getUuid() + " to MySQL: " + e.getMessage());
        }
    }

    private int bindValues(PreparedStatement ps, int i, PlayerData data) throws SQLException {
        ps.setString(i++, data.getName());
        ps.setString(i++, data.getCurrentRankId());
        ps.setInt(i++, data.getPrestige());
        ps.setLong(i++, data.getTotalPlaytime());
        ps.setInt(i++, data.getPlayerKills());
        ps.setInt(i++, data.getDeaths());
        ps.setLong(i++, data.getBlocksBroken());
        ps.setLong(i++, data.getBlocksPlaced());
        ps.setLong(i++, data.getLastRankupTime());
        ps.setLong(i++, data.getFirstJoin());
        ps.setString(i++, data.getBoosterType());
        ps.setDouble(i++, data.getBoosterMultiplier());
        ps.setLong(i++, data.getBoosterExpiresAt());
        return i;
    }

    @Override
    public void saveAsync(PlayerData data) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> save(data));
    }

    @Override
    public void saveAllSync(List<PlayerData> data) {
        for (PlayerData d : data) save(d);
    }

    @Override
    public List<PlayerData> loadAll() {
        List<PlayerData> result = new ArrayList<>();
        String sql = "SELECT * FROM `" + tableName + "`";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    result.add(fromResultSet(uuid, rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load all player data from MySQL: " + e.getMessage());
        }
        return result;
    }

    @Override
    public PlayerData findByName(String name) {
        String sql = "SELECT * FROM `" + tableName + "` WHERE LOWER(`name`) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    return fromResultSet(uuid, rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to find player by name in MySQL: " + e.getMessage());
        }
        return null;
    }

    private PlayerData fromResultSet(UUID uuid, ResultSet rs) throws SQLException {
        PlayerData data = new PlayerData(uuid, rs.getString("name"));
        data.setCurrentRankId(rs.getString("rank_id"));
        data.setPrestige(rs.getInt("prestige"));
        data.setTotalPlaytime(rs.getLong("playtime"));
        data.setPlayerKills(rs.getInt("kills"));
        data.setDeaths(rs.getInt("deaths"));
        data.setBlocksBroken(rs.getLong("blocks_broken"));
        data.setBlocksPlaced(rs.getLong("blocks_placed"));
        data.setLastRankupTime(rs.getLong("last_rankup"));
        data.setFirstJoin(rs.getLong("first_join"));
        data.setBoosterType(rs.getString("booster_type"));
        data.setBoosterMultiplier(rs.getDouble("booster_multiplier"));
        data.setBoosterExpiresAt(rs.getLong("booster_expires_at"));
        return data;
    }
}
