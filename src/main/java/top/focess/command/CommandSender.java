package top.focess.command;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represent an executor that has certain permissions and can interact with the command system.
 * <p>
 * This interface unifies the previous CommandSender and IOHandler, providing both
 * permission checks and I/O capabilities (input/output).
 */
public interface CommandSender {

    /**
     * Get the permission level of this sender.
     *
     * @return the command permission
     */
    @NotNull
    CommandPermission getPermission();

    /**
     * Indicate this CommandSender owns the permission
     *
     * @param permission the compared permission
     * @return true if the permission of this CommandSender is higher or equivalent to the compared permission, false otherwise
     */
    default boolean hasPermission(@NotNull CommandPermission permission) {
        return this.getPermission().hasPermission(permission);
    }

    /**
     * Read an input string from the sender.
     * <p>
     * This method may block until input is provided via {@link #receiveInput(String)}.
     *
     * @return the input string
     */
    @NotNull
    default String input() {
        return this.inputAsync().join();
    }

    /**
     * Send an output message to the sender.
     *
     * @param message the message to send
     */
    void output(@NotNull String message);

    /**
     * Wait for input asynchronously with a default timeout of 10 minutes.
     *
     * @return a future that completes with the input
     */
    @NotNull
    default CompletableFuture<String> inputAsync() {
        return this.inputAsync(TimeUnit.MINUTES.toMillis(10));
    }

    /**
     * Wait for input asynchronously with a specific timeout.
     *
     * @param timeoutMillis the timeout in milliseconds
     * @return a future that completes with the input or fails with InputTimeoutException
     */
    @NotNull
    default CompletableFuture<String> inputAsync(long timeoutMillis) {
        throw new UnsupportedOperationException("Async input is not supported by this CommandSender.");
    }

    /**
     * Provide input to this sender, potentially completing an async wait.
     *
     * @param input the input string
     */
    default void receiveInput(@NotNull String input) {
        throw new UnsupportedOperationException("Async input is not supported by this CommandSender.");
    }
}
