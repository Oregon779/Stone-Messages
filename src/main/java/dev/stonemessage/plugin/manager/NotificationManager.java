package dev.stonemessage.plugin.manager;

import dev.stonemessage.plugin.StoneMessage;
import dev.stonemessage.plugin.model.MessageDisplayType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

public class NotificationManager {
    private final StoneMessage plugin;

    public NotificationManager(StoneMessage plugin) {
        this.plugin = plugin;
    }

    public void broadcastJoin(Player subject, Map<String, String> placeholders, boolean firstJoin) {
        MessageDisplayType type = plugin.getConfigManager().getJoinNotificationType();
        boolean useFirstJoinText = firstJoin && plugin.getConfigManager().getBoolean("join.first-join.enabled", true);
        String section = useFirstJoinText ? "join.first-join" : "join";
        broadcast(section, type, placeholders, subject, plugin.getServer().getOnlinePlayers());
    }

    public void broadcastLeave(Player subject, Map<String, String> placeholders) {
        MessageDisplayType type = plugin.getConfigManager().getLeaveNotificationType();
        broadcast("leave", type, placeholders, subject, plugin.getServer().getOnlinePlayers());
    }

    // Every recipient sees the exact same text - PlaceholderAPI is resolved against
    // "subject" (the player who joined/left), not the viewer, and our own {placeholder}
    // map is already fixed for this event. So the message only needs to be built ONCE
    // here, not once per online player. At 250 players that turns 250 redundant
    // MiniMessage-parses + PAPI calls per join/leave into a single one, which is the
    // difference between a broadcast being free and it being a main-thread spike on
    // every connect/disconnect (including reconnect storms after a restart).
    private void broadcast(String section, MessageDisplayType type, Map<String, String> placeholders, Player subject, Collection<? extends Player> recipients) {
        ConfigManager cfg = plugin.getConfigManager();
        MessageManager mm = plugin.getMessageManager();

        switch (type) {
            case CHAT -> {
                Component msg = mm.format(cfg.getString(section + ".messages.chat", ""), placeholders, subject);
                for (Player recipient : recipients) {
                    recipient.sendMessage(msg);
                }
            }
            case ACTIONBAR -> {
                Component msg = mm.format(cfg.getString(section + ".messages.actionbar", ""), placeholders, subject);
                for (Player recipient : recipients) {
                    recipient.sendActionBar(msg);
                }
            }
            case TITLE -> {
                Component title = mm.format(cfg.getString(section + ".messages.title", ""), placeholders, subject);
                Component subtitle = mm.format(cfg.getString(section + ".messages.subtitle", ""), placeholders, subject);
                Title.Times times = titleTimes(cfg);
                Title packagedTitle = Title.title(title, subtitle, times);
                for (Player recipient : recipients) {
                    recipient.showTitle(packagedTitle);
                }
            }
        }
    }

    private Title.Times titleTimes(ConfigManager cfg) {
        int fadeIn = cfg.getInt("title-timing.fade-in-ticks", 5);
        int stay = cfg.getInt("title-timing.stay-ticks", 40);
        int fadeOut = cfg.getInt("title-timing.fade-out-ticks", 10);
        return Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L));
    }
}
