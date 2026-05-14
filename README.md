# Fabric Shell Mod

Minecraft Fabric server-side mod that adds an OP-only `/shell <command>` command.

> Warning: This mod executes arbitrary shell commands on the server host. Use it only in local or trusted test environments.

## Features

- `/shell <command>` runs commands through `sh -c <command>` on the server
- Requires owner-level permission (`PermissionLevel` 4)
- Executes asynchronously so the server thread is not blocked while the process runs
- Streams stdout and stderr to Minecraft chat
- Shows the process exit code
- Converts ANSI colors, 256-color, truecolor, and common text styles to Minecraft text formatting
- Handles common ANSI cursor positioning sequences used by tools like `fastfetch`

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `0.19.2`
- Fabric API `0.141.4+1.21.11`
- Java 21+

## Build

```bash
./gradlew build
```

The mod jar is generated at:

```text
build/libs/shellmod-1.0.0.jar
```

## Usage

```text
/shell echo "Hello World"
/shell ls -la
/shell curl -s https://api.github.com/users/octocat
/shell ./fastfetch-linux-amd64/usr/bin/fastfetch
```

## License

MIT
