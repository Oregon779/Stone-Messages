package dev.stonemessage.plugin.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderManagerTest {

    @Test
    void durationsLeaveOutZeroParts() {
        assertEquals("0m", PlaceholderManager.formatDuration(0));
        assertEquals("0m", PlaceholderManager.formatDuration(59));
        assertEquals("15m", PlaceholderManager.formatDuration(15 * 60));
        assertEquals("1h", PlaceholderManager.formatDuration(3600));
        assertEquals("2h 5m", PlaceholderManager.formatDuration(2 * 3600 + 5 * 60 + 30));
        assertEquals("3d 5m", PlaceholderManager.formatDuration(3 * 86400 + 5 * 60));
        assertEquals("2d 3h 15m", PlaceholderManager.formatDuration(2 * 86400 + 3 * 3600 + 15 * 60));
    }

    // {playtime} comes from Statistic.PLAY_ONE_MINUTE, which is counted in ticks.
    @Test
    void playtimeTicksConvertToTheRightDuration() {
        long ticks = 20L * (5 * 3600 + 30 * 60);
        assertEquals("5h 30m", PlaceholderManager.formatDuration(ticks / 20L));
    }
}
