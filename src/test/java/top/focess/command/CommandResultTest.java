package top.focess.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandResultTest {

    @Test
    void allContainsEveryConcreteResult() {
        assertTrue(CommandResult.ALL.contains(CommandResult.ALLOW));
        assertTrue(CommandResult.ALL.contains(CommandResult.REFUSE));
        assertTrue(CommandResult.ALL.contains(CommandResult.COMMAND_REFUSED));
        assertTrue(CommandResult.ALL.contains(CommandResult.ARGS_NOT_EXECUTED));
        assertTrue(CommandResult.ALL.contains(CommandResult.ARGS));
        assertTrue(CommandResult.ALL.contains(CommandResult.REFUSE_EXCEPTION));
        assertTrue(CommandResult.ALL.contains(CommandResult.COMMAND_NOT_FOUND));
    }

    @Test
    void executedFlagMatchesExecutedSet() {
        assertTrue(CommandResult.ALLOW.isExecuted());
        assertTrue(CommandResult.REFUSE.isExecuted());
        assertTrue(CommandResult.ARGS.isExecuted());
        assertFalse(CommandResult.COMMAND_REFUSED.isExecuted());
        assertFalse(CommandResult.REFUSE_EXCEPTION.isExecuted());
        assertFalse(CommandResult.COMMAND_NOT_FOUND.isExecuted());
        assertFalse(CommandResult.NONE.isExecuted());
    }

    @Test
    void negativeContainsException() {
        assertTrue(CommandResult.NEGATIVE.contains(CommandResult.REFUSE_EXCEPTION));
        assertTrue(CommandResult.NEGATIVE.contains(CommandResult.COMMAND_NOT_FOUND));
        assertFalse(CommandResult.NEGATIVE.contains(CommandResult.ALLOW));
    }

    @Test
    void noneIsEmpty() {
        assertFalse(CommandResult.NONE.contains(CommandResult.ALLOW));
    }
}
