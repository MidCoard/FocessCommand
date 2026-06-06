# FocessCommand

FocessCommand is a lightweight Java command framework for building typed, permission-aware command handlers.

> **Note:** Starting with `2.0.0`, this library targets **Java 17**.

## Features

- **Typed command arguments** with built-in and custom converters.
- **Quoted argument support** (single and double quotes) for whitespace-containing strings.
- **Stateless Routing Engine** that handles dispatching and auto-completion via `CommandRoute` with strict state execution (ALLOW, REFUSE, ARGS).
- **Unified CommandSender API** that integrates both permissions and synchronous/asynchronous I/O.
- **Intelligent Tab-completion API** returning structured `CommandCompletion` (candidate + optional description).
- **Metadata support**: add descriptions to `Command` and `CommandArgument` for richer UI integration.
- **Enhanced DataConverters**: easily create choices or enums with associated descriptions.
- **Multi-layer Permission System** (Static Roles: EVERYONE, ADMINISTRATOR, OWNER + Dynamic Predicates).
- **Structured execution result reporting**.

## Dependency

### Maven

```xml
<dependency>
    <groupId>top.focess</groupId>
    <artifactId>focess-command</artifactId>
    <version>2.2.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'top.focess:focess-command:2.2.0'
```

## Example Usage

### 1. Creating a Command
Commands are defined by extending the `Command` class and adding `Executor`s in the `init()` method.

```java
import top.focess.command.*;
import java.util.List;

public class PartyCommand extends Command {

    public PartyCommand() {
        super("party", "Manage your party", "p"); // "party" with alias "p"
        // Set a base permission for the entire command
        this.setPermission(CommandPermission.EVERYONE);
    }

    @Override
    public void init() {
        // Executor 1: party create <name>
        this.addExecutor((sender, data) -> {
            String partyName = data.get("name");
            sender.output("Party '" + partyName + "' created!");
            return CommandResult.ALLOW; // Must return ALLOW, REFUSE, or ARGS
        }, CommandArgument.of("create"), CommandArgument.ofString().named("name"));

        // Executor 2: party invite <player> (Only administrators can invite)
        this.addExecutor((sender, data) -> {
            String playerName = data.get();
            sender.output("Invited " + playerName + " to the party.");
            return CommandResult.ALLOW;
        }, CommandArgument.of("invite"), CommandArgument.ofString())
        .setPermission(CommandPermission.ADMINISTRATOR); // Overrides base permission
    }

    @Override
    public @NotNull List<String> usage(CommandSender sender) {
        return List.of(
            "Usage: /party create <name>",
            "Usage: /party invite <player>"
        );
    }
}
```

### 2. Creating a Sender
The `CommandSender` handles permissions and interactions. You can implement the interface directly or extend `AbstractCommandSender` to inherit asynchronous input support.

```java
import org.jetbrains.annotations.NotNull;
import top.focess.command.*;

public class ConsoleSender extends AbstractCommandSender {
    
    public ConsoleSender() {
        super(CommandPermission.OWNER); // Highest permission level
    }

    @Override
    public @NotNull String input() {
        // Provide synchronous input logic here
        return "";
    }

    @Override
    public void output(@NotNull String message) {
        System.out.println("[Console] " + message);
    }
}
```

### 3. Registration and Dispatch
Use `CommandManager` to register commands, dispatch execution, and get auto-completions.

```java
import top.focess.command.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        CommandManager manager = new CommandManager();
        CommandSender sender = new ConsoleSender();

        // Register the command
        manager.register(new PartyCommand());

        // Dispatch a command string
        ExecutionResult result = manager.dispatch(sender, "party create Heroes");
        // Outputs: [Console] Party 'Heroes' created!
        // result.getResult() == CommandResult.ALLOW

        // Auto-completion
        List<CommandCompletion> completions = manager.complete(sender, "party i");
        for (CommandCompletion completion : completions) {
            System.out.println(completion.candidate()); // Outputs: "invite"
        }
    }
}
```

## Build

```bash
mvn -B -Dgpg.skip=true verify
```

## Future features

- Better command help formatting and discoverability
- Automatic Command usage generation
