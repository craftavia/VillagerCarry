package net.craftavia.villagercarry;

import net.kyori.adventure.translation.Translator;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Values read from config.yml. */
public record Settings(Set<String> worlds, long cooldownMs, boolean allowBabies, boolean worldGuardCheck,
                       boolean log, Locale defaultLocale) {

    public static Settings from(final FileConfiguration config) {
        final Set<String> worlds = config.getStringList("worlds").stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        final Locale locale = Translator.parseLocale(config.getString("default-language", "en"));
        return new Settings(
                worlds,
                Math.max(0, config.getLong("cooldown-ms", 1000)),
                config.getBoolean("allow-babies", true),
                config.getBoolean("worldguard-check", true),
                config.getBoolean("log", true),
                locale == null ? Locale.ENGLISH : locale);
    }

    /** Matches the world folder name (world), the full key (minecraft:overworld) or the short key (overworld). */
    public boolean worldAllowed(final World world) {
        if (worlds.isEmpty()) {
            return true;
        }
        return worlds.contains(world.getName().toLowerCase(Locale.ROOT))
                || worlds.contains(world.key().asString())
                || worlds.contains(world.key().value());
    }
}
