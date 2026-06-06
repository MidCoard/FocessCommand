package top.focess.command;

import java.util.concurrent.TimeoutException;

/**
 * Thrown to indicate {@link CommandSender} has waited for too long to get input String
 */
public class InputTimeoutException extends TimeoutException {
    /**
     * Constructs a InputTimeoutException
     */
    public InputTimeoutException() {
        super("CommandSender has waited for too long to get input string.");
    }
}
