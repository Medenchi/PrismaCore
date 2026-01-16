package com.plasma.core.database;

import com.plasma.core.PlasmaCore;
import java.io.File;
import java.sql.*;
import java.util.UUID;

public class Database {

    private final PlasmaCore plugin;
    private Connection connection;

    public Database(PlasmaCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "plasma.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка БД: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                password TEXT,
                ip TEXT,
                registered_at INTEGER,
                last_login INTEGER,
                is_banned INTEGER DEFAULT 0,
                ban_reason TEXT,
                ban_until INTEGER,
                is_muted INTEGER DEFAULT 0,
                mute_until INTEGER,
                gender TEXT DEFAULT 'male'
            )
        """);
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS homes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL NOT NULL,
                pitch REAL NOT NULL,
                UNIQUE(uuid, name)
            )
        """);
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS coins (
                uuid TEXT PRIMARY KEY,
                balance REAL DEFAULT 0
            )
        """);
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS block_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                action TEXT NOT NULL,
                world TEXT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                block_type TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """);
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS friends (
                uuid TEXT NOT NULL,
                friend_uuid TEXT NOT NULL,
                added_at INTEGER NOT NULL,
                PRIMARY KEY (uuid, friend_uuid)
            )
        """);
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS shops (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                world TEXT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                item TEXT NOT NULL,
                amount INTEGER NOT NULL,
                price REAL NOT NULL,
                created_at INTEGER NOT NULL
            )
        """);
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS spawn (
                id INTEGER PRIMARY KEY DEFAULT 1,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL NOT NULL,
                pitch REAL NOT NULL
            )
        """);
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS sessions (
                uuid TEXT PRIMARY KEY,
                ip TEXT NOT NULL,
                expires_at INTEGER NOT NULL
            )
        """);
        
        stmt.close();
    }

    public Connection getConnection() { return connection; }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка закрытия БД: " + e.getMessage());
        }
    }

    // AUTH
    public boolean isRegistered(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM players WHERE uuid = ? AND password IS NOT NULL");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close(); ps.close();
            return exists;
        } catch (SQLException e) { return false; }
    }

    public void registerPlayer(UUID uuid, String username, String password, String ip) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO players (uuid, username, password, ip, registered_at) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, ip);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate(); ps.close();
            setBalance(uuid, plugin.getConfig().getDouble("coins.starting-balance", 100));
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка регистрации: " + e.getMessage()); }
    }

    public String getPassword(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT password FROM players WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            String password = rs.next() ? rs.getString("password") : null;
            rs.close(); ps.close();
            return password;
        } catch (SQLException e) { return null; }
    }

    public void createSession(UUID uuid, String ip, long expiresAt) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO sessions (uuid, ip, expires_at) VALUES (?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, ip);
            ps.setLong(3, expiresAt);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка сессии: " + e.getMessage()); }
    }

    public boolean hasValidSession(UUID uuid, String ip) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT expires_at FROM sessions WHERE uuid = ? AND ip = ?");
            ps.setString(1, uuid.toString());
            ps.setString(2, ip);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long expiresAt = rs.getLong("expires_at");
                rs.close(); ps.close();
                return System.currentTimeMillis() < expiresAt;
            }
            rs.close(); ps.close();
            return false;
        } catch (SQLException e) { return false; }
    }

    // COINS
    public double getBalance(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT balance FROM coins WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            double balance = rs.next() ? rs.getDouble("balance") : 0;
            rs.close(); ps.close();
            return balance;
        } catch (SQLException e) { return 0; }
    }

    public void setBalance(UUID uuid, double balance) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO coins (uuid, balance) VALUES (?, ?)");
            ps.setString(1, uuid.toString());
            ps.setDouble(2, balance);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка баланса: " + e.getMessage()); }
    }

    public void addBalance(UUID uuid, double amount) { setBalance(uuid, getBalance(uuid) + amount); }
    public void removeBalance(UUID uuid, double amount) { setBalance(uuid, Math.max(0, getBalance(uuid) - amount)); }

    // HOMES
    public void setHome(UUID uuid, String name, String world, double x, double y, double z, float yaw, float pitch) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, world);
            ps.setDouble(4, x);
            ps.setDouble(5, y);
            ps.setDouble(6, z);
            ps.setFloat(7, yaw);
            ps.setFloat(8, pitch);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка дома: " + e.getMessage()); }
    }

    public ResultSet getHome(UUID uuid, String name) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM homes WHERE uuid = ? AND name = ?");
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            return ps.executeQuery();
        } catch (SQLException e) { return null; }
    }

    public ResultSet getHomes(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM homes WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            return ps.executeQuery();
        } catch (SQLException e) { return null; }
    }

    public int getHomesCount(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) as count FROM homes WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt("count") : 0;
            rs.close(); ps.close();
            return count;
        } catch (SQLException e) { return 0; }
    }

    public void deleteHome(UUID uuid, String name) {
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM homes WHERE uuid = ? AND name = ?");
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка удаления дома: " + e.getMessage()); }
    }

    // SPAWN
    public void setSpawn(String world, double x, double y, double z, float yaw, float pitch) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO spawn (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, world);
            ps.setDouble(2, x);
            ps.setDouble(3, y);
            ps.setDouble(4, z);
            ps.setFloat(5, yaw);
            ps.setFloat(6, pitch);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка спавна: " + e.getMessage()); }
    }

    public ResultSet getSpawn() {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM spawn WHERE id = 1");
            return ps.executeQuery();
        } catch (SQLException e) { return null; }
    }

    // LOGS
    public void logBlock(UUID uuid, String action, String world, int x, int y, int z, String blockType) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO block_logs (uuid, action, world, x, y, z, block_type, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, action);
            ps.setString(3, world);
            ps.setInt(4, x);
            ps.setInt(5, y);
            ps.setInt(6, z);
            ps.setString(7, blockType);
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка лога: " + e.getMessage()); }
    }

    public ResultSet getBlockLogs(String world, int x, int y, int z) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM block_logs WHERE world = ? AND x = ? AND y = ? AND z = ? ORDER BY timestamp DESC LIMIT 10");
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            return ps.executeQuery();
        } catch (SQLException e) { return null; }
    }

    // FRIENDS
    public void addFriend(UUID uuid, UUID friendUuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO friends (uuid, friend_uuid, added_at) VALUES (?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, friendUuid.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка друга: " + e.getMessage()); }
    }

    public void removeFriend(UUID uuid, UUID friendUuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM friends WHERE uuid = ? AND friend_uuid = ?");
            ps.setString(1, uuid.toString());
            ps.setString(2, friendUuid.toString());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка удаления друга: " + e.getMessage()); }
    }

    public boolean isFriend(UUID uuid, UUID friendUuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM friends WHERE uuid = ? AND friend_uuid = ?");
            ps.setString(1, uuid.toString());
            ps.setString(2, friendUuid.toString());
            ResultSet rs = ps.executeQuery();
            boolean isFriend = rs.next();
            rs.close(); ps.close();
            return isFriend;
        } catch (SQLException e) { return false; }
    }

    public ResultSet getFriends(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT friend_uuid FROM friends WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            return ps.executeQuery();
        } catch (SQLException e) { return null; }
    }

    // BAN/MUTE
    public void banPlayer(UUID uuid, String reason, Long until) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE players SET is_banned = 1, ban_reason = ?, ban_until = ? WHERE uuid = ?");
            ps.setString(1, reason);
            ps.setObject(2, until);
            ps.setString(3, uuid.toString());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка бана: " + e.getMessage()); }
    }

    public void unbanPlayer(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE players SET is_banned = 0, ban_reason = NULL, ban_until = NULL WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка разбана: " + e.getMessage()); }
    }

    public boolean isBanned(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT is_banned, ban_until FROM players WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean banned = rs.getInt("is_banned") == 1;
                long banUntil = rs.getLong("ban_until");
                rs.close(); ps.close();
                if (banned && banUntil > 0 && System.currentTimeMillis() > banUntil) {
                    unbanPlayer(uuid);
                    return false;
                }
                return banned;
            }
            rs.close(); ps.close();
            return false;
        } catch (SQLException e) { return false; }
    }

    public String getBanReason(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT ban_reason FROM players WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            String reason = rs.next() ? rs.getString("ban_reason") : null;
            rs.close(); ps.close();
            return reason;
        } catch (SQLException e) { return null; }
    }

    public void mutePlayer(UUID uuid, Long until) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE players SET is_muted = 1, mute_until = ? WHERE uuid = ?");
            ps.setObject(1, until);
            ps.setString(2, uuid.toString());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка мута: " + e.getMessage()); }
    }

    public void unmutePlayer(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE players SET is_muted = 0, mute_until = NULL WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка размута: " + e.getMessage()); }
    }

    public boolean isMuted(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT is_muted, mute_until FROM players WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean muted = rs.getInt("is_muted") == 1;
                long muteUntil = rs.getLong("mute_until");
                rs.close(); ps.close();
                if (muted && muteUntil > 0 && System.currentTimeMillis() > muteUntil) {
                    unmutePlayer(uuid);
                    return false;
                }
                return muted;
            }
            rs.close(); ps.close();
            return false;
        } catch (SQLException e) { return false; }
    }

    // GENDER
    public void setGender(UUID uuid, String gender) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE players SET gender = ? WHERE uuid = ?");
            ps.setString(1, gender);
            ps.setString(2, uuid.toString());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { plugin.getLogger().severe("Ошибка пола: " + e.getMessage()); }
    }

    public String getGender(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT gender FROM players WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            String gender = rs.next() ? rs.getString("gender") : "male";
            rs.close(); ps.close();
            return gender;
        } catch (SQLException e) { return "male"; }
    }
}
