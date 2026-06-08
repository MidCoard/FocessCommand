package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NullableArgumentTest {

    private CommandManager manager;
    private TestCommandSender sender;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        sender = new TestCommandSender(CommandPermission.OWNER);
    }

    @Test
    void testTwoNullableArguments_OneSpecified() {
        manager.register(new Command("test", "test nullable") {
            @Override
            public void init() {
                addExecutor((s, d) -> {
                    String arg1 = d.get("arg1");
                    String arg2 = d.get("arg2");
                    s.output("arg1=" + arg1 + ", arg2=" + arg2);

                    // Also check positional
                    d.flip(); // Reset read pointer to check again
                    String pos1 = d.get();
                    String pos2 = d.get();
                    s.output("pos1=" + pos1 + ", pos2=" + pos2);

                    return CommandResult.ALLOW;
                }, CommandArgument.ofNullable(DataConverter.DEFAULT_DATA_CONVERTER).named("arg1"),
                   CommandArgument.ofNullable(DataConverter.DEFAULT_DATA_CONVERTER).named("arg2"));
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        manager.dispatch(sender, "test hello");
        // Based on new priority: it matches arg1, skips arg2
        // Output should be: arg1=hello, arg2=null
        // And for positional: pos1=hello, pos2=null

        List<String> outputs = sender.getOutputs();
        assertTrue(outputs.stream().anyMatch(s -> s.contains("arg1=hello, arg2=null")), "Named arguments mismatch. Actual: " + outputs);
        assertTrue(outputs.stream().anyMatch(s -> s.contains("pos1=hello, pos2=null")), "Positional arguments mismatch. Actual: " + outputs);
    }

    @Test
    void testTwoExecutors_NullableOverlap() {
        manager.register(new Command("overlap", "test overlap") {
            @Override
            public void init() {
                addExecutor((s, d) -> {
                    s.output("exec1");
                    return CommandResult.ALLOW;
                }, CommandArgument.ofString(), CommandArgument.ofNullable(DataConverter.DEFAULT_DATA_CONVERTER));
                
                addExecutor((s, d) -> {
                    s.output("exec2");
                    return CommandResult.ALLOW;
                }, CommandArgument.ofString());
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        manager.dispatch(sender, "overlap hello");
        // Both match. Executor 1 comes first.
        assertEquals("exec1", sender.getLastOutput());
    }
}
