package dev.stonemessage.plugin.listener;

import dev.stonemessage.plugin.StoneMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;

public class JoinListener implements Listener {
    private final StoneMessage plugin;

    public JoinListener(StoneMessage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(Component.empty());

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean firstJoin = plugin.getPlayerDataManager().isFirstJoin(uuid);
        plugin.getPlayerDataManager().startSession(uuid);
        plugin.getPlaceholderManager().countIfNewPlayer(player);
        boolean joinedVanished = plugin.getVanishManager().trackJoin(player);

        Map<String, String> placeholders = plugin.getPlaceholderManager().buildJoinPlaceholders(player);
        if (plugin.getConfigManager().getBoolean("join.enabled", true) && !joinedVanished) {
            plugin.getNotificationManager().broadcastJoin(player, placeholders, firstJoin);
        }
        // Only the joining player sees this, so it's sent even when they joined vanished.
        plugin.getNotificationManager().sendPrivateJoin(player, placeholders, firstJoin);
    }
}
