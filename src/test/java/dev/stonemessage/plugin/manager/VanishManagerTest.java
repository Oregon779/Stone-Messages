package dev.stonemessage.plugin.manager;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Player and MetadataValue are interfaces, so tiny java.lang.reflect.Proxy stand-ins
// are enough - only getUniqueId() and getMetadata("vanished") are ever called here.
class VanishManagerTest {

    private VanishManager manager;

    // Mutable so a test can flip the vanish state, like a vanish plugin would.
    private static final class FakePlayer {
        final UUID uuid = UUID.randomUUID();
        boolean vanished;
        final Player proxy = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getMetadata" -> VanishManager.METADATA_KEY.equals(args[0]) ? List.of(metadata(vanished)) : List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static MetadataValue metadata(boolean value) {
        return (MetadataValue) Proxy.newProxyInstance(MetadataValue.class.getClassLoader(), new Class<?>[]{MetadataValue.class},
                (p, method, args) -> {
                    if (method.getName().equals("asBoolean")) {
                        return value;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @BeforeEach
    void setUp() throws Exception {
        manager = new VanishManager(null);
        Field enabled = VanishManager.class.getDeclaredField("enabled");
        enabled.setAccessible(true);
        enabled.set(manager, true);
    }

    @Test
    void joiningVanishedSuppressesTheJoinMessage() {
        FakePlayer admin = new FakePlayer();
        admin.vanished = true;

        assertTrue(manager.trackJoin(admin.proxy));
        assertFalse(manager.trackJoin(new FakePlayer().proxy), "a visible player still gets a join message");
    }

    @Test
    void goingIntoVanishAndBackIsDetectedOnce() {
        FakePlayer admin = new FakePlayer();
        manager.trackJoin(admin.proxy);

        assertEquals(VanishManager.Change.NONE, manager.detectChange(admin.proxy));
        admin.vanished = true;
        assertEquals(VanishManager.Change.VANISHED, manager.detectChange(admin.proxy), "-> fake leave message");
        assertEquals(VanishManager.Change.NONE, manager.detectChange(admin.proxy), "only once, not every check");
        admin.vanished = false;
        assertEquals(VanishManager.Change.REAPPEARED, manager.detectChange(admin.proxy), "-> fake join message");
    }

    @Test
    void leavingVanishedSuppressesTheLeaveMessage() {
        FakePlayer admin = new FakePlayer();
        admin.vanished = true;
        manager.trackJoin(admin.proxy);

        assertTrue(manager.trackQuit(admin.proxy));
    }

    // The vanish plugin may already have un-hidden the player during quit; what counts
    // is that they were vanished while online.
    @Test
    void leaveStaysSilentEvenIfVanishMetadataIsGoneAtQuit() {
        FakePlayer admin = new FakePlayer();
        admin.vanished = true;
        manager.trackJoin(admin.proxy);
        admin.vanished = false;

        assertTrue(manager.trackQuit(admin.proxy));
    }

    @Test
    void nothingIsSuppressedWhenVanishSupportIsDisabled() throws Exception {
        Field enabled = VanishManager.class.getDeclaredField("enabled");
        enabled.setAccessible(true);
        enabled.set(manager, false);
        FakePlayer admin = new FakePlayer();
        admin.vanished = true;

        assertFalse(manager.trackJoin(admin.proxy));
        assertFalse(manager.trackQuit(admin.proxy));
    }
}
