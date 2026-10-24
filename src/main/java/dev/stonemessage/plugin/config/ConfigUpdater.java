package dev.stonemessage.plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

// Keeps a server's YAML file in sync with the version bundled in the jar.
//
// The file is rebuilt in the bundled file's key order: every option the server
// already has keeps its value and its comments, every option it lacks is added with
// the bundled default value and the bundled comments, and options that only exist
// in the server's file are kept. New options therefore need no code at all - adding
// them to the bundled file is enough.
//
// Renames, moves and removals can't be detected that way, so they are handled by
// versioned Migration steps keyed on the file's "config-version".
public final class ConfigUpdater {
    public static final String VERSION_KEY = "config-version";

    private ConfigUpdater() {
    }

    // Brings a file from (toVersion - 1) to toVersion. Runs on the server's parsed
    // file before missing options are merged in.
    public record Migration(int toVersion, Consumer<ConfigurationSection> step) {
    }

    public record UpdateResult(boolean changed, int addedKeys, int migrationsApplied,
                               int fromVersion, int toVersion, boolean fileNewerThanPlugin, Path backup) {
    }

    record Merge(YamlConfiguration result, int addedKeys, int migrationsApplied,
                 int fromVersion, int toVersion, boolean fileNewerThanPlugin) {
        boolean changed() {
            return addedKeys > 0 || migrationsApplied > 0 || fromVersion != toVersion;
        }
    }

    public static UpdateResult update(JavaPlugin plugin, String resourcePath, File targetFile) throws IOException {
        return update(plugin, resourcePath, targetFile, List.of());
    }

    public static UpdateResult update(JavaPlugin plugin, String resourcePath, File targetFile,
                                      List<Migration> migrations) throws IOException {
        try (InputStream defaultStream = plugin.getResource(resourcePath)) {
            if (defaultStream == null) {
                return new UpdateResult(false, 0, 0, 0, 0, false, null);
            }
            try (Reader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                return update(reader, targetFile, migrations);
            }
        }
    }

    static UpdateResult update(Reader defaultsReader, File targetFile, List<Migration> migrations) throws IOException {
        YamlConfiguration defaults = new YamlConfiguration();
        try {
            defaults.load(defaultsReader);
        } catch (InvalidConfigurationException ex) {
            throw new IOException("bundled defaults for " + targetFile.getName() + " are not valid YAML", ex);
        }

        // YamlConfiguration.loadConfiguration() would hand back an EMPTY config for a
        // file with a YAML syntax error, and merging into that would overwrite the
        // server's whole file with defaults. A broken file is left untouched instead.
        YamlConfiguration current = new YamlConfiguration();
        if (targetFile.exists()) {
            try {
                current.load(targetFile);
            } catch (InvalidConfigurationException ex) {
                throw new IOException(targetFile.getName() + " contains invalid YAML and was left untouched - fix it and reload: "
                        + ex.getMessage(), ex);
            }
        }

        Merge merge = merge(defaults, current, migrations);
        if (!merge.changed()) {
            return new UpdateResult(false, 0, 0, merge.fromVersion(), merge.toVersion(), merge.fileNewerThanPlugin(), null);
        }

        Path backup = null;
        if (merge.fromVersion() != merge.toVersion() && targetFile.exists()) {
            backup = targetFile.toPath().resolveSibling(targetFile.getName() + ".v" + merge.fromVersion() + ".bak");
            if (!Files.exists(backup)) {
                Files.copy(targetFile.toPath(), backup);
            }
        }
        writeAtomically(targetFile.toPath(), merge.result().saveToString());
        return new UpdateResult(true, merge.addedKeys(), merge.migrationsApplied(),
                merge.fromVersion(), merge.toVersion(), merge.fileNewerThanPlugin(), backup);
    }

    static Merge merge(YamlConfiguration defaults, YamlConfiguration current, List<Migration> migrations) {
        boolean versioned = defaults.contains(VERSION_KEY, true);
        int pluginVersion = defaults.getInt(VERSION_KEY, 0);
        int fileVersion = current.getInt(VERSION_KEY, 0);

        int applied = 0;
        if (versioned && fileVersion < pluginVersion) {
            List<Migration> pending = migrations.stream()
                    .filter(m -> m.toVersion() > fileVersion && m.toVersion() <= pluginVersion)
                    .sorted(Comparator.comparingInt(Migration::toVersion))
                    .toList();
            for (Migration migration : pending) {
                migration.step().accept(current);
                applied++;
            }
        }

        YamlConfiguration result = new YamlConfiguration();
        result.options().setHeader(current.options().getHeader().isEmpty()
                ? defaults.options().getHeader() : current.options().getHeader());
        result.options().setFooter(current.options().getFooter().isEmpty()
                ? defaults.options().getFooter() : current.options().getFooter());

        int added = copyInto(defaults, current, result);

        int resultVersion = fileVersion;
        if (versioned) {
            // A file written by a newer plugin version keeps its version; it is never
            // "downgraded" and no migrations run on it.
            resultVersion = Math.max(fileVersion, pluginVersion);
            result.set(VERSION_KEY, resultVersion);
        }
        return new Merge(result, added, applied, fileVersion, resultVersion, versioned && fileVersion > pluginVersion);
    }

    private static int copyInto(ConfigurationSection defaults, ConfigurationSection current, ConfigurationSection target) {
        int added = 0;
        for (String key : defaults.getKeys(false)) {
            boolean present = current != null && current.contains(key, true);
            Object defaultValue = defaults.get(key);

            if (defaultValue instanceof ConfigurationSection defaultSection
                    && (!present || current.isConfigurationSection(key))) {
                ConfigurationSection targetSection = target.createSection(key);
                int nested = copyInto(defaultSection, present ? current.getConfigurationSection(key) : null, targetSection);
                added += present ? nested : 1;
            } else {
                target.set(key, present ? current.get(key) : defaultValue);
                if (!present) {
                    added++;
                }
            }
            copyComments(present ? current : defaults, key, target);
        }

        if (current != null) {
            for (String key : current.getKeys(false)) {
                if (!defaults.contains(key, true)) {
                    target.set(key, current.get(key));
                    copyComments(current, key, target);
                }
            }
        }
        return added;
    }

    private static void copyComments(ConfigurationSection from, String key, ConfigurationSection to) {
        to.setComments(key, from.getComments(key));
        to.setInlineComments(key, from.getInlineComments(key));
    }

    // For Migration steps: moves a value (or whole section) together with its comments.
    // Does nothing if "from" doesn't exist, and never overwrites an existing "to".
    public static void moveKey(ConfigurationSection config, String from, String to) {
        if (!config.contains(from, true) || config.contains(to, true)) {
            return;
        }
        Object value = config.get(from);
        List<String> comments = config.getComments(from);
        List<String> inlineComments = config.getInlineComments(from);
        config.set(from, null);
        config.set(to, value);
        config.setComments(to, comments);
        config.setInlineComments(to, inlineComments);
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
