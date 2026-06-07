package top.focess.command;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents an entity capable of sending and receiving command interaction data.
 * <p>
 * In the Focess Command framework, a {@link CommandSender} is the bridge between 
 * the platform's input/output systems (e.g., a Terminal, a Chat Window, or a 
 * Network Socket) and the command engine.
 * 
 * <h2>Capabilities</h2>
 * <ul>
 *   <li><b>Permission Gating:</b> Senders carry a {@link CommandPermission} that 
 *       the framework uses to determine command and executor visibility.</li>
 *   <li><b>Push Output:</b> Senders receive status and result messages via 
 *       {@link #output(String)}.</li>
 *   <li><b>Interactive Input:</b> Senders support both synchronous ({@link #input()})
 *       and asynchronous ({@link #inputAsync()}) input retrieval, allowing commands
 *        to wait for follow-up data from the user.</li>
 * </ul>
 */
public interface CommandSender {

    /**
     * Retrieves the permission level of this sender.
     * <p>
     * This is used by the framework to filter visible commands and executors.
     *
     * @return A non-null {@link CommandPermission} instance.
     */
    @NotNull
    CommandPermission getPermission();

    /**
     * Checks if this sender has the required permission.
     *
     * @param permission The permission level to check against.
     * @return {@code true} if the sender's permission is greater than or equal 
     *         to the required level.
     */
    default boolean hasPermission(@NotNull CommandPermission permission) {
        return this.getPermission().hasPermission(permission);
    }

    /**
     * Reads a line of input from the sender, blocking until data is available.
     * <p>
     * This method is a synchronous convenience wrapper around {@link #inputAsync()}. 
     * It will block the current thread until {@link #receiveInput(String)} is called 
     * or a timeout occurs.
     *
     * @return The raw input string.
     * @throws RuntimeException wrapping an {@link InputTimeoutException} if the 
     *                          wait exceeds the default timeout.
     */
    @NotNull
    default String input() {
        return this.inputAsync().join();
    }

    /**
     * Sends a message to the sender's communication channel (e.g., terminal, chat).
     *
     * @param message The string message to display.
     */
    void output(@NotNull String message);

    /**
     * Requests input asynchronously with a default timeout (10 minutes).
     *
     * @return A {@link CompletableFuture} that completes with the user's input string.
     */
    @NotNull
    default CompletableFuture<String> inputAsync() {
        return this.inputAsync(TimeUnit.MINUTES.toMillis(10));
    }

    /**
     * Requests input asynchronously with a specific timeout.
     *
     * @param timeoutMillis The maximum time to wait for input in milliseconds.
     * @return A {@link CompletableFuture} that completes with the input string, 
     *         or fails with {@link InputTimeoutException} on timeout.
     * @throws UnsupportedOperationException If the implementation does not support 
     *                                       async input.
     */
    @NotNull
    default CompletableFuture<String> inputAsync(long timeoutMillis) {
        throw new UnsupportedOperationException("Async input is not supported by this CommandSender.");
    }

    /**
     * Notifies the sender that an input event has occurred.
     * <p>
     * This method is typically called by the platform's input listener (e.g., a 
     * Console Reader or Packet Handler) to complete a pending {@link #inputAsync()} request.
     *
     * @param input The raw input string provided by the user.
     * @return {@code true} if the input was accepted by a pending request, 
     *         {@code false} otherwise.
     * @throws UnsupportedOperationException If the implementation does not support 
     *                                       async input.
     */
    default boolean receiveInput(@NotNull String input) {
        throw new UnsupportedOperationException("Async input is not supported by this CommandSender.");
    }
}
