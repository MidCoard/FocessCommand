package top.focess.command;

import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * A partial implementation of {@link CommandSender} providing standard asynchronous 
 * input management.
 * <p>
 * Subclasses extending this class only need to implement the {@link #output(String)} 
 * method to have a fully functional command sender with interactive input support.
 */
public abstract class AbstractCommandSender implements CommandSender {

    private final CommandPermission commandPermission;
    private final Queue<CompletableFuture<String>> inputFutures = new ConcurrentLinkedQueue<>();

    /**
     * Constructs an {@code AbstractCommandSender} with a specific permission level.
     *
     * @param commandPermission The permission level of the sender.
     */
    protected AbstractCommandSender(@NotNull CommandPermission commandPermission) {
        this.commandPermission = commandPermission;
    }

    @Override
    @NotNull
    public CommandPermission getPermission() {
        return this.commandPermission;
    }

    /**
     * Completes the oldest pending asynchronous input request with the provided string.
     * <p>
     * <b>Implementation Note:</b> This implementation polls the internal queue for the 
     * first incomplete future and completes it.
     *
     * @param input The raw input string to provide.
     */
    @Override
    public final void receiveInput(@NotNull String input) {
        CompletableFuture<String> future;
        while ((future = this.inputFutures.poll()) != null) {
            if (future.complete(input))
                break;
        }
    }

    /**
     * Initiates an asynchronous wait for input, adding the request to a FIFO queue.
     * <p>
     * <b>Implementation Note:</b> This implementation creates a new {@link CompletableFuture}, 
     * adds it to the {@code inputFutures} queue, and schedules a timeout task. If the 
     * future times out, it is automatically removed from the queue.
     *
     * @param timeoutMillis The maximum time to wait in milliseconds.
     * @return A future that will complete with the next user input.
     */
    @Override
    @NotNull
    public CompletableFuture<String> inputAsync(long timeoutMillis) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        this.inputFutures.add(future);
        CompletableFuture.delayedExecutor(timeoutMillis, TimeUnit.MILLISECONDS).execute(() -> {
            if (this.inputFutures.remove(future)) {
                future.completeExceptionally(new InputTimeoutException());
            }
        });
        return future;
    }
}
