package dev.stonemessage.plugin.manager;

import dev.stonemessage.plugin.StoneMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerDataManager {
    private static final String FILE_NAME = "playerdata.yml";

    private final StoneMessage plugin;
    private File dataFile;
    private YamlConfiguration data;

    // Writes are batched instead of hitting disk on every single quit/first-join.
    // With 250 players all disconnecting around the same restart, that would
    // otherwise mean up to 250 blocking File I/O calls back-to-back on the main
    // thread. Now a mutation just flips this flag; the actual write happens on a
    // periodic async task (see flushIfDirty()).
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public PlayerDataManager(StoneMessage plugin) {
        this.plugin = plugin;
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), FILE_NAME);
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not create " + FILE_NAME + ": " + ex.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean isFirstJoin(UUID uuid) {
        return !data.contains(path(uuid, "first-join"));
    }

    public long getDaysSinceLastSeen(UUID uuid) {
        long elapsed = getElapsedSinceLastSeen(uuid);
        return elapsed < 0 ? -1 : TimeUnit.MILLISECONDS.toDays(elapsed);
    }

    public long getHoursSinceLastSeen(UUID uuid) {
        long elapsed = getElapsedSinceLastSeen(uuid);
        return elapsed < 0 ? -1 : TimeUnit.MILLISECONDS.toHours(elapsed);
    }

    private long getElapsedSinceLastSeen(UUID uuid) {
        if (!data.contains(path(uuid, "last-seen"))) {
            return -1;
        }
        long lastSeenMillis = data.getLong(path(uuid, "last-seen"));
        return System.currentTimeMillis() - lastSeenMillis;
    }

    public long getLastSeenMillis(UUID uuid) {
        return data.contains(path(uuid, "last-seen")) ? data.getLong(path(uuid, "last-seen")) : -1;
    }

    public long getOrRecordFirstJoinMillis(UUID uuid) {
        if (data.contains(path(uuid, "first-join"))) {
            return data.getLong(path(uuid, "first-join"));
        }
        long now = System.currentTimeMillis();
        data.set(path(uuid, "first-join"), now);
        dirty.set(true);
        return now;
    }

    public void recordSeenNow(UUID uuid) {
        data.set(path(uuid, "last-seen"), System.currentTimeMillis());
        dirty.set(true);
    }

    private String path(UUID uuid, String key) {
        return "players." + uuid + "." + key;
    }

    // Called from a periodic sync task. saveToString() only serializes the
    // in-memory map (fast, no disk syscalls), so it's safe to run on the main
    // thread; the actual file write is handed off async since that's the part
    // that can block on disk I/O.
    public void flushIfDirty() {
        if (!dirty.compareAndSet(true, false)) {
            return;
        }
        String yaml = data.saveToString();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> writeToDisk(yaml));
    }

    // Used on plugin disable, where a final blocking write is acceptable since it
    // happens once at shutdown, never per-player and never mid-game.
    public void flushNowBlocking() {
        if (!dirty.compareAndSet(true, false)) {
            return;
        }
        writeToDisk(data.saveToString());
    }

    // Writes to a temp file first, then atomically replaces the real file. A direct
    // write would truncate dataFile immediately - if the process dies mid-write (crash,
    // kill -9, disk full), the ENTIRE player database would be left corrupted/truncated
    // and silently reset to empty on next load. Writing to a temp file first means a
    // crash mid-write only loses this one unwritten batch, never the existing history.
    private void writeToDisk(String yaml) {
        File tempFile = new File(dataFile.getParentFile(), FILE_NAME + ".tmp");
        try {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                writer.write(yaml);
            }
            try {
                Files.move(tempFile.toPath(), dataFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save " + FILE_NAME + ": " + ex.getMessage());
        }
    }
}
