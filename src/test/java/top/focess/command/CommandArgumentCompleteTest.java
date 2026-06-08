package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandArgumentCompleteTest {

    private CommandManager manager;
    private TestCommandSender sender;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        sender = new TestCommandSender(CommandPermission.OWNER);
    }

    @Test
    void testArchitecturalCompletionFlow() {
        // 1. Setup command with executors and arguments
        Command command = new Command("party", "desc") {
            @Override
            public void init() {
                addExecutor((s, d) -> CommandResult.ALLOW, CommandArgument.of("create"), CommandArgument.ofString().named("name"));
                addExecutor((s, d) -> CommandResult.ALLOW, CommandArgument.of("list"));
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        manager.register(command);

        // 2. User types "party " and requests completion
        String input = "party ";
        CommandRoute route = manager.route(sender, input);
        
        // 3. Framework finds current arguments at the cursor position
        List<CommandArgument<?>> currentArgs = route.getCurrentArguments();
        
        // 4. In a real dispatcher (like veto), we have the arguments excluding the command name
        String[] dispatcherArgs = CommandManager.tokenizeToCommandArgs(input); // [""]
        
        // 5. We call complete on each possible argument
        boolean foundCreate = false;
        for (CommandArgument<?> arg : currentArgs) {
            List<CommandCompletion> completions = arg.complete(sender, command, dispatcherArgs);
            for (CommandCompletion completion : completions) {
                if (completion.candidate().equals("create")) {
                    foundCreate = true;
                }
            }
        }
        assertTrue(foundCreate, "Should find 'create' completion");
        
        // 6. Test next position: "party create "
        input = "party create ";
        route = manager.route(sender, input);
        currentArgs = route.getCurrentArguments();
        dispatcherArgs = CommandManager.tokenizeToCommandArgs(input); // ["create", ""]
        
        assertEquals(1, currentArgs.size());
        assertEquals("name", currentArgs.get(0).getName());
        List<CommandCompletion> completions = currentArgs.get(0).complete(sender, command, dispatcherArgs);
        // By default, a String argument has no completions
        assertTrue(completions.isEmpty());
    }

    @Test
    void testEmptyArgsDoesNotCrash() {
        CommandArgument<String> arg = CommandArgument.ofString();
        Command command = new Command("test", "desc") {
            @Override public void init() {}
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        };
        
        // This was the original bug: passing empty array
        assertDoesNotThrow(() -> arg.complete(sender, command, new String[0]));
    }
}
