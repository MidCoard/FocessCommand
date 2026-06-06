package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandManagerTest {

    private CommandManager manager;
    private TestSender sender;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        sender = new TestSender(CommandPermission.OWNER);
    }

    @Test
    void dispatchReturnsNotFoundForMissingCommand() {
        final ExecutionResult result = manager.dispatch(sender, "unknown arg");
        assertEquals(CommandResult.COMMAND_NOT_FOUND, result.getResult());
    }

    @Test
    void dispatchHandlesQuotedArguments() {
        manager.register(new Command("echo", "Echo command") {
            @Override
            public void init() {
                addExecutor((s, d) -> {
                    s.output(d.get().toString());
                    return CommandResult.ALLOW;
                }, CommandArgument.ofString());
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        manager.dispatch(sender, "echo \"hello world\"");
        assertEquals("hello world", sender.lastOutput());
    }

    @Test
    void registerThrowsOnDuplicate() {
        Command c1 = new TestCommand("test");
        Command c2 = new TestCommand("test");
        manager.register(c1);
        assertThrows(CommandDuplicateException.class, () -> manager.register(c2));
    }

    private static class TestCommand extends Command {
        TestCommand(String name) { super(name, "desc"); }
        @Override public void init() {}
        @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
    }

    private static class TestSender extends AbstractCommandSender {
        private String last;
        TestSender(CommandPermission p) { super(p); }
        @Override public @NotNull String input() { return ""; }
        @Override public void output(@NotNull String message) { this.last = message; }
        String lastOutput() { return last; }
    }
}
