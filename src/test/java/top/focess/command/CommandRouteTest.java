package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandRouteTest {

    private CommandManager manager;
    private TestSender owner;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        owner = new TestSender(CommandPermission.OWNER);
        
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
        CommandRoute route = manager.route(owner, "party create p1");
        assertNotNull(route.getCommand());
        assertEquals("party", route.getCommand().getName());
        assertNotNull(route.getExecutor());
        assertEquals(2, route.getData().size());
        // Fixed argument "create" is not in DataCollection
        assertEquals("p1", route.getData().get());
        assertEquals(CommandResult.ALLOW, route.getState());
    }

    @Test
    void testCompletionEdgeCases() {
        // "party create p1" -> complete p1
        List<CommandCompletion> c1 = manager.complete(owner, "party create p1");
        
        // "party create p1 " -> complete next arg (none)
        List<CommandCompletion> c2 = manager.complete(owner, "party create p1 ");
        
        // "party create \"hello world\"" -> complete after "create" (nothing for "hello world" once closed)
        List<CommandCompletion> c3 = manager.complete(owner, "party create \"hello world\"");
        assertTrue(c3.isEmpty(), "Completions should be empty for a closed quote without a trailing space");

        // "party create \"hello " -> complete "hello " (as a partial argument)
        List<CommandCompletion> c4 = manager.complete(owner, "party create \"hello ");
    }
    
    @Test
    void testTokenizationDirectly() {
        CommandRoute r1 = manager.route(owner, "party create \"hello world\"");
        assertEquals(Lists.newArrayList("party", "create", "hello world"), r1.getTokens());
        
        CommandRoute r2 = manager.route(owner, "party create \"hello ");
        assertEquals(Lists.newArrayList("party", "create", "hello "), r2.getTokens());
        
        CommandRoute r3 = manager.route(owner, "party create p1 ");
        assertEquals(Lists.newArrayList("party", "create", "p1", ""), r3.getTokens());
    }

    private static class TestSender extends AbstractCommandSender {
        TestSender(CommandPermission p) { super(p); }
        @Override @NotNull public String input() { return ""; }
        @Override public void output(@NotNull String message) {}
    }
}
