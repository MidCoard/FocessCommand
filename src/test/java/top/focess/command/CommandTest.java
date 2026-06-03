package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
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
    void executesMatchingArguments() {
        final Command command = new EchoCommand();
        Command.register(command);

        final ExecutionResult result = command.execute(sender, new String[]{"hello"}, ioHandler);

        assertEquals(CommandResult.ALLOW, result.getResult());
        assertEquals(Lists.newArrayList("hello"), ioHandler.outputs);
    }

    @Test
    void unmatchedArgumentsPrintUsage() {
        final Command command = new AddCommand();
        Command.register(command);

        final ExecutionResult result = command.execute(sender, new String[]{"add", "not-a-number"}, ioHandler);

        assertEquals(CommandResult.ARGS_NOT_EXECUTED, result.getResult());
        assertEquals(Lists.newArrayList("add <number>"), ioHandler.outputs);
    }

    @Test
    void typedArgumentsAreParsed() {
        final Command command = new AddCommand();
        Command.register(command);

        final ExecutionResult result = command.execute(sender, new String[]{"add", "5"}, ioHandler);

        assertEquals(CommandResult.ALLOW, result.getResult());
        assertEquals(Lists.newArrayList("5"), ioHandler.outputs);
    }

    @Test
    void unregisteredCommandIsRefused() {
        final Command command = new EchoCommand();
        final ExecutionResult result = command.execute(sender, new String[]{"hello"}, ioHandler);
        assertEquals(CommandResult.COMMAND_REFUSED, result.getResult());
    }

    @Test
    void completionSuggestsCorrectly() {
        final Command command = new AddCommand();
        command.register(command); // Need registration for complete() to work

        List<String> suggestions = command.complete(sender, new String[]{"add", ""});
        assertTrue(suggestions.isEmpty()); // AddCommand uses Int converter which has no default suggestions

        final Command boolCommand = new Command("bool") {
            @Override public void init() { addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of(DataConverter.BOOLEAN_DATA_CONVERTER)); }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        boolCommand.register(boolCommand);
        
        suggestions = boolCommand.complete(sender, new String[]{""});
        assertTrue(suggestions.contains("true"));
        assertTrue(suggestions.contains("false"));
    }

    @Test
    void dynamicCompleterOverridesStaticConverter() {
        final Command cmd = new Command("invite") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> CommandResult.ALLOW, 
                    CommandArgument.ofString()
                        .completer((sender, command, arg) -> Lists.newArrayList("alice", "bob")));
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        cmd.register(cmd);

        List<String> suggestions = cmd.complete(sender, new String[]{""});
        assertTrue(suggestions.contains("alice"));
        assertTrue(suggestions.contains("bob"));
        assertEquals(2, suggestions.size());
    }

    @Test
    void multiExecutorPatternCompletion() {
        final Command patternCmd = new Command("pattern") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of("create"), CommandArgument.ofString());
                addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of("list"));
                addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of("remove"), CommandArgument.ofString());
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        patternCmd.register(patternCmd);

        // Case 1: Empty argument - should suggest all branch starts
        List<String> suggestions = patternCmd.complete(sender, new String[]{""});
        assertTrue(suggestions.contains("create"));
        assertTrue(suggestions.contains("list"));
        assertTrue(suggestions.contains("remove"));
        assertEquals(3, suggestions.size());

        // Case 2: Narrowing down
        suggestions = patternCmd.complete(sender, new String[]{"c"});
        assertTrue(suggestions.contains("create"));
        assertEquals(1, suggestions.size());

        suggestions = patternCmd.complete(sender, new String[]{"r"});
        assertTrue(suggestions.contains("remove"));
        assertEquals(1, suggestions.size());

        // Case 3: Moving to the next positional argument in a specific branch
        suggestions = patternCmd.complete(sender, new String[]{"create", ""});
        // "create" is followed by a String argument with no completer
        assertTrue(suggestions.isEmpty());
        
        // Case 4: No match for first argument
        suggestions = patternCmd.complete(sender, new String[]{"unknown", ""});
        assertTrue(suggestions.isEmpty());
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
        public @NotNull List<String> usage(final CommandSender sender) {
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
        public @NotNull List<String> usage(final CommandSender sender) {
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
        public @NotNull List<String> usage(final CommandSender sender) {
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
