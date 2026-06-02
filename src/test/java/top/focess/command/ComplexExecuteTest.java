package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A more involved end-to-end exercise of {@link Command#execute(CommandSender, String[], IOHandler)}
 * covering sub-command routing, named arguments, executor-level permissions, result-executor callbacks,
 * the {@link CommandResult#ARGS} / {@link CommandResult#ARGS_NOT_EXECUTED} usage paths and exception
 * capture.
 */
class ComplexExecuteTest {

    private final CollectingIOHandler ioHandler = new CollectingIOHandler();

    @BeforeEach
    @AfterEach
    void reset() {
        Command.unregisterAll();
    }

    @Test
    void routesAddSubCommandAndFiresResultExecutor() {
        final CalcCommand command = new CalcCommand();
        Command.register(command);

        final ExecutionResult result = command.execute(owner(), new String[]{"add", "2", "3"}, ioHandler);

        assertEquals(CommandResult.ALLOW, result.getResult());
        assertTrue(result.isExecuted());
        assertEquals(Lists.newArrayList("5"), ioHandler.outputs);
        assertSame(CommandResult.ALLOW, CalcCommand.LAST_ALLOW.get());
    }

    @Test
    void routesSubtractSubCommandUsingNamedArguments() {
        final CalcCommand command = new CalcCommand();
        Command.register(command);

        final ExecutionResult result = command.execute(owner(), new String[]{"sub", "10", "4"}, ioHandler);

        assertEquals(CommandResult.ALLOW, result.getResult());
        assertEquals(Lists.newArrayList("6"), ioHandler.outputs);
    }

    @Test
    void capturesExecutorExceptionMessage() {
        final CalcCommand command = new CalcCommand();
        Command.register(command);

        final ExecutionResult result = command.execute(owner(), new String[]{"div", "1", "0"}, ioHandler);

        assertEquals(CommandResult.REFUSE_EXCEPTION, result.getResult());
        assertFalse(result.isExecuted());
        assertEquals("cannot divide by zero", result.getMessage().orElse(null));
    }

    @Test
    void argsResultPrintsUsage() {
        final CalcCommand command = new CalcCommand();
        Command.register(command);

        final ExecutionResult result = command.execute(owner(), new String[]{"help"}, ioHandler);

        assertEquals(CommandResult.ARGS, result.getResult());
        assertEquals(Lists.newArrayList(String.join("\n", command.usage(owner()))), ioHandler.outputs);
    }

    @Test
    void unknownSubCommandPrintsUsage() {
        final CalcCommand command = new CalcCommand();
        Command.register(command);

        final ExecutionResult result = command.execute(owner(), new String[]{"unknown"}, ioHandler);

        assertEquals(CommandResult.ARGS_NOT_EXECUTED, result.getResult());
        assertEquals(Lists.newArrayList(String.join("\n", command.usage(owner()))), ioHandler.outputs);
    }

    @Test
    void executorPermissionGatesAdminSubCommand() {
        final CalcCommand command = new CalcCommand();
        Command.register(command);

        final ExecutionResult denied = command.execute(member(), new String[]{"admin"}, ioHandler);
        assertEquals(CommandResult.ARGS_NOT_EXECUTED, denied.getResult());
        assertEquals(Lists.newArrayList(String.join("\n", command.usage(member()))), ioHandler.outputs);

        ioHandler.outputs.clear();
        final ExecutionResult allowed = command.execute(owner(), new String[]{"admin"}, ioHandler);
        assertEquals(CommandResult.ALLOW, allowed.getResult());
        assertEquals(Lists.newArrayList("admin-ok"), ioHandler.outputs);
    }

    private static CommandSender owner() {
        return new TestSender(CommandPermission.OWNER);
    }

    private static CommandSender member() {
        return new TestSender(CommandPermission.MEMBER);
    }

    private static final class CalcCommand extends Command {

        private static final AtomicReference<CommandResult> LAST_ALLOW = new AtomicReference<>();

        CalcCommand() {
            super("calc", "c");
        }

        @Override
        public void init() {
            LAST_ALLOW.set(null);
            addExecutor((sender, data, io) -> {
                io.output(String.valueOf(data.getInt() + data.getInt()));
                return CommandResult.ALLOW;
            }, CommandArgument.of("add"), CommandArgument.ofInt(), CommandArgument.ofInt())
                    .addCommandResultExecutor(CommandResult.ALLOW, LAST_ALLOW::set);

            addExecutor((sender, data, io) -> {
                io.output(String.valueOf((int) data.get("left") - (int) data.get("right")));
                return CommandResult.ALLOW;
            }, CommandArgument.of("sub"), CommandArgument.ofInt().named("left"), CommandArgument.ofInt().named("right"));

            addExecutor((sender, data, io) -> {
                final int dividend = data.getInt();
                final int divisor = data.getInt();
                if (divisor == 0)
                    throw new ArithmeticException("cannot divide by zero");
                io.output(String.valueOf(dividend / divisor));
                return CommandResult.ALLOW;
            }, CommandArgument.of("div"), CommandArgument.ofInt(), CommandArgument.ofInt());

            addExecutor((sender, data, io) -> {
                io.output("admin-ok");
                return CommandResult.ALLOW;
            }, CommandArgument.of("admin")).setPermission(CommandPermission.OWNER);

            addExecutor((sender, data, io) -> CommandResult.ARGS, CommandArgument.of("help"));
        }

        @Override
        public @NotNull List<String> usage(final CommandSender sender) {
            return Lists.newArrayList(
                    "calc add <int> <int>",
                    "calc sub <int> <int>",
                    "calc div <int> <int>",
                    "calc admin");
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
