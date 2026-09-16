package dev.stonemessage.plugin.listener;

import dev.stonemessage.plugin.StoneMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class QuitListener implements Listener {
    private final StoneMessage plugin;

    public QuitListener(StoneMessage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(Component.empty());

        Player player = event.getPlayer();
        plugin.getPlayerDataManager().recordSeenNow(player.getUniqueId());

        if (!plugin.getConfigManager().getBoolean("leave.enabled", true)) {
            return;
        }

        Map<String, String> placeholders = plugin.getPlaceholderManager().buildLeavePlaceholders(player);
        plugin.getNotificationManager().broadcastLeave(player, placeholders);
    }
}
