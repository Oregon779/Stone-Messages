package dev.stonemessage.plugin.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// PlayerDataManager's constructor only stores the plugin reference for later use by
// load()/writeToDisk()'s error path; none of the methods under test here dereference
// it, so a real/mocked StoneMessage isn't needed. The private dataFile/data fields are
// wired directly via reflection to a temp file, bypassing load() (and any Bukkit
// server bootstrap) entirely.
class PlayerDataManagerTest {

    private PlayerDataManager manager;
    private File dataFile;

    @BeforeEach
    void setUp(@TempDir File tempDir) throws Exception {
        dataFile = new File(tempDir, "playerdata.yml");
        manager = new PlayerDataManager(null);
        setField(manager, "dataFile", dataFile);
        setField(manager, "data", new YamlConfiguration());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = PlayerDataManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private PlayerDataManager reloadFromDisk() throws Exception {
        PlayerDataManager reloaded = new PlayerDataManager(null);
        setField(reloaded, "dataFile", dataFile);
        setField(reloaded, "data", YamlConfiguration.loadConfiguration(dataFile));
        return reloaded;
    }

    @Test
    void brandNewPlayerIsReportedAsFirstJoin() {
        assertTrue(manager.isFirstJoin(UUID.randomUUID()));
    }

    @Test
    void playerIsNoLongerFirstJoinOnceRecorded() {
        UUID uuid = UUID.randomUUID();

        manager.getOrRecordFirstJoinMillis(uuid);

        assertFalse(manager.isFirstJoin(uuid));
    }

    // Regression test for the bug where isFirstJoin() was derived from the "last-seen"
    // field, which is only written on PlayerQuitEvent. A crash (kill -9, OOM) between a
    // player's first join and their next quit meant "last-seen" was never recorded, so
    // the same player would see the "first join, welcome!" message again on their next
    // connect. First-join status must depend only on having recorded a first-join
    // timestamp, never on whether a quit event ever fired.
    @Test
    void firstJoinIsNotRepeatedAfterCrashBeforeAnyQuitEventFired() {
        UUID uuid = UUID.randomUUID();

        manager.getOrRecordFirstJoinMillis(uuid); // what JoinListener does on the very first join
        // no recordSeenNow(uuid) here - simulates a crash before PlayerQuitEvent ever fires

        assertFalse(manager.isFirstJoin(uuid),
                "a player must not see the first-join message again just because the server crashed before their quit event fired");
    }

    // Regression test for the atomic-write fix: writeToDisk() used to truncate
    // playerdata.yml directly, so a crash mid-write could corrupt/wipe the whole file.
    @Test
    void firstJoinTimestampSurvivesAFlushAndReload() throws Exception {
        UUID uuid = UUID.randomUUID();
        long recorded = manager.getOrRecordFirstJoinMillis(uuid);

        manager.flushNowBlocking();

        PlayerDataManager reloaded = reloadFromDisk();
        assertFalse(reloaded.isFirstJoin(uuid));
        assertEquals(recorded, reloaded.getOrRecordFirstJoinMillis(uuid));
    }

    @Test
    void flushDoesNotLeaveATempFileBehindOnSuccess() {
        manager.getOrRecordFirstJoinMillis(UUID.randomUUID());

        manager.flushNowBlocking();

        File tempFile = new File(dataFile.getParentFile(), "playerdata.yml.tmp");
        assertFalse(tempFile.exists(), "the atomic-write temp file should be renamed away, never left behind");
    }

    @Test
    void lastSeenIsTrackedOnQuit() {
        UUID uuid = UUID.randomUUID();

        manager.recordSeenNow(uuid);

        assertEquals(0, manager.getDaysSinceLastSeen(uuid));
        assertTrue(manager.getLastSeenMillis(uuid) > 0);
    }
}
