package dev.stonemessage.plugin.manager;

import dev.stonemessage.plugin.StoneMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Detects vanished players through the "vanished" player metadata that SuperVanish,
// PremiumVanish, EssentialsX and most other vanish plugins set - so no vanish plugin
// is needed at compile time. Vanish changes while online are picked up by a cheap
// periodic check (one metadata lookup per online player).
public class VanishManager {
    static final String METADATA_KEY = "vanished";

    enum Change { NONE, VANISHED, REAPPEARED }

    private final StoneMessage plugin;
    // Online players currently known to be vanished; removed again on quit.
    private final Set<UUID> vanished = new HashSet<>();
    private boolean enabled;
    private BukkitTask task;

    public VanishManager(StoneMessage plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        ConfigManager cfg = plugin.getConfigManager();
        enabled = cfg.getBoolean("vanish.enabled", true);
        vanished.clear();
        if (!enabled) {
            return;
        }
        // Silent resync, e.g. after /stonemessage reload - no messages for players
        // who were already vanished before.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVanished(player)) {
                vanished.add(player.getUniqueId());
            }
        }
        long interval = Math.max(1, cfg.getInt("vanish.check-interval-ticks", 10));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkForChanges, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    static boolean isVanished(Player player) {
        for (MetadataValue value : player.getMetadata(METADATA_KEY)) {
            if (value.asBoolean()) {
                return true;
            }
        }
        return false;
    }

    // Returns true if the player joined vanished, i.e. no join message should be shown.
    public boolean trackJoin(Player player) {
        if (!enabled || !isVanished(player)) {
            return false;
        }
        vanished.add(player.getUniqueId());
        return true;
    }

    // Returns true if the player left while vanished, i.e. no leave message should be shown.
    public boolean trackQuit(Player player) {
        if (!enabled) {
            return false;
        }
        boolean wasVanished = vanished.remove(player.getUniqueId());
        return wasVanished || isVanished(player);
    }

    public int visibleOnlineCount() {
        return Math.max(0, Bukkit.getOnlinePlayers().size() - vanished.size());
    }

    Change detectChange(Player player) {
        boolean now = isVanished(player);
        UUID uuid = player.getUniqueId();
        if (now && vanished.add(uuid)) {
            return Change.VANISHED;
        }
        if (!now && vanished.remove(uuid)) {
            return Change.REAPPEARED;
        }
        return Change.NONE;
    }

    private void checkForChanges() {
        ConfigManager cfg = plugin.getConfigManager();
        for (Player player : Bukkit.getOnlinePlayers()) {
            switch (detectChange(player)) {
                case VANISHED -> {
                    if (cfg.getBoolean("vanish.fake-leave-on-vanish", true) && cfg.getBoolean("leave.enabled", true)) {
                        plugin.getNotificationManager().broadcastLeave(player,
                                plugin.getPlaceholderManager().buildLeavePlaceholders(player));
                    }
                }
                case REAPPEARED -> {
                    if (cfg.getBoolean("vanish.fake-join-on-unvanish", true) && cfg.getBoolean("join.enabled", true)) {
                        plugin.getNotificationManager().broadcastJoin(player,
                                plugin.getPlaceholderManager().buildJoinPlaceholders(player), false);
                    }
                }
                case NONE -> {
                }
            }
        }
    }
}
