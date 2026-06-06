package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionAndIoTest {

    private TestSender sender;
    private CommandManager manager;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        sender = new TestSender(CommandPermission.OWNER);
    }

    @AfterEach
    void reset() {
        Command.unregisterAll();
    }

    @Test
    void executeCapturesExceptionMessageWithoutThrowing() {
        final Command command = new BoomCommand();
        manager.register(command);

        final ExecutionResult result = manager.dispatch(sender, "boom boom");

        assertEquals(CommandResult.REFUSE_EXCEPTION, result.getResult());
        assertEquals("kaboom", result.getMessage().orElse(null));
        assertFalse(result.isExecuted());
    }

    @Test
    void executeWrapsSuccess() {
        final Command command = new OkCommand();
        manager.register(command);

        final ExecutionResult result = manager.dispatch(sender, "ok ok");

        assertEquals(CommandResult.ALLOW, result.getResult());
        assertTrue(result.isExecuted());
        assertFalse(result.getMessage().isPresent());
    }

    @Test
    void inputAsyncCompletesWhenInputArrives() throws Exception {
        final CompletableFuture<String> future = sender.inputAsync();
        // input arrives later from another thread
        CompletableFuture.runAsync(() -> sender.input("answer"));

        assertEquals("answer", future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void inputAsyncTimesOut() {
        final CompletableFuture<String> future = sender.inputAsync(50);
        assertThrowsInputTimeout(future);
    }

    private static void assertThrowsInputTimeout(final CompletableFuture<String> future) {
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (final ExecutionException e) {
            assertTrue(e.getCause() instanceof InputTimeoutException);
            return;
        } catch (final InterruptedException | TimeoutException e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("expected the future to complete exceptionally");
    }

    private static final class BoomCommand extends Command {
        BoomCommand() {
            super("boom", "A command that throws an exception");
        }

        @Override
        public void init() {
            addExecutor((s, data) -> {
                throw new IllegalStateException("kaboom");
            }, CommandArgument.of("boom"));
        }

        @Override
        public @NotNull List<String> usage(final CommandSender sender) {
            return Lists.newArrayList("boom");
        }
    }

    private static final class OkCommand extends Command {
        OkCommand() {
            super("ok", "A command that completes normally");
        }

        @Override
        public void init() {
            addExecutor((s, data) -> CommandResult.ALLOW, CommandArgument.of("ok"));
        }

        @Override
        public @NotNull List<String> usage(final CommandSender sender) {
            return Lists.newArrayList("ok");
        }
    }

    private static final class TestSender extends AbstractCommandSender {
        TestSender(final CommandPermission permission) {
            super(permission);
        }
        @Override public @NotNull String input() { return ""; }
        @Override public void output(@NotNull String message) {}
    }
}
