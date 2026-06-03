# FocessCommand

FocessCommand is a lightweight Java command framework for building typed, permission-aware command handlers.

> **Note:** Starting with `2.0.0`, this library targets **Java 17**.

## Features

- **Typed command arguments** with built-in and custom converters.
- **Quoted argument support** (single and double quotes) for whitespace-containing strings.
- **Integrated Command Dispatcher** for executing raw input strings directly via `CommandManager`.
- **Intelligent Tab-completion API** for command names, aliases, and positional arguments (including support for optional/nullable arguments).
- **Multi-layer Permission System** (Static Roles + Dynamic Predicates).
- **Case-insensitive command lookup** and case-insensitive `Enum` support.
- **Structured execution result reporting**.

## Dependency

### Maven

```xml
<dependency>
    <groupId>top.focess</groupId>
    <artifactId>focess-command</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'top.focess:focess-command:2.1.0'
```

## Build

```bash
mvn -B -Dgpg.skip=true verify
```

## Future features

- Better command help formatting and discoverability
- Automatic Command usage generation
