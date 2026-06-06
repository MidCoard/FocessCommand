package top.focess.command;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A robust {@link CommandSender} implementation for testing purposes.
 * <p>
 * It captures all outputs and allows pre-setting inputs or using the default
 * asynchronous input mechanism provided by {@link AbstractCommandSender}.
 */
public class TestCommandSender extends AbstractCommandSender {

    private final List<String> outputs = Collections.synchronizedList(new ArrayList<>());
    private volatile String nextInput = null;

    public TestCommandSender(@NotNull CommandPermission permission) {
        super(permission);
    }

    @Override
    public void output(@NotNull String message) {
        this.outputs.add(message);
    }

    @Override
    public @NotNull CompletableFuture<String> inputAsync(long timeoutMillis) {
        if (this.nextInput != null) {
            String input = this.nextInput;
            this.nextInput = null;
            return CompletableFuture.completedFuture(input);
        }
        return super.inputAsync(timeoutMillis);
    }

    /**
     * Pre-set the next input to be returned by the next {@link #input()} or {@link #inputAsync()} call.
     * This allows testing the default blocking mechanism with pre-filled data.
     */
    public void setNextInput(String input) {
        this.nextInput = input;
    }

    /**
     * Get all output messages received by this sender.
     */
    @NotNull
    public List<String> getOutputs() {
        return new ArrayList<>(this.outputs);
    }

    /**
     * Get the last output message received by this sender, or null if none.
     */
    public String getLastOutput() {
        return this.outputs.isEmpty() ? null : this.outputs.get(this.outputs.size() - 1);
    }

    /**
     * Clear all captured outputs.
     */
    public void clearOutputs() {
        this.outputs.clear();
    }
}
