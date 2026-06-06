package top.focess.command;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the permission levels for commands.
 */
public enum CommandPermission {

    /**
     * Permission granted to everyone.
     */
    EVERYONE(0),

    /**
     * Permission for administrators.
     */
    ADMINISTRATOR(10),

    /**
     * Permission for the owner/root user.
     */
    OWNER(100);

    private final int priority;

    CommandPermission(int priority) {
        this.priority = priority;
    }

    /**
     * Check if this permission level is higher or equal to another.
     *
     * @param permission the other permission
     * @return true if satisfied, false otherwise
     */
    @Contract(pure = true)
    public boolean hasPermission(@NotNull CommandPermission permission) {
        return this.priority >= permission.priority;
    }
}
