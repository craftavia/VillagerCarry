package net.craftavia.villagercarry;

import org.bukkit.entity.EntitySnapshot;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Removes short-lived fields from the villager data stored in the egg so eggs stay small
 * (a full villager record with its brain can be several kilobytes, which matters when players
 * store many eggs in shulker boxes). Every key not listed here is kept: villager data, offers,
 * xp, gossips (reputation/discounts), name, health, age, inventory, effects, attributes,
 * NoAI/Silent/Invulnerable/PersistenceRequired, plugin data, restock timers, ...
 */
final class SnapshotTrimmer {

    /** Removed top-level keys. All of them fall back to their default when the villager is spawned again. */
    static final List<String> REMOVED = List.of(
            // Bed, workstation and meeting point are cleared on placement anyway; the rest belongs to the
            // old place. Without last_slept the villager cannot summon a golem until it sleeps again
            // at its new home (the abuse-safe direction).
            "Brain",
            "sleeping_pos",
            // Position, motion and momentary state: it starts fresh at the new place.
            "Pos", "Motion", "Rotation", "fall_distance", "FallDistance", "Fire", "Air", "OnGround",
            "PortalCooldown", "HurtTime", "DeathTime", "FallFlying",
            "last_hurt_by_player", "last_hurt_by_player_memory_time", "last_hurt_by_mob", "ticks_since_last_hurt_by_mob",
            "current_impulse_context_reset_grace_time", "current_explosion_impact_pos",
            // Identity and old world: the placed villager gets a new UUID.
            "UUID", "WorldUUIDLeast", "WorldUUIDMost",
            "Paper.Origin", "Paper.OriginWorld",
            // Defaults to 2 when missing.
            "Bukkit.updateLevel");

    private SnapshotTrimmer() {
    }

    /**
     * Removes the {@link #REMOVED} keys from the snapshot's NBT in place. Uses the server's internal
     * CompoundTag through reflection; returns false (and leaves the data untouched) if that changes.
     */
    static boolean trim(final EntitySnapshot snapshot) {
        try {
            final Object tag = snapshot.getClass().getMethod("getData").invoke(snapshot);
            final Method remove = tag.getClass().getMethod("remove", String.class);
            for (final String key : REMOVED) {
                remove.invoke(tag, key);
            }
            return true;
        } catch (final ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }
}
