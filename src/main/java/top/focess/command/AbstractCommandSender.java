package top.focess.command;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A base implementation of {@link CommandSender} that handles asynchronous input.
 */
public abstract class AbstractCommandSender implements CommandSender {

    private final CommandPermission commandPermission;
    private CompletableFuture<String> inputFuture = null;

    protected AbstractCommandSender(@NotNull CommandPermission commandPermission) {
        this.commandPermission = commandPermission;
    }

    @Override
    @NotNull
    public CommandPermission getPermission() {
        return this.commandPermission;
    }

    @Override
    public final void input(@NotNull String input) {
        if (this.inputFuture != null) {
            this.inputFuture.complete(input);
            this.inputFuture = null;
        }
    }

    @Override
    @NotNull
    public final CompletableFuture<String> inputAsync(long timeoutMillis) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        this.inputFuture = future;
        CompletableFuture.delayedExecutor(timeoutMillis, TimeUnit.MILLISECONDS).execute(() -> {
            if (this.inputFuture == future) {
                this.inputFuture = null;
                future.completeExceptionally(new InputTimeoutException());
            }
        });
        return future;
    }
}
