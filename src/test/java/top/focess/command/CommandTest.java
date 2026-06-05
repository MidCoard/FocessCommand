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

        List<CommandCompletion> suggestions = command.complete(sender, new String[]{"add", ""});
        assertTrue(suggestions.isEmpty()); // AddCommand uses Int converter which has no default suggestions

        final Command boolCommand = new Command("bool", "A boolean command") {
            @Override public void init() { addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of(DataConverter.BOOLEAN_DATA_CONVERTER)); }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        boolCommand.register(boolCommand);
        
        suggestions = boolCommand.complete(sender, new String[]{""});
        List<String> candidates = suggestions.stream().map(CommandCompletion::candidate).toList();
        assertTrue(candidates.contains("true"));
        assertTrue(candidates.contains("false"));
    }

    @Test
    void dynamicCompleterOverridesStaticConverter() {
        final Command cmd = new Command("invite", "Invites a user") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> CommandResult.ALLOW, 
                    CommandArgument.ofString()
                        .completer((sender, command, arg) -> Lists.newArrayList(CommandCompletion.of("alice"), CommandCompletion.of("bob"))));
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        cmd.register(cmd);

        List<CommandCompletion> suggestions = cmd.complete(sender, new String[]{""});
        List<String> candidates = suggestions.stream().map(CommandCompletion::candidate).toList();
        assertTrue(candidates.contains("alice"));
        assertTrue(candidates.contains("bob"));
        assertEquals(2, suggestions.size());
    }

    @Test
    void literalArgumentWithDescriptionCompletion() {
        final Command cmd = new Command("test", "A test command") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> CommandResult.ALLOW, 
                    CommandArgument.of("sub").description("A sub-command"));
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        cmd.register(cmd);

        List<CommandCompletion> suggestions = cmd.complete(sender, new String[]{""});
        assertEquals(1, suggestions.size());
        assertEquals("sub", suggestions.get(0).candidate());
        assertEquals("A sub-command", suggestions.get(0).description());
    }

    private static final class EchoCommand extends Command {
        EchoCommand() {
            super("echo", "Echoes the input message", "e");
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
            super("other", "Another command", "e");
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
            super("add", "Adds a number");
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
