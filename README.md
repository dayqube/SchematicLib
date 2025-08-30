# 📦 SchematicLib

A simple and lightweight library for pasting and undoing Minecraft schematics using WorldEdit/FAWE.

## ✨ Features

- ✅ Easy schematic pasting by name
- ✅ Automatic schematic loading from plugin's schematics folder
- ✅ Synchronous and asynchronous operations
- ✅ Built-in undo functionality
- ✅ Auto-remove feature with delay

## 📋 Requirements

- Spigot/Paper 1.13+
- Java 17+

## 📦 Installation

### Maven

```xml
<repository>
    <id>daycube-releases</id>
    <name>DAYCUBE</name>
    <url>https://repo.hy-pe.ru/releases</url>
</repository>

<dependency>
    <groupId>me.seetch</groupId>
    <artifactId>schematiclib</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```xml
maven {
    name "daycubeReleases"
    url "https://repo.hy-pe.ru/releases
}

implementation 'me.seetch:mlang:1.0.0'
```

### Manual

Download the JAR file from [Releases](https://github.com/dayqube/SchematicLib/releases) and add it to your project.

## 🛠 Usage

### Basic Setup

```java
public class MyPlugin extends JavaPlugin {
    private SchematicLib schematicLib;

    @Override
    public void onEnable() {
        schematicLib = new SchematicLib(this);
    }
}
```

### Synchronous Operations

```java
// Paste schematic
Location location = player.getLocation();
schematicLib.paste("house.schem", location);

// Undo paste
schematicLib.undo(location);
```

### Asynchronous Operations

```java
// Async paste with callback
schematicLib.pasteAsync("house.schem", location)
.thenRun(() -> {
    // Success
})
.exceptionally(error -> {
    // Handle error
return null;
});

// Async undo
schematicLib.undoAsync(location);
```

### Auto-remove Feature

```java
// Paste and automatically remove after 200 ticks (10 seconds)
schematicLib.pasteAndAutoRemove("house.schem", location, 200L);
```

### With Error Handling

```java
schematicLib.pasteAndAutoRemove("house.schem", location, 200L, (error) -> {
    player.sendMessage("Failed to paste schematic: " + error.getMessage());
});
```

## 📁 Schematic Files

Place your schematic files in:
plugins/YourPlugin/schematics/

Supported formats:
- .schem
- .schematic

## 💡 Example Command

```java
public class SchematicCommand implements CommandExecutor {
    private final SchematicLib manager;

    public SchematicCommand(SchematicLib manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only command!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("/pasteschem <schematic_name>");
            return true;
        }

        String schematicName = args[0];
        Location location = player.getLocation();

        manager.pasteAndAutoRemove(schematicName, location, 200L, (error) -> {
            player.sendMessage("Error: " + error.getMessage());
        }).thenRun(() -> {
            player.sendMessage("Schematic pasted! Will be removed in 10 seconds.");
        });

        return true;
    }
}
```

## 📚 API Methods

### SchematicLib

```text
- void paste(String schematicName, Location location)
- CompletableFuture<Void> pasteAsync(String schematicName, Location location)
- CompletableFuture<Void> pasteAsync(String schematicName, Location location, Consumer<Exception> onError)
- void undo(Location location)
- CompletableFuture<Void> undoAsync(Location location)
- CompletableFuture<Void> undoAsync(Location location, Consumer<Exception> onError)
- CompletableFuture<Void> pasteAndAutoRemove(String schematicName, Location location, long delayTicks)
- CompletableFuture<Void> pasteAndAutoRemove(String schematicName, Location location, long delayTicks, Consumer<Exception> onError)
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

If you have questions or suggestions, create an [Issue](https://github.com/dayqube/SchematicLib/issues) on GitHub.