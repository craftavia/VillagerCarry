package net.craftavia.villagercarry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * IDs of eggs that were already turned back into a villager. Every egg works once; a copied egg
 * (duplication glitch, creative middle-click) with the same ID is rejected and removed.
 */
public final class UsedEggs {

    private final VillagerCarryPlugin plugin;
    private final Path file;
    private final Set<UUID> used = ConcurrentHashMap.newKeySet();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        final Thread thread = new Thread(r, "VillagerCarry-writer");
        thread.setDaemon(true);
        return thread;
    });

    public UsedEggs(final VillagerCarryPlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("used-eggs.txt");
    }

    /** Called once at startup; the file is small (37 bytes per line). */
    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    used.add(UUID.fromString(line));
                } catch (final IllegalArgumentException ignored) {
                    // broken line, skip it
                }
            }
        } catch (final IOException ex) {
            plugin.getSLF4JLogger().error("Could not read the used egg list", ex);
        }
    }

    public boolean isUsed(final UUID id) {
        return used.contains(id);
    }

    /** Marks the egg as used and appends it to the file off the main thread. Returns false if it was already used. */
    public boolean markUsed(final UUID id) {
        if (!used.add(id)) {
            return false;
        }
        writer.execute(() -> {
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                out.write(id.toString());
                out.newLine();
            } catch (final IOException ex) {
                plugin.getSLF4JLogger().error("Could not save used egg {}", id, ex);
            }
        });
        return true;
    }

    public void close() {
        writer.shutdown();
        try {
            writer.awaitTermination(5, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
