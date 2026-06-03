package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void dispatchReturnsNotFoundForMissingCommand() {
        final CommandManager manager = new CommandManager();
        final CommandSender sender = new CommandSender(CommandPermission.OWNER) {};
        final ExecutionResult result = manager.dispatch(sender, "unknown arg", new IOHandler() {
            @Override public String input() { return ""; }
            @Override public void output(String message) {}
        });
        assertEquals(CommandResult.COMMAND_NOT_FOUND, result.getResult());
    }

    @Test
    void dispatchHandlesQuotedArguments() {
        final CommandManager manager = new CommandManager();
        final AtomicReference<String> argReceived = new AtomicReference<>();
        manager.register(new Command("echo") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> {
                    argReceived.set(d.get());
                    return CommandResult.ALLOW;
                }, CommandArgument.ofString());
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        manager.dispatch(new CommandSender(CommandPermission.OWNER) {}, "echo \"hello world\"", new IOHandler() {
            @Override public String input() { return ""; }
            @Override public void output(String message) {}
        });
        assertEquals("hello world", argReceived.get());
    }

    @Test
    void completeSuggestsNamesCaseInsensitively() {
        final CommandManager manager = new CommandManager();
        manager.register(new NamedCommand("apple"));
        manager.register(new NamedCommand("Apply"));
        final CommandSender sender = new CommandSender(CommandPermission.OWNER) {};

        List<String> suggestions = manager.complete(sender, "app");
        assertTrue(suggestions.contains("apple"));
        assertTrue(suggestions.contains("apply"));
    }

    @Test
    void completeHandlesPartialQuotedInput() {
        final CommandManager manager = new CommandManager();
        manager.register(new Command("test") {
            @Override public void init() { addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of("sub item")); }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });
        final CommandSender sender = new CommandSender(CommandPermission.OWNER) {};

        List<String> suggestions = manager.complete(sender, "test \"sub ");
        assertTrue(suggestions.contains("sub item"));
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
        public @NotNull List<String> usage(final CommandSender sender) {
            return Lists.newArrayList();
        }
    }
}
