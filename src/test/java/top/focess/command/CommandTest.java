package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandTest {

    private final TestCommandSender sender = new TestCommandSender(CommandPermission.OWNER);

    @Test
    void testCommand() {
        final Command command = new Command("hello", "a hello command") {
            @Override
            public void init() {
                this.addExecutor((s, d) -> {
                    s.output(d.get());
                    return CommandResult.ALLOW;
                }, CommandArgument.ofString());
                this.addExecutor((s, d) -> {
                    s.output(String.valueOf(d.getInt()));
                    return CommandResult.ALLOW;
                }, CommandArgument.of("add"), CommandArgument.ofInt());
            }

            @Override
            public @NotNull List<String> usage(final CommandSender sender) {
                return Lists.newArrayList("hello <string>", "hello add <number>");
            }
        };
        CommandManager manager = new CommandManager();
        manager.register(command);

        final ExecutionResult result = manager.dispatch(sender, "hello hello");
        assertEquals(CommandResult.ALLOW, result.getResult());
        assertEquals(Lists.newArrayList("hello"), sender.getOutputs());
        sender.clearOutputs();

        final ExecutionResult result1 = manager.dispatch(sender, "hello add not-a-number");
        assertEquals(CommandResult.ARGS_NOT_EXECUTED, result1.getResult());
        assertEquals(Lists.newArrayList("hello <string>\nhello add <number>"), sender.getOutputs());
        sender.clearOutputs();

        final ExecutionResult result2 = manager.dispatch(sender, "hello add 5");
        assertEquals(CommandResult.ALLOW, result2.getResult());
        assertEquals(Lists.newArrayList("5"), sender.getOutputs());
        sender.clearOutputs();
    }
}
