package dev.stonemessage.plugin.manager;

import dev.stonemessage.plugin.StoneMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PlaceholderManager {
    private final StoneMessage plugin;

    public PlaceholderManager(StoneMessage plugin) {
        this.plugin = plugin;
    }

    public Map<String, String> buildJoinPlaceholders(Player player) {
        Map<String, String> placeholders = buildBasePlaceholders(player);

        PlayerDataManager data = plugin.getPlayerDataManager();
        long days = data.getDaysSinceLastSeen(player.getUniqueId());
        long hours = data.getHoursSinceLastSeen(player.getUniqueId());
        long lastSeenMillis = data.getLastSeenMillis(player.getUniqueId());
        long firstJoinMillis = data.getOrRecordFirstJoinMillis(player.getUniqueId());

        placeholders.put("days_offline", String.valueOf(Math.max(0, days)));
        placeholders.put("hours_offline", String.valueOf(Math.max(0, hours)));
        placeholders.put("last_seen_date", lastSeenMillis < 0 ? "" : formatDate(lastSeenMillis));
        placeholders.put("first_join_date", formatDate(firstJoinMillis));

        return placeholders;
    }

    public Map<String, String> buildLeavePlaceholders(Player player) {
        return buildBasePlaceholders(player);
    }

    private Map<String, String> buildBasePlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        Location loc = player.getLocation();

        placeholders.put("player", player.getName());
        placeholders.put("displayname", player.getDisplayName());
        placeholders.put("uuid", player.getUniqueId().toString());
        placeholders.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "");
        placeholders.put("x", String.valueOf(loc.getBlockX()));
        placeholders.put("y", String.valueOf(loc.getBlockY()));
        placeholders.put("z", String.valueOf(loc.getBlockZ()));
        placeholders.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        placeholders.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        placeholders.put("ping", String.valueOf(player.getPing()));
        placeholders.put("gamemode", player.getGameMode().name());
        placeholders.put("server_name", Bukkit.getServer().getMotd());
        placeholders.put("tps", formatTps());

        // Single lookup for all three rank-related fields - see IntegrationManager.getRankData().
        IntegrationManager.RankData rank = plugin.getIntegrationManager().getRankData(player);
        placeholders.put("prefix", rank.prefix());
        placeholders.put("suffix", rank.suffix());
        placeholders.put("rank", rank.rank());

        return placeholders;
    }

    private String formatTps() {
        try {
            double[] tps = Bukkit.getTPS();
            return String.format(Locale.US, "%.2f", tps[0]);
        } catch (Throwable ex) {
            return "N/A";
        }
    }

    private String formatDate(long millis) {
        if (millis <= 0) {
            return "";
        }
        String pattern = plugin.getConfigManager().getDateFormat();
        try {
            return new SimpleDateFormat(pattern).format(new Date(millis));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid date-format '" + pattern + "' in config.yml, using default instead.");
            return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(millis));
        }
    }
}
