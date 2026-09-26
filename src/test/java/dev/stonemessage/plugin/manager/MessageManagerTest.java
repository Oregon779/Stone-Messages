package dev.stonemessage.plugin.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Regression tests for the MiniMessage/format injection fix: a dynamic placeholder
// value (player name, nickname, LuckPerms prefix/suffix/rank, ...) must never be able
// to inject real formatting, color or click-events - it must always render as inert
// literal text, no matter what it contains.
//
// format(raw, placeholders, null) never touches the plugin field (the PlaceholderAPI
// branch is only taken for a non-null player), so these tests construct MessageManager
// directly, without a live Bukkit server.
class MessageManagerTest {

    private final MessageManager messageManager = new MessageManager(null);

    @Test
    void maliciousClickEventInPlaceholderValueIsNeverExecutable() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "<click:run_command:'/op Attacker'>ClickMe</click>");

        Component result = messageManager.format("Hello {player}!", placeholders);

        assertNull(result.clickEvent(), "a hostile placeholder value must never attach a real ClickEvent");
        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertTrue(plain.contains("<click:run_command:'/op Attacker'>ClickMe</click>"),
                "the malicious tag must render as literal text, not be parsed away: " + plain);
    }

    @Test
    void legacyColorCodeInPlaceholderValueStaysLiteralText() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "&4&lINJECTED");

        Component result = messageManager.format("Hello {player}!", placeholders);

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertTrue(plain.contains("&4&lINJECTED"),
                "legacy color codes inside a dynamic value must stay literal text: " + plain);
        assertNull(result.color(), "no color should have been applied from the injected value");
    }

    @Test
    void adminAuthoredMarkupStillWorksAfterTheFix() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "Steve");

        Component result = messageManager.format("&aHello {player}!", placeholders);

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertEquals("Hello Steve!", plain);
        assertEquals(NamedTextColor.GREEN, result.color());
    }

    @Test
    void nullPlaceholderValueIsRenderedAsEmptyNotAsLiteralNull() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("prefix", null);

        Component result = messageManager.format("{prefix}Steve", placeholders);

        assertEquals("Steve", PlainTextComponentSerializer.plainText().serialize(result));
    }
}
