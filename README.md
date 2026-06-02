# FocessCommand

FocessCommand is a lightweight Java command framework for building typed, permission-aware command handlers.

> **Note:** Starting with `2.0.0`, this library targets **Java 17**.

## Features

- Typed command arguments with built-in converters
- Executor-level and command-level permission checks
- Command aliases and case-insensitive command lookup
- Structured execution result reporting
- Manager-based command registries for isolated command scopes

## Dependency

### Maven

```xml
<dependency>
    <groupId>top.focess</groupId>
    <artifactId>focess-command</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'top.focess:focess-command:2.0.0'
```

## Build

```bash
mvn -B -Dgpg.skip=true verify
```

## Future features

- Better command help formatting and discoverability
- **Tab-complete support** for command names and arguments