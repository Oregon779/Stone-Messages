package dev.stonemessage.plugin;

import dev.stonemessage.plugin.command.StoneMessageCommand;
import dev.stonemessage.plugin.listener.JoinListener;
import dev.stonemessage.plugin.listener.QuitListener;
import dev.stonemessage.plugin.manager.ConfigManager;
import dev.stonemessage.plugin.manager.IntegrationManager;
import dev.stonemessage.plugin.manager.MessageManager;
import dev.stonemessage.plugin.manager.NotificationManager;
import dev.stonemessage.plugin.manager.PlaceholderManager;
import dev.stonemessage.plugin.manager.PlayerDataManager;
import dev.stonemessage.plugin.manager.UpdateChecker;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class StoneMessage extends JavaPlugin {
    private ConfigManager configManager;
    private IntegrationManager integrationManager;
    private MessageManager messageManager;
    private PlayerDataManager playerDataManager;
    private PlaceholderManager placeholderManager;
    private NotificationManager notificationManager;
    private UpdateChecker updateChecker;
    private BukkitTask playerDataFlushTask;

    @Override
    public void onEnable() {
        getLogger().info("Loading configuration...");
        configManager = new ConfigManager(this);
        configManager.load();

        getLogger().info("Loading messages (" + configManager.getLanguage() + ")...");
        messageManager = new MessageManager(this);
        messageManager.load();

        getLogger().info("Loading player data...");
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.load();

        getLogger().info("Checking for LuckPerms / PlaceholderAPI...");
        integrationManager = new IntegrationManager(this);
        integrationManager.setup();

        placeholderManager = new PlaceholderManager(this);
        notificationManager = new NotificationManager(this);
        updateChecker = new UpdateChecker(this);

        getLogger().info("Registering commands...");
        registerCommands();

        getLogger().info("Registering listeners...");
        registerListeners();

        getLogger().info("Starting update checker...");
        updateChecker.start();

        // Runs every 30s on the main thread, but flushIfDirty() is a no-op unless a
        // player joined/left since the last tick, and even then it only does cheap
        // in-memory serialization here - the actual disk write is dispatched async.
        playerDataFlushTask = getServer().getScheduler().runTaskTimer(this,
                playerDataManager::flushIfDirty, 20L * 30, 20L * 30);

        getLogger().info("Stone Message has been enabled - join/leave messages are now handled by this plugin.");
    }

    @Override
    public void onDisable() {
        if (updateChecker != null) {
            updateChecker.stop();
        }
        if (playerDataFlushTask != null) {
            playerDataFlushTask.cancel();
        }
        if (playerDataManager != null) {
            // Blocking is fine here: happens once at shutdown, not per-player.
            playerDataManager.flushNowBlocking();
        }
        getLogger().info("Stone Message has been disabled.");
    }

    public void reload() {
        configManager.reload();
        messageManager.load();
        integrationManager.setup();
        updateChecker.start();
    }

    private void registerCommands() {
        StoneMessageCommand command = new StoneMessageCommand(this);
        getCommand("stonemessage").setExecutor((CommandExecutor) command);
        getCommand("stonemessage").setTabCompleter((TabCompleter) command);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents((Listener) new JoinListener(this), (Plugin) this);
        pm.registerEvents((Listener) new QuitListener(this), (Plugin) this);
        pm.registerEvents((Listener) updateChecker, (Plugin) this);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
