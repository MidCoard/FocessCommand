package top.focess.command;

/**
 * Thrown to indicate {@link CommandSender} has waited for too long to get input String
 */
public class InputTimeoutException extends RuntimeException {
    /**
     * Constructs a InputTimeoutException
     */
    public InputTimeoutException() {
        super("CommandSender has waited for too long to get input string.");
    }
}
