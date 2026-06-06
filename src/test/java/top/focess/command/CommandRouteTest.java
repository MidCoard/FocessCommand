package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandRouteTest {

    private CommandManager manager;
    private TestCommandSender sender;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        sender = new TestCommandSender(CommandPermission.OWNER);
        manager.register(new Command("party", "A party command") {
            @Override
            public void init() {
                addExecutor((s, d) -> CommandResult.ALLOW, CommandArgument.of("create"), CommandArgument.ofString().named("name"));
                addExecutor((s, d) -> CommandResult.ALLOW, CommandArgument.of("remove"), CommandArgument.ofString().named("name"));
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });
    }

    @Test
    void testRouteInfo() {
        CommandRoute route = manager.route(sender, "party create p1");
        assertNotNull(route.getCommand());
        assertEquals("party", route.getCommand().getName());
        assertNotNull(route.getExecutor());
        // Fixed argument "create" is not in DataCollection
        assertEquals("p1", route.getData().get());
        assertEquals(CommandResult.MATCHED, route.getState());
    }

    @Test
    void testCompletionEdgeCases() {
        // "party create p1" -> complete p1
        List<CommandCompletion> c1 = manager.complete(sender, "party create p1");
        
        // "party create p1 " -> complete next arg (none)
        List<CommandCompletion> c2 = manager.complete(sender, "party create p1 ");
        
        // "party create \"hello world\"" -> complete after "create" (nothing for "hello world" once closed)
        List<CommandCompletion> c3 = manager.complete(sender, "party create \"hello world\"");
        assertTrue(c3.isEmpty(), "Completions should be empty for a closed quote without a trailing space");

        // "party create \"hello " -> complete "hello " (as a partial argument)
        List<CommandCompletion> c4 = manager.complete(sender, "party create \"hello ");
    }
    
    @Test
    void testEmptyCompletion() {
        // "party" is registered. So empty string should suggest "party"
        List<CommandCompletion> c1 = manager.complete(sender, "");
        assertTrue(c1.stream().anyMatch(c -> c.candidate().equals("party")), "Empty string should suggest root commands");
    }

    @Test
    void testCurrentArguments() {
        // "party create " -> should suggest name argument
        CommandRoute route = manager.route(sender, "party create ");
        List<CommandArgument<?>> current = route.getCurrentArguments();
        assertEquals(1, current.size());
        assertEquals("name", current.get(0).getName());
        assertFalse(current.get(0).isNullable());
    }

    @Test
    void testCurrentArgumentsWithNullable() {
        manager.register(new Command("opt", "Optional test") {
            @Override
            public void init() {
                // opt <req> [optional]
                addExecutor((s, d) -> CommandResult.ALLOW, CommandArgument.ofString().named("req"), CommandArgument.ofNullable(DataConverter.DEFAULT_DATA_CONVERTER).named("optional"));
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        // "opt val " -> we've filled req, we are at the optional argument
        CommandRoute route = manager.route(sender, "opt val ");
        List<CommandArgument<?>> current = route.getCurrentArguments();
        assertEquals(1, current.size());
        assertEquals("optional", current.get(0).getName());
        assertTrue(current.get(0).isNullable());
    }

    @Test
    void testCurrentArgumentsWithMultipleExecutors() {
        final CommandArgument<String> a = CommandArgument.of("a");
        final CommandArgument<String> c = CommandArgument.of("c");
        manager.register(new Command("multi", "Multiple test") {
            @Override
            public void init() {
                // multi a [b] c
                addExecutor((s, d) -> CommandResult.ALLOW, a, CommandArgument.ofNullable(DataConverter.DEFAULT_DATA_CONVERTER).named("b"), c);
                // multi a c
                addExecutor((s, d) -> CommandResult.ALLOW, a, c);
            }
            @Override public @NotNull List<String> usage(CommandSender sender) { return Lists.newArrayList(); }
        });

        // "multi a " -> position 1 could be [b] or c
        CommandRoute route = manager.route(sender, "multi a ");
        List<CommandArgument<?>> current = route.getCurrentArguments();
        // Executor 1 contributes [b] and c
        // Executor 2 contributes c
        // Since we used the same 'c' object, distinct() should collapse them to 2
        assertEquals(2, current.size());
        
        assertTrue(current.stream().anyMatch(arg -> "b".equals(arg.getName())));
        assertTrue(current.stream().anyMatch(arg -> arg == c));
    }

    @Test
    void testTokenizationDirectly() {
        CommandRoute r1 = manager.route(sender, "party create \"hello world\"");
        assertEquals(Lists.newArrayList("party", "create", "hello world"), r1.getTokens());
        
        CommandRoute r2 = manager.route(sender, "party create \"hello ");
        assertEquals(Lists.newArrayList("party", "create", "hello "), r2.getTokens());
        
        CommandRoute r3 = manager.route(sender, "party create p1 ");
        assertEquals(Lists.newArrayList("party", "create", "p1", ""), r3.getTokens());
    }
}
