package dev.stonemessage.plugin.manager;

import dev.stonemessage.plugin.StoneMessage;
import dev.stonemessage.plugin.config.ConfigUpdater;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private static final Map<Character, String> LEGACY_TAGS = new HashMap<>();
    private static final String[] BUNDLED_LANGUAGES = {"en", "de"};

    static {
        LEGACY_TAGS.put('0', "black");
        LEGACY_TAGS.put('1', "dark_blue");
        LEGACY_TAGS.put('2', "dark_green");
        LEGACY_TAGS.put('3', "dark_aqua");
        LEGACY_TAGS.put('4', "dark_red");
        LEGACY_TAGS.put('5', "dark_purple");
        LEGACY_TAGS.put('6', "gold");
        LEGACY_TAGS.put('7', "gray");
        LEGACY_TAGS.put('8', "dark_gray");
        LEGACY_TAGS.put('9', "blue");
        LEGACY_TAGS.put('a', "green");
        LEGACY_TAGS.put('b', "aqua");
        LEGACY_TAGS.put('c', "red");
        LEGACY_TAGS.put('d', "light_purple");
        LEGACY_TAGS.put('e', "yellow");
        LEGACY_TAGS.put('f', "white");
        LEGACY_TAGS.put('k', "obfuscated");
        LEGACY_TAGS.put('l', "bold");
        LEGACY_TAGS.put('m', "strikethrough");
        LEGACY_TAGS.put('n', "underlined");
        LEGACY_TAGS.put('o', "italic");
        LEGACY_TAGS.put('r', "reset");
    }

    private final StoneMessage plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, YamlConfiguration> languageCache = new HashMap<>();
    private String activeLanguage = "en";

    public MessageManager(StoneMessage plugin) {
        this.plugin = plugin;
    }

    public void load() {
        languageCache.clear();
        activeLanguage = plugin.getConfigManager().getLanguage();

        for (String lang : BUNDLED_LANGUAGES) {
            loadLanguage(lang);
        }
        if (!languageCache.containsKey(activeLanguage)) {
            loadLanguage(activeLanguage);
        }
    }

    private void loadLanguage(String lang) {
        String resourcePath = "languages/" + lang + "/messages.yml";
        File langFolder = new File(plugin.getDataFolder(), "languages/" + lang);
        File file = new File(langFolder, "messages.yml");

        if (!file.exists()) {
            langFolder.mkdirs();
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in == null) {
                    plugin.getLogger().warning("No bundled messages.yml found for language '" + lang + "'");
                    return;
                }
                Files.copy(in, file.toPath());
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not create messages.yml for '" + lang + "': " + ex.getMessage());
                return;
            }
        }

        try {
            ConfigUpdater.UpdateResult result = ConfigUpdater.update(plugin, resourcePath, file);
            if (result.addedKeys() > 0) {
                plugin.getLogger().info("Added " + result.addedKeys() + " new message key(s) to languages/" + lang + "/messages.yml");
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to update messages for '" + lang + "': " + ex.getMessage());
        }

        languageCache.put(lang, YamlConfiguration.loadConfiguration(file));
    }

    public String getRaw(String path) {
        YamlConfiguration active = languageCache.get(activeLanguage);
        String value = active != null ? active.getString(path) : null;
        if (value == null) {
            YamlConfiguration fallback = languageCache.get("en");
            value = fallback != null ? fallback.getString(path) : null;
        }
        return value != null ? value : "";
    }

    private String applyPlaceholders(String raw, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return raw;
        }
        String result = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    // Same substitution as applyPlaceholders(), but each value is escaped first so
    // dynamic, potentially player-controlled content (nicknames/{displayname}, LuckPerms
    // {prefix}/{suffix}/{rank}, ...) can never be parsed as MiniMessage markup - it always
    // renders as inert literal text, no matter what it contains.
    private String applyPlaceholdersSafely(String raw, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return raw;
        }
        String result = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue();
            String safeValue = value == null ? "" : miniMessage.escapeTags(value);
            result = result.replace("{" + entry.getKey() + "}", safeValue);
        }
        return result;
    }

    private String convertLegacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = input.length();
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < len) {
                char next = input.charAt(i + 1);
                if (next == '#' && i + 8 <= len && isHex(safeSub(input, i + 2, i + 8))) {
                    String hex = input.substring(i + 2, i + 8);
                    sb.append("<#").append(hex).append('>');
                    i += 7;
                    continue;
                }
                char lower = Character.toLowerCase(next);
                if (LEGACY_TAGS.containsKey(lower)) {
                    sb.append('<').append(LEGACY_TAGS.get(lower)).append('>');
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String safeSub(String input, int start, int end) {
        if (start < 0 || end > input.length() || start > end) {
            return "";
        }
        return input.substring(start, end);
    }

    private boolean isHex(String s) {
        if (s.length() != 6) {
            return false;
        }
        for (char c : s.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                return false;
            }
        }
        return true;
    }

    public Component format(String raw, Map<String, String> placeholders) {
        return format(raw, placeholders, null);
    }

    // Order matters: PAPI and our own &/hex legacy codes are resolved on the
    // admin-authored template first. Only THEN are our own {placeholder} values -
    // which can carry player-controlled content such as nicknames or LuckPerms
    // prefixes/suffixes - substituted in, pre-escaped via applyPlaceholdersSafely().
    // That way a hostile display name (e.g. containing "<click:run_command:...>" or
    // "&c") can never be parsed as real formatting/functionality; it always renders
    // as plain text instead.
    public Component format(String raw, Map<String, String> placeholders, Player player) {
        String withPapi = player != null ? plugin.getIntegrationManager().applyPlaceholderApi(player, raw) : raw;
        String legacyConverted = convertLegacyToMiniMessage(withPapi);
        String withPlaceholders = applyPlaceholdersSafely(legacyConverted, placeholders);
        return miniMessage.deserialize(withPlaceholders);
    }

    public String getFormattedRaw(String path, Map<String, String> placeholders) {
        return applyPlaceholders(getRaw(path), placeholders);
    }

    public void sendChat(CommandSender target, String path, Map<String, String> placeholders) {
        String prefixed = getRaw("prefix") + getRaw(path);
        target.sendMessage(format(prefixed, placeholders));
    }

    public void sendRaw(CommandSender target, String path, Map<String, String> placeholders) {
        target.sendMessage(format(getRaw(path), placeholders));
    }
}
