# FocessCommand

FocessCommand is a lightweight Java command framework for building typed, permission-aware command handlers.

> **Note:** Starting with `2.0.0`, this library targets **Java 17**.

## Features

- **Typed command arguments** with built-in and custom converters.
- **Quoted argument support** (single and double quotes) for whitespace-containing strings.
- **Integrated Command Dispatcher** for executing raw input strings directly via `CommandManager`.
- **Intelligent Tab-completion API** returning structured `CommandCompletion` (candidate + optional description).
- **Metadata support**: add descriptions to `Command` and `CommandArgument` for richer UI integration.
- **Enhanced DataConverters**: easily create choices or enums with associated descriptions.
- **Multi-layer Permission System** (Static Roles + Dynamic Predicates).
- **Case-insensitive command lookup** and case-insensitive `Enum` support.
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

## Build

```bash
mvn -B -Dgpg.skip=true verify
```

## Future features

- Better command help formatting and discoverability
- Automatic Command usage generation
