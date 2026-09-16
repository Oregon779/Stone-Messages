package dev.stonemessage.plugin.manager;

import dev.stonemessage.plugin.StoneMessage;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

public class IntegrationManager {
    private final StoneMessage plugin;
    private LuckPerms luckPerms;
    private boolean placeholderApiPresent;

    public IntegrationManager(StoneMessage plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                luckPerms = LuckPermsProvider.get();
                plugin.getLogger().info("LuckPerms found - {prefix}, {suffix} and {rank} placeholders are now live.");
            } catch (IllegalStateException ex) {
                luckPerms = null;
                plugin.getLogger().warning("LuckPerms is installed but its API isn't ready yet - {prefix}/{suffix}/{rank} will stay empty.");
            }
        } else {
            luckPerms = null;
            plugin.getLogger().info("LuckPerms not found - {prefix}, {suffix} and {rank} will resolve to an empty string.");
        }

        placeholderApiPresent = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (placeholderApiPresent) {
            plugin.getLogger().info("PlaceholderAPI found - any %placeholder% you add to your messages will also be resolved (LuckPerms, Vault, and every other expansion you have installed).");
        }
    }

    public boolean isLuckPermsPresent() {
        return luckPerms != null;
    }

    public boolean isPlaceholderApiPresent() {
        return placeholderApiPresent;
    }

    // One LuckPerms user lookup instead of three: prefix/suffix/rank used to each
    // call getUserManager().getUser() separately. That lookup is a cache read (not
    // free at 250+ concurrent joins/leaves during a reconnect storm), so this bundles
    // all three into a single fetch.
    public RankData getRankData(Player player) {
        if (luckPerms == null) {
            return RankData.EMPTY;
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return RankData.EMPTY;
        }
        CachedMetaData meta = user.getCachedData().getMetaData();
        String prefix = meta.getPrefix();
        String suffix = meta.getSuffix();
        String group = user.getPrimaryGroup();
        return new RankData(
                prefix != null ? prefix : "",
                suffix != null ? suffix : "",
                group != null ? group : ""
        );
    }

    public String applyPlaceholderApi(Player player, String text) {
        if (!placeholderApiPresent || text == null || text.isEmpty()) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public record RankData(String prefix, String suffix, String rank) {
        public static final RankData EMPTY = new RankData("", "", "");
    }
}
