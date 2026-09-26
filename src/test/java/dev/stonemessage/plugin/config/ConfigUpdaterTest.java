package dev.stonemessage.plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Exercises ConfigUpdater.mergeSection() directly against plain YamlConfiguration
// objects - no plugin/server context needed, since the merge logic itself is pure.
class ConfigUpdaterTest {

    @Test
    void missingTopLevelKeyIsAddedAsAWholeNestedSection() {
        YamlConfiguration current = new YamlConfiguration();
        current.set("existing.value", "user-customized");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("existing.value", "default-value");
        defaults.set("brandnew.sub.a", 1);
        defaults.set("brandnew.sub.b", "two");

        int added = ConfigUpdater.mergeSection(defaults, current);

        assertEquals(1, added);
        assertEquals(1, current.getInt("brandnew.sub.a"));
        assertEquals("two", current.getString("brandnew.sub.b"));
    }

    @Test
    void existingCustomizedValueIsNeverOverwritten() {
        YamlConfiguration current = new YamlConfiguration();
        current.set("join.notification", "TITLE");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("join.notification", "CHAT");
        defaults.set("join.enabled", true);

        ConfigUpdater.mergeSection(defaults, current);

        assertEquals("TITLE", current.getString("join.notification"), "existing customized value must survive");
        assertTrue(current.getBoolean("join.enabled"), "a new key alongside it must still be added");
    }

    @Test
    void mergedSectionSurvivesASaveAndReloadRoundTrip() throws Exception {
        YamlConfiguration current = new YamlConfiguration();
        current.set("existing.value", "user-customized");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("existing.value", "default-value");
        defaults.set("brandnew.flag", true);

        ConfigUpdater.mergeSection(defaults, current);
        String yaml = current.saveToString();

        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(yaml);

        assertEquals("user-customized", reloaded.getString("existing.value"));
        assertTrue(reloaded.getBoolean("brandnew.flag"));
    }

    @Test
    void secondMergeOnAnAlreadyCurrentConfigAddsNothing() {
        YamlConfiguration current = new YamlConfiguration();
        current.set("language", "en");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("language", "en");

        int added = ConfigUpdater.mergeSection(defaults, current);

        assertEquals(0, added);
    }
}
