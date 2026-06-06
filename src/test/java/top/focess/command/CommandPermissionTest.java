package top.focess.command;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPermissionTest {

    @Test
    void higherPermissionImpliesLower() {
        assertTrue(CommandPermission.OWNER.hasPermission(CommandPermission.EVERYONE));
        assertTrue(CommandPermission.OWNER.hasPermission(CommandPermission.ADMINISTRATOR));
        assertTrue(CommandPermission.ADMINISTRATOR.hasPermission(CommandPermission.EVERYONE));
    }

    @Test
    void lowerPermissionDoesNotImplyHigher() {
        assertFalse(CommandPermission.EVERYONE.hasPermission(CommandPermission.OWNER));
        assertFalse(CommandPermission.ADMINISTRATOR.hasPermission(CommandPermission.OWNER));
    }

    @Test
    void samePermissionIsSatisfied() {
        assertTrue(CommandPermission.EVERYONE.hasPermission(CommandPermission.EVERYONE));
    }

    @Test
    void customPredicateGatesDynamically() {
        final AtomicBoolean state = new AtomicBoolean(false);
        final Predicate<CommandSender> predicate = s -> state.get();
        final CommandSender sender = new CommandSender() {
            @Override @NotNull public CommandPermission getPermission() { return CommandPermission.OWNER; }
            @Override public void output(@NotNull String message) {}
        };

        assertFalse(predicate.test(sender));
        state.set(true);
        assertTrue(predicate.test(sender));
    }
}
