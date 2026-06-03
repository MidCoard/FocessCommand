package top.focess.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPermissionTest {

    @Test
    void higherPermissionImpliesLower() {
        assertTrue(CommandPermission.OWNER.hasPermission(CommandPermission.MEMBER));
        assertTrue(CommandPermission.OWNER.hasPermission(CommandPermission.ADMINISTRATOR));
        assertTrue(CommandPermission.ADMINISTRATOR.hasPermission(CommandPermission.MEMBER));
    }

    @Test
    void lowerPermissionDoesNotImplyHigher() {
        assertFalse(CommandPermission.MEMBER.hasPermission(CommandPermission.OWNER));
        assertFalse(CommandPermission.ADMINISTRATOR.hasPermission(CommandPermission.OWNER));
    }

    @Test
    void friendInheritsOwnerPriority() {
        assertTrue(CommandPermission.FRIEND.hasPermission(CommandPermission.OWNER));
        assertTrue(CommandPermission.OWNER.hasPermission(CommandPermission.FRIEND));
    }

    @Test
    void samePermissionIsSatisfied() {
        assertTrue(CommandPermission.MEMBER.hasPermission(CommandPermission.MEMBER));
    }

    @Test
    void customPredicateGatesDynamically() {
        final java.util.concurrent.atomic.AtomicBoolean state = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.function.Predicate<CommandSender> predicate = s -> state.get();
        final CommandSender sender = new CommandSender(CommandPermission.OWNER) {};

        assertFalse(predicate.test(sender));
        state.set(true);
        assertTrue(predicate.test(sender));
    }
}
