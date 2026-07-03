package com.kazumiii.rankupplus.storage;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class YamlDataStore implements DataStore {

    private final RankUpPlus plugin;
    private final File dataFolder;

    public YamlDataStore(RankUpPlus plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
    }

    @Override
    public void init() {
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    @Override
    public void close() {
        // Nothing to release for flat-file storage.
    }

    @Override
    public PlayerData load(UUID uuid, String name) {
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) return new PlayerData(uuid, name);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(uuid, yaml.getString("name", name));
        data.setCurrentRankId(yaml.getString("rank", "DEFAULT"));
        data.setPrestige(yaml.getInt("prestige", 0));
        data.setTotalPlaytime(yaml.getLong("playtime", 0));
        data.setPlayerKills(yaml.getInt("kills", 0));
        data.setDeaths(yaml.getInt("deaths", 0));
        data.setBlocksBroken(yaml.getLong("blocksBroken", 0));
        data.setBlocksPlaced(yaml.getLong("blocksPlaced", 0));
        data.setLastRankupTime(yaml.getLong("lastRankup", 0));
        data.setFirstJoin(yaml.getLong("firstJoin", System.currentTimeMillis()));
        data.setBoosterType(yaml.getString("boosterType", null));
        data.setBoosterMultiplier(yaml.getDouble("boosterMultiplier", 1.0));
        data.setBoosterExpiresAt(yaml.getLong("boosterExpiresAt", 0));
        return data;
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
        File file = new File(dataFolder, data.getUuid() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", data.getName());
        yaml.set("rank", data.getCurrentRankId());
        yaml.set("prestige", data.getPrestige());
        yaml.set("playtime", data.getTotalPlaytime());
        yaml.set("kills", data.getPlayerKills());
        yaml.set("deaths", data.getDeaths());
        yaml.set("blocksBroken", data.getBlocksBroken());
        yaml.set("blocksPlaced", data.getBlocksPlaced());
        yaml.set("lastRankup", data.getLastRankupTime());
        yaml.set("firstJoin", data.getFirstJoin());
        yaml.set("boosterType", data.getBoosterType());
        yaml.set("boosterMultiplier", data.getBoosterMultiplier());
        yaml.set("boosterExpiresAt", data.getBoosterExpiresAt());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data for " + data.getUuid() + ": " + e.getMessage());
        }
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
        File[] files = dataFolder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files == null) return result;
        for (File file : files) {
            String fileName = file.getName();
            try {
                UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                result.add(load(uuid, null));
            } catch (IllegalArgumentException ignored) {
                // Not a valid UUID-named file; skip it.
            }
        }
        return result;
    }

    @Override
    public PlayerData findByName(String name) {
        File[] files = dataFolder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files == null) return null;
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String storedName = yaml.getString("name");
            if (storedName != null && storedName.equalsIgnoreCase(name)) {
                String fileName = file.getName();
                try {
                    UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                    return load(uuid, storedName);
                } catch (IllegalArgumentException ignored) {
                    // Filename wasn't a valid UUID; skip it.
                }
            }
        }
        return null;
    }
}
