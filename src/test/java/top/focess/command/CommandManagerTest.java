package top.focess.command;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandManagerTest {

    @Test
    void registersAndResolvesWithinManager() {
        final CommandManager manager = new CommandManager();
        final Command command = new NamedCommand("hello", "h");
        manager.register(command);

        assertTrue(command.isRegistered());
        assertSame(command, manager.get("hello"));
        assertSame(command, manager.get("H"));
        assertNull(manager.get("missing"));
        assertSame(command, manager.getCommands().get(0));
    }

    @Test
    void managersAreIsolated() {
        final CommandManager first = new CommandManager();
        final CommandManager second = new CommandManager();
        final Command command = new NamedCommand("solo");
        first.register(command);

        assertSame(command, first.get("solo"));
        assertNull(second.get("solo"));
        // a different manager has its own namespace, so the same name can be reused
        second.register(new NamedCommand("solo"));
        assertNull(Command.get("solo"));
    }

    @Test
    void duplicateKeyIsRejectedPerManager() {
        final CommandManager manager = new CommandManager();
        manager.register(new NamedCommand("dup", "d"));
        assertThrows(CommandDuplicateException.class, () -> manager.register(new NamedCommand("other", "d")));
    }

    @Test
    void unregisterClearsManagerState() {
        final CommandManager manager = new CommandManager();
        final Command command = new NamedCommand("temp", "t");
        manager.register(command);
        command.unregister();

        assertFalse(command.isRegistered());
        assertNull(manager.get("temp"));
        assertNull(manager.get("t"));
        assertTrue(manager.getCommands().isEmpty());
    }

    private static final class NamedCommand extends Command {
        NamedCommand(final String name, final String... aliases) {
            super(name, aliases);
        }

        @Override
        public void init() {
            // no executors needed for these tests
        }

        @Override
        public List<String> usage(final CommandSender sender) {
            return Lists.newArrayList();
        }
    }
}
