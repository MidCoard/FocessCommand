package top.focess.command;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTest {

    private final CollectingIOHandler ioHandler = new CollectingIOHandler();
    private final CommandSender sender = new TestSender(CommandPermission.OWNER);

    @BeforeEach
    @AfterEach
    void reset() {
        Command.unregisterAll();
    }

    @Test
    void resolvesByNameAndAliasCaseInsensitively() {
        final Command command = new EchoCommand();
        Command.register(command);

        assertSame(command, Command.get("echo"));
        assertSame(command, Command.get("ECHO"));
        assertSame(command, Command.get("e"));
        assertSame(command, Command.get("E"));
        assertNull(Command.get("missing"));
        assertTrue(command.isRegistered());
    }

    @Test
    void duplicateAliasIsRejected() {
        Command.register(new EchoCommand());
        assertThrows(CommandDuplicateException.class, () -> Command.register(new AliasClashCommand()));
    }

    @Test
    void unregisterRemovesNameAndAliases() {
        final Command command = new EchoCommand();
        Command.register(command);
        command.unregister();

        assertFalse(command.isRegistered());
        assertNull(Command.get("echo"));
        assertNull(Command.get("e"));
        assertTrue(Command.getCommands().isEmpty());
    }

    @Test
    void executesMatchingArguments() throws Exception {
        final Command command = new EchoCommand();
        Command.register(command);

        final CommandResult result = command.execute(sender, new String[]{"hello"}, ioHandler);

        assertEquals(CommandResult.ALLOW, result);
        assertEquals(Lists.newArrayList("hello"), ioHandler.outputs);
    }

    @Test
    void unmatchedArgumentsPrintUsage() throws Exception {
        final Command command = new AddCommand();
        Command.register(command);

        final CommandResult result = command.execute(sender, new String[]{"add", "not-a-number"}, ioHandler);

        assertEquals(CommandResult.ARGS_NOT_EXECUTED, result);
        assertEquals(Lists.newArrayList("add <number>"), ioHandler.outputs);
    }

    @Test
    void typedArgumentsAreParsed() throws Exception {
        final Command command = new AddCommand();
        Command.register(command);

        final CommandResult result = command.execute(sender, new String[]{"add", "5"}, ioHandler);

        assertEquals(CommandResult.ALLOW, result);
        assertEquals(Lists.newArrayList("5"), ioHandler.outputs);
    }

    @Test
    void unregisteredCommandIsRefused() throws Exception {
        final Command command = new EchoCommand();
        final CommandResult result = command.execute(sender, new String[]{"hello"}, ioHandler);
        assertEquals(CommandResult.COMMAND_REFUSED, result);
    }

    private static final class EchoCommand extends Command {
        EchoCommand() {
            super("echo", "e");
        }

        @Override
        public void init() {
            addExecutor((s, data, io) -> {
                io.output(data.get());
                return CommandResult.ALLOW;
            }, CommandArgument.ofString());
        }

        @Override
        public List<String> usage(final CommandSender sender) {
            return Lists.newArrayList("echo <message>");
        }
    }

    private static final class AliasClashCommand extends Command {
        AliasClashCommand() {
            super("other", "e");
        }

        @Override
        public void init() {
            // no executors needed for this test
        }

        @Override
        public List<String> usage(final CommandSender sender) {
            return Lists.newArrayList("other");
        }
    }

    private static final class AddCommand extends Command {
        AddCommand() {
            super("add");
        }

        @Override
        public void init() {
            addExecutor((s, data, io) -> {
                io.output(String.valueOf(data.getInt()));
                return CommandResult.ALLOW;
            }, CommandArgument.of("add"), CommandArgument.ofInt());
        }

        @Override
        public List<String> usage(final CommandSender sender) {
            return Lists.newArrayList("add <number>");
        }
    }

    private static final class TestSender extends CommandSender {
        TestSender(final CommandPermission permission) {
            super(permission);
        }
    }

    private static final class CollectingIOHandler extends IOHandler {
        private final List<String> outputs = Lists.newArrayList();

        @Override
        public void output(final String output) {
            this.outputs.add(output);
        }
    }
}
