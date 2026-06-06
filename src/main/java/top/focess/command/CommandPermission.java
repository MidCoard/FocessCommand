package top.focess.command;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a tiered hierarchy of permission levels for command access control.
 * <p>
 * In the Focess Command framework, permissions are comparative. A {@link CommandSender} 
 * is granted access if their permission level is greater than or equal to the required 
 * level (verified via {@link #hasPermission(CommandPermission)}).
 * 
 * <h3>Hierarchy</h3>
 * {@link #OWNER} (100) &gt; {@link #ADMINISTRATOR} (10) &gt; {@link #EVERYONE} (0)
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
