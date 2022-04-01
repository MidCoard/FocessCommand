package top.focess.command;

/**
 * This class present an executor to execute command. We can use it to distinguish different permissions.
 */
public abstract class CommandSender {


    private final CommandPermission permission;

    public CommandSender(CommandPermission commandPermission) {
        this.permission = commandPermission;
    }

    /**
     * Indicate this CommandSender owns the permission
     *
     * @param permission the compared permission
     * @return true if the permission of this CommandSender is higher or equivalent to the compared permission, false otherwise
     */
    public boolean hasPermission(final CommandPermission permission) {
        return this.permission.hasPermission(permission);
    }

    /**
     * Send message to the sender
     * @param message the message
     */
    public abstract void sendMessage(String message);
}
