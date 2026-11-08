package net.craftavia.villagercarry;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class VillagerCarryPlugin extends JavaPlugin {

    private final Messages messages = new Messages(this);
    private Settings settings;
    private UsedEggs usedEggs;
    private WorldGuardHook worldGuard;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages.saveDefaults();
        if (!load()) {
            getSLF4JLogger().error("Could not read the configuration, using the defaults where needed.");
        }

        usedEggs = new UsedEggs(this);
        usedEggs.load();

        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuard = new WorldGuardHook();
            getSLF4JLogger().info("WorldGuard found, region build permission will be checked.");
        }

        final EggFactory eggs = new EggFactory(this);
        getServer().getPluginManager().registerEvents(new VillagerListener(this, eggs), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("villagercarry", "VillagerCarry admin command",
                        List.of("vcarry"), new CarryCommand(this)));
    }

    @Override
    public void onDisable() {
        if (usedEggs != null) {
            usedEggs.close();
        }
        messages.unregister();
    }

    /** Reads config.yml and the message files. Returns false if something could not be read. */
    boolean load() {
        boolean ok = true;
        reloadConfig();
        settings = Settings.from(getConfig());
        try {
            messages.apply(messages.read(), settings.defaultLocale());
        } catch (final Exception ex) {
            getSLF4JLogger().error("Could not read the message files", ex);
            ok = false;
        }
        return ok;
    }

    void reload(final CommandSender sender) {
        Messages.send(sender, load() ? "reloaded" : "reload-failed");
    }

    public Settings settings() {
        return settings;
    }

    public UsedEggs usedEggs() {
        return usedEggs;
    }

    public WorldGuardHook worldGuard() {
        return worldGuard;
    }
}
