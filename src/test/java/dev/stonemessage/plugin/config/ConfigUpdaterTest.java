package dev.stonemessage.plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// No plugin/server context needed: merge() works on parsed YAML, and the file-level
// update(Reader, File, ...) takes the bundled defaults as a Reader.
class ConfigUpdaterTest {

    private static final String DEFAULTS = """
            config-version: 2

            # Language of command feedback.
            language: en

            features:
              example:
                # Turns the example feature on.
                enabled: true
                radius: 10
            """;

    private static YamlConfiguration yaml(String text) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(text);
        return config;
    }

    private static ConfigUpdater.Merge merge(String defaults, String current, List<ConfigUpdater.Migration> migrations) throws Exception {
        return ConfigUpdater.merge(yaml(defaults), yaml(current), migrations);
    }

    @Test
    void missingNestedOptionIsAddedWithDefaultValueAndComment() throws Exception {
        ConfigUpdater.Merge merge = merge(DEFAULTS, "config-version: 2\nlanguage: en\n", List.of());

        YamlConfiguration result = merge.result();
        assertTrue(result.getBoolean("features.example.enabled"));
        assertEquals(10, result.getInt("features.example.radius"));
        assertEquals(List.of("Turns the example feature on."), result.getComments("features.example.enabled"));
        assertEquals(1, merge.addedKeys(), "a whole missing section counts as one added option");
    }

    @Test
    void customizedValuesAndTheirCommentsAreNeverOverwritten() throws Exception {
        String current = """
                config-version: 2
                # my own note
                language: de
                features:
                  example:
                    enabled: false
                """;

        YamlConfiguration result = merge(DEFAULTS, current, List.of()).result();

        assertEquals("de", result.getString("language"));
        assertEquals(List.of("my own note"), result.getComments("language"));
        assertFalse(result.getBoolean("features.example.enabled"), "customized nested value must survive");
        assertEquals(10, result.getInt("features.example.radius"), "sibling that was missing must be added");
    }

    @Test
    void newOptionsAreInsertedAtTheirDefaultPositionNotAppended() throws Exception {
        String defaults = "a: 1\nb: 2\nc: 3\n";

        YamlConfiguration result = merge(defaults, "a: 1\nc: 30\n", List.of()).result();

        assertEquals(List.of("a", "b", "c"), new ArrayList<>(result.getKeys(false)));
        assertEquals(30, result.getInt("c"));
    }

    @Test
    void optionsOnlyInTheServerFileAreKept() throws Exception {
        String current = "config-version: 2\nlanguage: en\nlegacy-option: keep-me\nfeatures:\n  example:\n    custom: 5\n";

        YamlConfiguration result = merge(DEFAULTS, current, List.of()).result();

        assertEquals("keep-me", result.getString("legacy-option"));
        assertEquals(5, result.getInt("features.example.custom"));
    }

    @Test
    void anUpToDateFileIsReportedAsUnchanged() throws Exception {
        ConfigUpdater.Merge merge = merge(DEFAULTS, DEFAULTS, List.of());

        assertFalse(merge.changed());
        assertEquals(0, merge.addedKeys());
    }

    @Test
    void migrationsRunOnlyBetweenTheFileVersionAndThePluginVersion() throws Exception {
        List<String> ran = new ArrayList<>();
        List<ConfigUpdater.Migration> migrations = List.of(
                new ConfigUpdater.Migration(1, c -> ran.add("to-1")),
                new ConfigUpdater.Migration(2, c -> ran.add("to-2")),
                new ConfigUpdater.Migration(3, c -> ran.add("to-3")));

        ConfigUpdater.Merge merge = merge(DEFAULTS, "config-version: 1\n", migrations);

        assertEquals(List.of("to-2"), ran);
        assertEquals(1, merge.fromVersion());
        assertEquals(2, merge.toVersion());
        assertEquals(2, merge.result().getInt(ConfigUpdater.VERSION_KEY));
    }

    @Test
    void renamedOptionKeepsTheServersValueAndComment() throws Exception {
        String current = """
                config-version: 1
                features:
                  example:
                    # tuned for our server
                    range: 42
                """;
        List<ConfigUpdater.Migration> migrations = List.of(new ConfigUpdater.Migration(2,
                c -> ConfigUpdater.moveKey(c, "features.example.range", "features.example.radius")));

        YamlConfiguration result = merge(DEFAULTS, current, migrations).result();

        assertEquals(42, result.getInt("features.example.radius"), "the renamed option must keep the old value, not the default");
        assertEquals(List.of("tuned for our server"), result.getComments("features.example.radius"));
        assertFalse(result.contains("features.example.range"));
    }

    @Test
    void aFileFromANewerPluginVersionIsNeitherMigratedNorDowngraded() throws Exception {
        List<String> ran = new ArrayList<>();

        ConfigUpdater.Merge merge = merge(DEFAULTS, "config-version: 5\n", List.of(new ConfigUpdater.Migration(2, c -> ran.add("x"))));

        assertTrue(ran.isEmpty());
        assertTrue(merge.fileNewerThanPlugin());
        assertEquals(5, merge.result().getInt(ConfigUpdater.VERSION_KEY));
    }

    // End-to-end with the real bundled config.yml: an "old" server file without the
    // update-checker section and with a customized value.
    @Test
    void realBundledConfigUpgradesAnOldServerFile(@TempDir File dir) throws Exception {
        String bundled;
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String old = bundled.split("# Checks https://modrinth.com")[0]
                .replace("config-version: 1\n", "")
                .replaceFirst("notification: CHAT", "notification: TITLE");
        File target = new File(dir, "config.yml");
        Files.writeString(target.toPath(), old, StandardCharsets.UTF_8);

        ConfigUpdater.UpdateResult result;
        try (Reader reader = new StringReader(bundled)) {
            result = ConfigUpdater.update(reader, target, List.of());
        }

        String written = Files.readString(target.toPath());
        YamlConfiguration upgraded = YamlConfiguration.loadConfiguration(target);
        assertTrue(result.changed());
        assertEquals("TITLE", upgraded.getString("join.notification"), "customized value survives");
        assertTrue(upgraded.getBoolean("update-checker.enabled"), "missing section is added");
        assertEquals(60, upgraded.getInt("update-checker.check-interval-minutes"));
        assertTrue(written.contains("# Checks https://modrinth.com/project/stone-message"), "new section brings its comment");
        assertTrue(written.contains("# Language used for general command feedback."), "existing comments survive");
        assertTrue(written.startsWith("# ====="), "file header survives");
        assertEquals(1, upgraded.getInt(ConfigUpdater.VERSION_KEY));
        assertNotNull(result.backup(), "a version change makes a backup first");
        assertEquals(old, Files.readString(result.backup()));
        assertFalse(Files.exists(dir.toPath().resolve("config.yml.tmp")));
    }

    @Test
    void anUnchangedFileIsNotRewritten(@TempDir File dir) throws Exception {
        File target = new File(dir, "config.yml");
        String content = "config-version: 2\nlanguage: \"de\"\nfeatures:\n  example:\n    enabled: true\n    radius: 10\n";
        Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);

        ConfigUpdater.UpdateResult result = ConfigUpdater.update(new StringReader(DEFAULTS), target, List.of());

        assertFalse(result.changed());
        assertEquals(content, Files.readString(target.toPath()), "byte-identical, quoting untouched");
    }

    // YamlConfiguration.loadConfiguration() returns an EMPTY config for a broken file;
    // merging into that would have replaced the server's entire config with defaults.
    @Test
    void aFileWithInvalidYamlIsLeftUntouched(@TempDir File dir) throws Exception {
        File target = new File(dir, "config.yml");
        String broken = "language: de\njoin:\n  notification: TITLE\n   enabled: [oops\n";
        Files.writeString(target.toPath(), broken, StandardCharsets.UTF_8);

        assertThrows(java.io.IOException.class, () -> ConfigUpdater.update(new StringReader(DEFAULTS), target, List.of()));

        assertEquals(broken, Files.readString(target.toPath()));
        try (var files = Files.list(dir.toPath())) {
            assertEquals(1, files.count(), "no temp or backup file may be created either");
        }
    }

    @Test
    void unversionedFilesLikeMessagesYmlStillGetMissingKeys(@TempDir File dir) throws Exception {
        File target = new File(dir, "messages.yml");
        Files.writeString(target.toPath(), "prefix: \"x\"\n", StandardCharsets.UTF_8);

        ConfigUpdater.UpdateResult result = ConfigUpdater.update(new InputStreamReader(
                new java.io.ByteArrayInputStream("prefix: \"y\"\ngeneral:\n  reload-success: ok\n".getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8), target, List.of());

        YamlConfiguration upgraded = YamlConfiguration.loadConfiguration(target);
        assertEquals("x", upgraded.getString("prefix"));
        assertEquals("ok", upgraded.getString("general.reload-success"));
        assertFalse(upgraded.contains(ConfigUpdater.VERSION_KEY), "no version key is invented for unversioned files");
        assertEquals(null, result.backup());
    }
}
