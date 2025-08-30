package me.seetch;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SchematicLib {

    private final Plugin plugin;
    private final File schematicsFolder;
    private final Map<Location, EditSession> sessions = new HashMap<>();

    public SchematicLib(Plugin plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    public CompletableFuture<Void> pasteAsync(String schematicName, Location location) {
        return pasteAsync(schematicName, location, null);
    }

    public CompletableFuture<Void> pasteAsync(String schematicName, Location location, Consumer<Exception> onError) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File file = new File(schematicsFolder, schematicName);
                if (!file.exists()) {
                    throw new IllegalArgumentException("Schematic file not found: " + schematicName);
                }

                ClipboardFormat format = ClipboardFormats.findByFile(file);
                if (format == null) {
                    throw new IllegalArgumentException("Unknown schematic format: " + schematicName);
                }

                Clipboard clipboard;
                try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                    clipboard = reader.read();
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        performPaste(clipboard, location);
                        future.complete(null);
                    } catch (Exception e) {
                        if (onError != null) {
                            onError.accept(e);
                        }
                        future.completeExceptionally(e);
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (onError != null) {
                        onError.accept(e);
                    }
                    future.completeExceptionally(e);
                });
            }
        });

        return future;
    }

    public void paste(String schematicName, Location location) {
        File file = new File(schematicsFolder, schematicName);
        if (!file.exists()) {
            throw new IllegalArgumentException("Schematic file not found: " + schematicName);
        }

        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) {
                throw new IllegalArgumentException("Unknown schematic format: " + schematicName);
            }

            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                clipboard = reader.read();
            }

            performPaste(clipboard, location);

        } catch (Exception e) {
            throw new RuntimeException("Failed to paste schematic: " + schematicName, e);
        }
    }

    private void performPaste(Clipboard clipboard, Location location) {
        com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(location.getWorld());
        EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld);

        try {
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ())).ignoreAirBlocks(true).build();

            Operations.complete(operation);
            editSession.close();

            sessions.put(location, editSession);
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform paste operation", e);
        }
    }

    public CompletableFuture<Void> undoAsync(Location location) {
        return undoAsync(location, null);
    }

    public CompletableFuture<Void> undoAsync(Location location, Consumer<Exception> onError) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                performUndo(location);
                future.complete(null);
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e);
                }
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public void undo(Location location) {
        performUndo(location);
    }

    private void performUndo(Location location) {
        EditSession session = sessions.remove(location);
        if (session != null) {
            try {
                EditSession newSession = WorldEdit.getInstance().newEditSession(session.getWorld());
                session.undo(newSession);
            } finally {
                session.close();
            }
        }
    }

    public CompletableFuture<Void> pasteAndAutoRemove(String schematicName, Location location, long delayTicks) {
        return pasteAndAutoRemove(schematicName, location, delayTicks, null);
    }

    public CompletableFuture<Void> pasteAndAutoRemove(String schematicName, Location location, long delayTicks, Consumer<Exception> onError) {
        return pasteAsync(schematicName, location, onError).thenRun(() -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                undo(location);
            }, delayTicks);
        });
    }
}
