package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommandManagerDispatchAndCompleteTest {

    private CommandManager commandManager;
    private CommandSender sender;
    private IOHandler ioHandler;

    @BeforeEach
    public void setUp() {
        commandManager = new CommandManager();
        sender = new TestSender(CommandPermission.OWNER);
        ioHandler = new IOHandler() {
            @Override
            public String input() { return ""; }
            @Override
            public void output(String message) {}
        };
    }

    @Test
    public void testDispatch() {
        Command testCommand = new NamedCommand("test", "t") {
            @Override
            public void init() {
                this.addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of("sub"));
            }
            @Override
            public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList("test sub"); }
        };
        commandManager.register(testCommand);

        ExecutionResult result = commandManager.dispatch(sender, "test sub", ioHandler);
        assertEquals(CommandResult.ALLOW, result.getResult());

        result = commandManager.dispatch(sender, "t sub", ioHandler);
        assertEquals(CommandResult.ALLOW, result.getResult());

        result = commandManager.dispatch(sender, "unknown sub", ioHandler);
        assertEquals(CommandResult.COMMAND_REFUSED, result.getResult());
    }

    @Test
    public void testAutoCompleteCommandName() {
        commandManager.register(new NamedCommand("apple") {
            @Override public void init() {}
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });
        commandManager.register(new NamedCommand("apply") {
            @Override public void init() {}
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });
        commandManager.register(new NamedCommand("banana") {
            @Override public void init() {}
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        List<String> suggestions = commandManager.complete(sender, "app");
        assertTrue(suggestions.contains("apple"));
        assertTrue(suggestions.contains("apply"));
        assertFalse(suggestions.contains("banana"));

        suggestions = commandManager.complete(sender, "b");
        assertTrue(suggestions.contains("banana"));
    }

    @Test
    public void testAutoCompleteArguments() {
        Command testCommand = new NamedCommand("test") {
            @Override
            public void init() {
                this.addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of("sub1"), CommandArgument.ofString());
                this.addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of("sub2"));
            }
            @Override
            public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        commandManager.register(testCommand);

        List<String> suggestions = commandManager.complete(sender, "test ");
        assertTrue(suggestions.contains("sub1"));
        assertTrue(suggestions.contains("sub2"));

        suggestions = commandManager.complete(sender, "test s");
        assertTrue(suggestions.contains("sub1"));
        assertTrue(suggestions.contains("sub2"));

        suggestions = commandManager.complete(sender, "test sub1 ");
        // sub1 is followed by a String argument which has no default suggestions
        assertTrue(suggestions.isEmpty());
    }

    @Test
    public void testBooleanAutoComplete() {
        Command testCommand = new NamedCommand("test") {
            @Override
            public void init() {
                this.addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of(DataConverter.BOOLEAN_DATA_CONVERTER));
            }
            @Override
            public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        commandManager.register(testCommand);

        List<String> suggestions = commandManager.complete(sender, "test t");
        assertTrue(suggestions.contains("true"));
        assertFalse(suggestions.contains("false"));

        suggestions = commandManager.complete(sender, "test f");
        assertFalse(suggestions.contains("true"));
        assertTrue(suggestions.contains("false"));

        suggestions = commandManager.complete(sender, "test ");
        assertTrue(suggestions.contains("true"));
        assertTrue(suggestions.contains("false"));
    }

    private enum Color { RED, GREEN, BLUE }

    @Test
    public void testEnumAndChoicesAutoComplete() {
        Command testCommand = new NamedCommand("test") {
            @Override
            public void init() {
                this.addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of(DataConverter.ofEnum(Color.class)));
                this.addExecutor((s, d, io) -> CommandResult.ALLOW, CommandArgument.of(DataConverter.ofChoices("apple", "banana")));
            }
            @Override
            public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        commandManager.register(testCommand);

        // Test Enum
        List<String> suggestions = commandManager.complete(sender, "test ");
        assertTrue(suggestions.contains("red"));
        assertTrue(suggestions.contains("green"));
        assertTrue(suggestions.contains("blue"));
        assertTrue(suggestions.contains("apple"));
        assertTrue(suggestions.contains("banana"));

        suggestions = commandManager.complete(sender, "test r");
        assertTrue(suggestions.contains("red"));
        assertFalse(suggestions.contains("green"));

        // Test Choices
        suggestions = commandManager.complete(sender, "test a");
        assertTrue(suggestions.contains("apple"));
        assertFalse(suggestions.contains("banana"));
    }

    @Test
    public void testQuotedArguments() {
        Command testCommand = new NamedCommand("test") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> {
                    assertEquals("arg with spaces", d.get());
                    return CommandResult.ALLOW;
                }, CommandArgument.ofString());
            }
            @Override
            public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        commandManager.register(testCommand);

        ExecutionResult result = commandManager.dispatch(sender, "test \"arg with spaces\"", ioHandler);
        assertEquals(CommandResult.ALLOW, result.getResult());

        result = commandManager.dispatch(sender, "test 'arg with spaces'", ioHandler);
        assertEquals(CommandResult.ALLOW, result.getResult());
    }

    @Test
    public void testDoubleNullable() {
        Command testCommand = new NamedCommand("test") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> {
                    if (d.get(Double.class) == null) {
                        assertThrows(NullPointerException.class, d::getDouble);
                        io.output("null");
                    } else {
                        assertEquals(1.5, d.getDouble());
                        io.output("value");
                    }
                    return CommandResult.ALLOW;
                }, CommandArgument.ofNullable(DataConverter.DOUBLE_DATA_CONVERTER));
            }
            @Override
            public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        commandManager.register(testCommand);

        // Case 1: Provided
        ExecutionResult result = commandManager.dispatch(sender, "test 1.5", ioHandler);
        assertEquals(CommandResult.ALLOW, result.getResult());

        // Case 2: Skipped
        result = commandManager.dispatch(sender, "test", ioHandler);
        assertEquals(CommandResult.ALLOW, result.getResult());
    }

    private static class TestSender extends CommandSender {
        public TestSender(CommandPermission permission) {
            super(permission);
        }
    }

    private abstract static class NamedCommand extends Command {
        public NamedCommand(@NotNull String name, @NotNull String... aliases) {
            super(name, aliases);
        }
    }
}
