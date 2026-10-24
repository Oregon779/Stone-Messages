package dev.stonemessage.plugin.manager;

import dev.stonemessage.plugin.StoneMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PlaceholderManager {
    private final StoneMessage plugin;
    // Everyone who has ever played on this server. Counted once on enable (reading the
    // server's player list touches the disk), then kept up to date on first joins.
    private int uniquePlayers;

    public PlaceholderManager(StoneMessage plugin) {
        this.plugin = plugin;
    }

    public void countUniquePlayers() {
        uniquePlayers = Bukkit.getOfflinePlayers().length;
    }

    public void countIfNewPlayer(Player player) {
        if (!player.hasPlayedBefore()) {
            uniquePlayers++;
        }
    }

    public Map<String, String> buildJoinPlaceholders(Player player) {
        Map<String, String> placeholders = buildBasePlaceholders(player);

        PlayerDataManager data = plugin.getPlayerDataManager();
        long days = data.getDaysSinceLastSeen(player.getUniqueId());
        long hours = data.getHoursSinceLastSeen(player.getUniqueId());
        long lastSeenMillis = data.getLastSeenMillis(player.getUniqueId());
        long offlineMillis = data.getMillisSinceLastSeen(player.getUniqueId());
        long firstJoinMillis = data.getOrRecordFirstJoinMillis(player.getUniqueId());

        placeholders.put("days_offline", String.valueOf(Math.max(0, days)));
        placeholders.put("hours_offline", String.valueOf(Math.max(0, hours)));
        placeholders.put("time_offline", offlineMillis < 0 ? "" : formatDuration(offlineMillis / 1000));
        placeholders.put("last_seen_date", lastSeenMillis < 0 ? "" : formatDate(lastSeenMillis));
        placeholders.put("first_join_date", formatDate(firstJoinMillis));
        // LEAVE_GAME counts every previous session, so +1 is this join (1 on a first join).
        placeholders.put("join_count", String.valueOf(player.getStatistic(Statistic.LEAVE_GAME) + 1));
        placeholders.put("unique_players", String.valueOf(uniquePlayers));

        return placeholders;
    }

    public Map<String, String> buildLeavePlaceholders(Player player) {
        Map<String, String> placeholders = buildBasePlaceholders(player);
        placeholders.put("session_time", formatDuration(plugin.getPlayerDataManager().getSessionMillis(player.getUniqueId()) / 1000));
        return placeholders;
    }

    // "2d 3h 15m", leaving out zero parts; "0m" for under a minute.
    static String formatDuration(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || sb.isEmpty()) {
            sb.append(minutes).append('m');
        }
        return sb.toString().trim();
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
        // Vanished players aren't counted - otherwise the number would give them away.
        placeholders.put("online", String.valueOf(plugin.getVanishManager().visibleOnlineCount()));
        placeholders.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        placeholders.put("ping", String.valueOf(player.getPing()));
        placeholders.put("gamemode", player.getGameMode().name());
        placeholders.put("server_name", Bukkit.getServer().getMotd());
        placeholders.put("tps", formatTps());
        placeholders.put("level", String.valueOf(player.getLevel()));
        // PLAY_ONE_MINUTE is counted in ticks despite its name.
        placeholders.put("playtime", formatDuration(player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L));
        placeholders.put("date", formatDate(System.currentTimeMillis()));

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
