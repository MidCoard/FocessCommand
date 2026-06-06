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
    private TestCommandSender owner;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        owner = new TestCommandSender(CommandPermission.OWNER);
    }

    @Test
    void testComplexQuotedDispatch() {
        manager.register(new Command("story", "A story command") {
            @Override
            public void init() {
                addExecutor((s, d) -> {
                    s.output(d.get() + ": " + d.get());
                    return CommandResult.ALLOW;
                }, CommandArgument.ofString(), CommandArgument.ofString());
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        manager.dispatch(owner, "story \"Once upon\" 'a time'");
        assertEquals("Once upon: a time", owner.getLastOutput());
    }

    @Test
    void testDynamicUsageGating() {
        final AtomicBoolean visible = new AtomicBoolean(false);
        manager.register(new Command("secret", "A secret command") {
            @Override
            public void init() {
                addExecutor((s, d) -> CommandResult.ALLOW)
                        .addExecutorPermission(s -> visible.get());
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList("the secret is 42"); }
        });

        // Case 1: Hidden
        visible.set(false);
        ExecutionResult res = manager.dispatch(owner, "secret wrong-args");
        assertEquals(CommandResult.COMMAND_NOT_FOUND, res.getResult()); // Result from route engine when usage gated
        assertNull(owner.getLastOutput()); // Help message NOT printed

        // Case 2: Visible
        visible.set(true);
        res = manager.dispatch(owner, "secret wrong-args");
        assertEquals(CommandResult.ARGS_NOT_EXECUTED, res.getResult());
        assertEquals("the secret is 42", owner.getLastOutput()); // Help message printed
    }

    @Test
    void testPermissionOverlap() {
        manager.register(new Command("multi", "A multi-argument command") {
            @Override
            public void init() {
                addExecutor((s, d) -> { s.output("member"); return CommandResult.ALLOW; });
                addExecutor((s, d) -> { s.output("owner"); return CommandResult.ALLOW; }, CommandArgument.of("owner"))
                        .setPermission(CommandPermission.OWNER);
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        TestCommandSender member = new TestCommandSender(CommandPermission.EVERYONE);
        
        // Member can only see member executor
        manager.dispatch(member, "multi");
        assertEquals("member", member.getLastOutput());
        
        manager.dispatch(member, "multi owner");
        assertEquals(CommandResult.ARGS_NOT_EXECUTED, manager.dispatch(member, "multi owner").getResult());

        // Owner can see both (but "multi owner" matches specific executor)
        manager.dispatch(owner, "multi owner");
        assertEquals("owner", owner.getLastOutput());
    }
}
