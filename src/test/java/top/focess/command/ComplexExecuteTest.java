package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ComplexExecuteTest {

    private CommandManager manager;
    private CollectingIOHandler ioHandler;
    private CommandSender owner;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        ioHandler = new CollectingIOHandler();
        owner = new TestSender(CommandPermission.OWNER);
    }

    @Test
    void testComplexQuotedDispatch() {
        manager.register(new Command("story", "A story command") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> {
                    io.output(d.get() + ": " + d.get());
                    return CommandResult.ALLOW;
                }, CommandArgument.ofString(), CommandArgument.ofString());
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        manager.dispatch(owner, "story \"Once upon\" 'a time'", ioHandler);
        assertEquals("Once upon: a time", ioHandler.lastOutput());
    }

    @Test
    void testDynamicUsageGating() {
        final AtomicBoolean visible = new AtomicBoolean(false);
        manager.register(new Command("secret", "A secret command") {
            @Override
            public void init() {
                setExecutorPermission(s -> visible.get());
                addExecutor((s, d, io) -> CommandResult.ALLOW);
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList("the secret is 42"); }
        });

        // Case 1: Hidden
        visible.set(false);
        ExecutionResult res = manager.dispatch(owner, "secret wrong-args", ioHandler);
        assertEquals(CommandResult.NONE, res.getResult());
        assertNull(ioHandler.lastOutput()); // Help message NOT printed

        // Case 2: Visible
        visible.set(true);
        res = manager.dispatch(owner, "secret wrong-args", ioHandler);
        assertEquals(CommandResult.ARGS_NOT_EXECUTED, res.getResult());
        assertEquals("the secret is 42", ioHandler.lastOutput()); // Help message printed
    }

    @Test
    void testPermissionOverlap() {
        manager.register(new Command("multi", "A multi-argument command") {
            @Override
            public void init() {
                addExecutor((s, d, io) -> { io.output("member"); return CommandResult.ALLOW; });
                addExecutor((s, d, io) -> { io.output("owner"); return CommandResult.ALLOW; }, CommandArgument.of("owner"))
                        .setPermission(CommandPermission.OWNER);
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        CommandSender member = new TestSender(CommandPermission.MEMBER);
        
        // Member can only see member executor
        manager.dispatch(member, "multi", ioHandler);
        assertEquals("member", ioHandler.lastOutput());
        
        manager.dispatch(member, "multi owner", ioHandler);
        assertEquals(CommandResult.ARGS_NOT_EXECUTED, manager.dispatch(member, "multi owner", ioHandler).getResult());

        // Owner can see both (but "multi owner" matches specific executor)
        manager.dispatch(owner, "multi owner", ioHandler);
        assertEquals("owner", ioHandler.lastOutput());
    }

    private static final class TestSender extends CommandSender {
        TestSender(CommandPermission p) { super(p); }
    }

    private static final class CollectingIOHandler extends IOHandler {
        private String last;
        @Override public String input() { return ""; }
        @Override public void output(String message) { this.last = message; }
        String lastOutput() { return last; }
    }
}
