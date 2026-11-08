package net.craftavia.villagercarry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Creates and reads villager eggs. The villager itself is stored as SNBT in the egg's PersistentDataContainer. */
public final class EggFactory {

    /** Total XP needed for each villager level (vanilla thresholds). */
    private static final int[] LEVEL_XP = {0, 10, 70, 150, 250};
    private static final int BAR = 20;

    private final VillagerCarryPlugin plugin;
    private final NamespacedKey dataKey;
    private final NamespacedKey idKey;
    private boolean trimWarned;

    public EggFactory(final VillagerCarryPlugin plugin) {
        this.plugin = plugin;
        this.dataKey = new NamespacedKey(plugin, "data");
        this.idKey = new NamespacedKey(plugin, "id");
    }

    public boolean isEgg(final ItemStack item) {
        return item != null && item.getType() == Material.VILLAGER_SPAWN_EGG
                && item.getPersistentDataContainer().has(dataKey, PersistentDataType.STRING);
    }

    public String data(final ItemStack item) {
        return item.getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);
    }

    public UUID id(final ItemStack item) {
        final String value = item.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }

    /** Builds the egg; its name and description are written in {@code locale} (the player picking it up). */
    public ItemStack create(final Villager villager, final EntitySnapshot snapshot, final Locale locale) {
        final String data = compactData(snapshot);
        final ItemStack egg = ItemStack.of(Material.VILLAGER_SPAWN_EGG);
        egg.editMeta(meta -> {
            meta.customName(line(locale, "egg-name"));
            meta.lore(lore(villager, locale));
            meta.setMaxStackSize(1);
            meta.addItemFlags(ItemFlag.values());
            final PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(dataKey, PersistentDataType.STRING, data);
            pdc.set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        });
        return egg;
    }

    /**
     * Data written to the egg: SNBT without short-lived fields. If trimming is not possible or the trimmed
     * data cannot be read back, the full data is written instead, so a villager is never lost.
     */
    private String compactData(final EntitySnapshot snapshot) {
        final String full = snapshot.getAsString();
        if (SnapshotTrimmer.trim(snapshot)) {
            final String slim = snapshot.getAsString();
            try {
                if (plugin.getServer().getEntityFactory().createEntitySnapshot(slim).getEntityType()
                        == EntityType.VILLAGER) {
                    return slim;
                }
            } catch (final RuntimeException ignored) {
                // fall back to the full data below
            }
        }
        if (!trimWarned) {
            trimWarned = true;
            plugin.getSLF4JLogger().warn("Could not shrink the villager data, eggs store the full data.");
        }
        return full;
    }

    private List<Component> lore(final Villager villager, final Locale locale) {
        final List<Component> lines = new ArrayList<>();

        final String type = villager.getVillagerType().key().value();
        // Unknown (data pack) types fall back to the capitalised key.
        final String fallback = type.isEmpty() ? type : type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1);
        lines.add(line(locale, "lore-type",
                GlobalTranslator.render(Component.translatable(Messages.NS + "type." + type, fallback), locale)));
        lines.add(line(locale, villager.isAdult() ? "lore-adult" : "lore-baby"));

        final AttributeInstance maxAttr = villager.getAttribute(Attribute.MAX_HEALTH);
        final double max = maxAttr == null ? 20 : maxAttr.getValue();
        final double health = Math.min(villager.getHealth(), max);
        final int totalHearts = (int) Math.ceil(max / 2);
        final int fullHearts = (int) Math.ceil(health / 2);
        lines.add(line(locale, "lore-health",
                Component.text("❤".repeat(fullHearts)),
                Component.text("❤".repeat(Math.max(0, totalHearts - fullHearts))),
                Component.text(format(health)),
                Component.text(format(max))));

        final Villager.Profession profession = villager.getProfession();
        final int level = Math.clamp(villager.getVillagerLevel(), 1, 5);
        final Component professionName = profession == Villager.Profession.NONE
                ? Messages.render("profession-none", locale)
                : Component.translatable(profession.translationKey());
        lines.add(line(locale, "lore-profession", professionName,
                Component.translatable("merchant.level." + level)));

        final int xp = villager.getVillagerExperience();
        if (level >= 5) {
            lines.add(line(locale, "lore-xp-max", Component.text(xp)));
        } else {
            final int needed = LEVEL_XP[level];
            final int filled = (int) Math.round(Math.clamp(xp / (double) needed, 0, 1) * BAR);
            lines.add(line(locale, "lore-xp",
                    Component.text("|".repeat(filled)),
                    Component.text("|".repeat(BAR - filled)),
                    Component.text(xp),
                    Component.text(needed)));
        }

        lines.add(Component.empty());
        lines.add(line(locale, "lore-home-title"));
        final Location home = villager.getMemory(MemoryKey.HOME);
        lines.add(home == null ? line(locale, "lore-bed-none") : line(locale, "lore-bed", position(home)));
        final Location job = villager.getMemory(MemoryKey.JOB_SITE);
        lines.add(job == null ? line(locale, "lore-job-none") : line(locale, "lore-job", position(job)));

        lines.add(Component.empty());
        final List<MerchantRecipe> recipes = villager.getRecipes();
        lines.add(line(locale, "lore-trades-title", Component.text(recipes.size())));
        if (recipes.isEmpty()) {
            lines.add(line(locale, "lore-trades-none"));
        }
        for (final MerchantRecipe recipe : recipes) {
            Component cost = Component.empty();
            boolean first = true;
            for (final ItemStack ingredient : recipe.getIngredients()) {
                if (ingredient == null || ingredient.getType().isAir()) {
                    continue;
                }
                if (!first) {
                    cost = cost.append(Messages.render("lore-trade-plus", locale));
                }
                cost = cost.append(stack(ingredient, locale));
                first = false;
            }
            lines.add(line(locale, "lore-trade", cost,
                    stack(recipe.getResult(), locale).append(enchants(recipe.getResult(), locale))));
            final boolean exhausted = recipe.getUses() >= recipe.getMaxUses();
            final Component uses = Messages.render(exhausted ? "lore-uses-full" : "lore-uses-open", locale,
                    Component.text(recipe.getUses()), Component.text(recipe.getMaxUses()));
            lines.add(line(locale, "lore-trade-info", uses, Component.text(recipe.getVillagerExperience())));
        }

        lines.add(Component.empty());
        lines.add(line(locale, "lore-hint"));
        return lines;
    }

    private static Component line(final Locale locale, final String key, final ComponentLike... args) {
        return Messages.render(key, locale, args).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static Component stack(final ItemStack item, final Locale locale) {
        return Messages.render("lore-trade-item", locale, Component.text(item.getAmount()), item.effectiveName());
    }

    private static Component enchants(final ItemStack item, final Locale locale) {
        final Map<Enchantment, Integer> map = item.getItemMeta() instanceof EnchantmentStorageMeta stored
                ? stored.getStoredEnchants() : item.getEnchantments();
        Component out = Component.empty();
        for (final Map.Entry<Enchantment, Integer> entry : map.entrySet()) {
            // displayName comes colored (curses are red); the message file decides the color of the line.
            final Component name = entry.getKey().displayName(entry.getValue()).color(null);
            out = out.append(Messages.render("lore-trade-enchant", locale, name));
        }
        return out;
    }

    private static Component position(final Location location) {
        return Component.text(location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
    }

    private static String format(final double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
