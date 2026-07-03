package com.kazumiii.rankupplus.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads/saves/reloads a single named YAML file from the plugin's data folder,
 * saving the bundled jar resource of the same name as the on-disk default the
 * first time it's needed — the same behavior JavaPlugin#saveDefaultConfig()
 * gives config.yml, generalized here for ranks.yml / scoreboard.yml /
 * tablist.yml / chat.yml.
 */
public class YamlFile {

    private final JavaPlugin plugin;
    private final String fileName;
    private final File file;
    private YamlConfiguration config;

    public YamlFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
        load();
    }

    private void load() {
        if (!file.exists()) {
            saveDefault();
        }
        config = YamlConfiguration.loadConfiguration(file);

        // Merge in any keys present in the bundled default but missing on disk
        // (e.g. after an update adds new config options), without overwriting
        // anything the server owner has already customized.
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
        }
    }

    private void saveDefault() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        InputStream in = plugin.getResource(fileName);
        if (in == null) {
            // No bundled default for this name — create an empty file so
            // load() has something to read instead of failing.
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + fileName + ": " + e.getMessage());
            }
            return;
        }
        plugin.saveResource(fileName, false);
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + fileName + ": " + e.getMessage());
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}
