package dev.stonemessage.plugin.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Placeholder values (nicknames, LuckPerms prefixes/suffixes, ...) must keep their
// colors and styling, but must never be able to inject anything interactive such as
// click-to-run-command, hover or shift-click insertion.
//
// format(raw, placeholders, null) never touches the plugin field (the PlaceholderAPI
// branch is only taken for a non-null player), so these tests construct MessageManager
// directly, without a live Bukkit server.
class MessageManagerTest {

    private final MessageManager messageManager = new MessageManager(null);

    private Component format(String template, String key, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(key, value);
        placeholders.putIfAbsent("player", "Steve");
        return messageManager.format(template, placeholders);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static boolean hasInteractivity(Component node) {
        if (node.clickEvent() != null || node.hoverEvent() != null || node.insertion() != null) {
            return true;
        }
        for (Component child : node.children()) {
            if (hasInteractivity(child)) {
                return true;
            }
        }
        return false;
    }

    // Effective color of the first text node containing the fragment, honoring inheritance.
    private static TextColor colorOf(Component node, String fragment, TextColor inherited) {
        TextColor effective = node.color() != null ? node.color() : inherited;
        if (node instanceof TextComponent text && text.content().contains(fragment)) {
            return effective;
        }
        for (Component child : node.children()) {
            TextColor found = colorOf(child, fragment, effective);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Test
    void clickEventInPlaceholderValueIsNeverExecutable() {
        Component result = format("Hello {player}!", "player", "<click:run_command:'/op Attacker'>ClickMe</click>");

        assertFalse(hasInteractivity(result), "a hostile placeholder value must never attach a real ClickEvent");
        assertTrue(plain(result).contains("<click:run_command:'/op Attacker'>ClickMe</click>"),
                "the malicious tag must render as literal text: " + plain(result));
    }

    @Test
    void hoverAndInsertionInPlaceholderValueAreNeutralized() {
        Component hover = format("Hello {displayname}!", "displayname", "<hover:show_text:'x'>Hi</hover>");
        Component insert = format("Hello {displayname}!", "displayname", "<insert:/op Attacker>Hi</insert>");

        assertFalse(hasInteractivity(hover));
        assertFalse(hasInteractivity(insert));
    }

    @Test
    void clickWrappedInAnAllowedColorIsStillNeutralized() {
        Component result = format("Hello {displayname}!", "displayname", "<red><click:run_command:'/op x'>X</click></red>");

        assertFalse(hasInteractivity(result));
    }

    @Test
    void coloredLuckPermsPrefixIsRenderedInColor() {
        Component result = format("&7{prefix}{player} joined", "prefix", "&c[Admin] ");

        assertEquals("[Admin] Steve joined", plain(result));
        assertEquals(NamedTextColor.RED, colorOf(result, "[Admin]", null));
    }

    // Prefixes commonly end with a color meant for the player name; that carry-over
    // (plain string-concatenation semantics) must keep working.
    @Test
    void trailingPrefixColorStillColorsThePlayerName() {
        Component result = format("{prefix}{player} joined", "prefix", "&7[&cAdmin&7] &c");

        assertEquals("[Admin] Steve joined", plain(result));
        assertEquals(NamedTextColor.RED, colorOf(result, "Steve", null));
    }

    @Test
    void sectionSignCodesFromDisplayNameAreRendered() {
        Component result = format("{displayname}", "displayname", "§6Gold");

        assertEquals("Gold", plain(result));
        assertEquals(NamedTextColor.GOLD, colorOf(result, "Gold", null));
    }

    @Test
    void adminAuthoredMarkupStillWorks() {
        Component result = format("&aHello {player}!", "player", "Steve");

        assertEquals("Hello Steve!", plain(result));
        assertEquals(NamedTextColor.GREEN, result.color());
    }

    @Test
    void nullPlaceholderValueIsRenderedAsEmptyNotAsLiteralNull() {
        Component result = format("{prefix}Steve", "prefix", null);

        assertEquals("Steve", plain(result));
    }
}
