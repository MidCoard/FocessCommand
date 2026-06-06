package top.focess.command;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.focess.command.CommandManager.Token;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenizerTest {

    private CommandManager manager;
    private CommandSender sender;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        sender = new AbstractCommandSender(CommandPermission.OWNER) {
            @Override @NotNull public String input() { return ""; }
            @Override public void output(@NotNull String message) {}
        };
    }

    private List<Token> tokenize(String input) {
        // We use CommandRoute's getTokens() as a proxy to test tokenization
        // but to test the Token properties (isQuoted, isUnclosed) directly,
        // we can use reflection or simply verify the behavior via completions.
        // For a deep test, we can use reflection to access the private tokenize method.
        try {
            java.lang.reflect.Method method = CommandManager.class.getDeclaredMethod("tokenize", String.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Token> tokens = (List<Token>) method.invoke(manager, input);
            return tokens;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBasicTokenization() {
        List<Token> tokens = tokenize("party create p1");
        assertEquals(3, tokens.size());
        assertEquals("party", tokens.get(0).content());
        assertEquals("create", tokens.get(1).content());
        assertEquals("p1", tokens.get(2).content());
        
        assertFalse(tokens.get(0).isQuoted());
        assertFalse(tokens.get(1).isQuoted());
        assertFalse(tokens.get(2).isQuoted());
    }

    @Test
    void testMultipleSpaces() {
        List<Token> tokens = tokenize("party    create   p1");
        assertEquals(3, tokens.size());
        assertEquals("party", tokens.get(0).content());
        assertEquals("create", tokens.get(1).content());
        assertEquals("p1", tokens.get(2).content());
    }

    @Test
    void testTrailingSpace() {
        List<Token> tokens = tokenize("party create ");
        assertEquals(3, tokens.size());
        assertEquals("party", tokens.get(0).content());
        assertEquals("create", tokens.get(1).content());
        assertEquals("", tokens.get(2).content());
        assertFalse(tokens.get(2).isQuoted());
        assertFalse(tokens.get(2).isUnclosed());
    }

    @Test
    void testDoubleQuotes() {
        List<Token> tokens = tokenize("party create \"hello world\"");
        assertEquals(3, tokens.size());
        assertEquals("party", tokens.get(0).content());
        assertEquals("create", tokens.get(1).content());
        assertEquals("hello world", tokens.get(2).content());
        
        assertTrue(tokens.get(2).isQuoted());
        assertFalse(tokens.get(2).isUnclosed());
    }

    @Test
    void testSingleQuotes() {
        List<Token> tokens = tokenize("party create 'hello world'");
        assertEquals(3, tokens.size());
        assertEquals("hello world", tokens.get(2).content());
        assertTrue(tokens.get(2).isQuoted());
        assertFalse(tokens.get(2).isUnclosed());
    }

    @Test
    void testUnclosedDoubleQuote() {
        List<Token> tokens = tokenize("party create \"hello world");
        assertEquals(3, tokens.size());
        assertEquals("hello world", tokens.get(2).content());
        assertTrue(tokens.get(2).isQuoted());
        assertTrue(tokens.get(2).isUnclosed());
    }

    @Test
    void testUnclosedSingleQuote() {
        List<Token> tokens = tokenize("party create 'hello world");
        assertEquals(3, tokens.size());
        assertEquals("hello world", tokens.get(2).content());
        assertTrue(tokens.get(2).isQuoted());
        assertTrue(tokens.get(2).isUnclosed());
    }

    @Test
    void testNestedQuotes() {
        // "This 'is' nested" -> should be parsed as one double-quoted token containing single quotes
        List<Token> tokens = tokenize("say \"This 'is' nested\"");
        assertEquals(2, tokens.size());
        assertEquals("say", tokens.get(0).content());
        assertEquals("This 'is' nested", tokens.get(1).content());
        assertTrue(tokens.get(1).isQuoted());
        assertFalse(tokens.get(1).isUnclosed());

        // 'This "is" nested'
        tokens = tokenize("say 'This \"is\" nested'");
        assertEquals(2, tokens.size());
        assertEquals("This \"is\" nested", tokens.get(1).content());
        assertTrue(tokens.get(1).isQuoted());
    }

    @Test
    void testEmptyString() {
        List<Token> tokens = tokenize("");
        assertEquals(1, tokens.size());
        assertEquals("", tokens.get(0).content());
        assertFalse(tokens.get(0).isQuoted());
    }

    @Test
    void testJustSpaces() {
        List<Token> tokens = tokenize("   ");
        assertEquals(1, tokens.size());
        assertEquals("", tokens.get(0).content());
    }
}
