package dev.stonemessage.plugin.manager;

import dev.stonemessage.plugin.StoneMessage;
import dev.stonemessage.plugin.config.ConfigUpdater;
import dev.stonemessage.plugin.model.MessageDisplayType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {
    private static final String RESOURCE_PATH = "config.yml";

    private final StoneMessage plugin;
    private File configFile;
    private YamlConfiguration config;

    public ConfigManager(StoneMessage plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), RESOURCE_PATH);
        if (!configFile.exists()) {
            plugin.saveResource(RESOURCE_PATH, false);
        }

        try {
            ConfigUpdater.UpdateResult result = ConfigUpdater.update(plugin, RESOURCE_PATH, configFile);
            if (result.addedKeys() > 0) {
                plugin.getLogger().info("Added " + result.addedKeys() + " new option(s) to config.yml");
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to update config.yml: " + ex.getMessage());
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        load();
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public String getLanguage() {
        return config.getString("language", "en");
    }

    public String getDateFormat() {
        return config.getString("date-format", "dd.MM.yyyy HH:mm");
    }

    public MessageDisplayType getJoinNotificationType() {
        return MessageDisplayType.fromConfig(getString("join.notification", "CHAT"), MessageDisplayType.CHAT);
    }

    public MessageDisplayType getLeaveNotificationType() {
        return MessageDisplayType.fromConfig(getString("leave.notification", "CHAT"), MessageDisplayType.CHAT);
    }
}
