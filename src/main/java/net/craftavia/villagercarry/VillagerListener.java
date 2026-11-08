package net.craftavia.villagercarry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VillagerListener implements Listener {

    static final String USE = "villagercarry.use";

    private static final Sound PICKUP_SOUND = Sound.sound(Key.key("entity.item.pickup"), Sound.Source.PLAYER, 0.8f, 1.0f);
    private static final Sound PLACE_SOUND = Sound.sound(Key.key("entity.villager.celebrate"), Sound.Source.NEUTRAL, 1.0f, 1.0f);

    private final VillagerCarryPlugin plugin;
    private final EggFactory eggs;
    private final Map<UUID, Long> lastAction = new ConcurrentHashMap<>();
    private boolean poiWarned;

    public VillagerListener(final VillagerCarryPlugin plugin, final EggFactory eggs) {
        this.plugin = plugin;
        this.eggs = eggs;
    }

    // ------------------------------------------------------------------ pick up

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final ItemStack hand = player.getInventory().getItem(event.getHand());

        // A villager egg must not be used on any mob (vanilla would spawn a baby villager).
        if (eggs.isEgg(hand)) {
            event.setCancelled(true);
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (!player.isSneaking() || !player.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }
        if (!player.hasPermission(USE)) {
            return;
        }

        event.setCancelled(true); // sneak + right-click = carry attempt, do not open the trade screen
        if (!villager.isValid() || villager.isDead()) {
            return;
        }

        if (!plugin.settings().worldAllowed(villager.getWorld())) {
            Messages.send(player, "world-disabled");
            return;
        }
        if (onCooldown(player)) {
            return;
        }
        if (villager.isTrading()) {
            Messages.send(player, "trading");
            return;
        }
        if (villager.isLeashed()) {
            Messages.send(player, "leashed");
            return;
        }
        if (!villager.isAdult() && !plugin.settings().allowBabies()) {
            Messages.send(player, "baby-disabled");
            return;
        }
        if (!protectionAllows(player, villager.getLocation())) {
            Messages.send(player, "not-allowed-here");
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            Messages.send(player, "inventory-full");
            return;
        }

        villager.eject();
        if (villager.isInsideVehicle()) {
            villager.leaveVehicle();
        }
        // A villager removed while sleeping leaves its bed "occupied" forever: wake it up first.
        if (villager.isSleeping()) {
            villager.wakeup();
        }

        final EntitySnapshot snapshot = villager.createSnapshot();
        if (snapshot == null) {
            Messages.send(player, "error");
            return;
        }
        final ItemStack egg = eggs.create(villager, snapshot, player.locale());

        releasePois(villager);
        villager.remove();

        player.getInventory().addItem(egg).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        player.playSound(PICKUP_SOUND);
        Messages.send(player, "picked-up");
        log(player, "picked up", villager);
    }

    // ------------------------------------------------------------------ place

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(final PlayerInteractEvent event) {
        final ItemStack item = event.getItem();
        if (!eggs.isEgg(item)) {
            return;
        }

        final boolean deniedByOther = event.useItemInHand() == Event.Result.DENY
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.useInteractedBlock() == Event.Result.DENY;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getHand() == null) {
            return;
        }
        if (deniedByOther) {
            return; // a protection plugin denied it and shows its own message
        }

        final Player player = event.getPlayer();
        if (!player.hasPermission(USE)) {
            return;
        }

        final Block clicked = event.getClickedBlock();
        final Material type = clicked.getType();
        if (type == Material.SPAWNER || type == Material.TRIAL_SPAWNER) {
            Messages.send(player, "spawner-blocked");
            return;
        }
        if (!plugin.settings().worldAllowed(clicked.getWorld())) {
            Messages.send(player, "world-disabled");
            return;
        }
        if (onCooldown(player)) {
            return;
        }

        final Block target = clicked.isPassable() && !clicked.isLiquid()
                ? clicked : clicked.getRelative(event.getBlockFace());
        if (!fits(target)) {
            Messages.send(player, "no-room");
            return;
        }
        final Location at = target.getLocation().add(0.5, 0, 0.5);
        at.setYaw(player.getLocation().getYaw() + 180f);
        if (!protectionAllows(player, at)) {
            Messages.send(player, "not-allowed-here");
            return;
        }

        final EquipmentSlot slot = event.getHand();
        final UUID id = eggs.id(item);
        if (id == null || plugin.usedEggs().isUsed(id)) {
            player.getInventory().setItem(slot, null);
            Messages.send(player, "invalid-egg");
            return;
        }

        Entity spawned;
        try {
            final EntitySnapshot snapshot = plugin.getServer().getEntityFactory().createEntitySnapshot(eggs.data(item));
            spawned = snapshot.createEntity(at);
        } catch (final RuntimeException ex) {
            plugin.getSLF4JLogger().warn("Could not read a villager egg ({}): {}", player.getName(), ex.getMessage());
            spawned = null;
        }
        if (!(spawned instanceof Villager villager)) {
            if (spawned != null) {
                spawned.remove();
            }
            Messages.send(player, "error");
            return;
        }

        // Old bed/workstation memories are not carried over: the villager looks for new ones at its new home.
        villager.setMemory(MemoryKey.HOME, null);
        villager.setMemory(MemoryKey.JOB_SITE, null);
        villager.setMemory(MemoryKey.POTENTIAL_JOB_SITE, null);
        villager.setMemory(MemoryKey.MEETING_POINT, null);
        // Faces the player who placed it.
        villager.setRotation(at.getYaw(), 0f);

        plugin.usedEggs().markUsed(id);
        final ItemStack inSlot = player.getInventory().getItem(slot);
        if (eggs.isEgg(inSlot) && id.equals(eggs.id(inSlot))) {
            player.getInventory().setItem(slot, null);
        }
        villager.getWorld().playSound(PLACE_SOUND, villager);
        Messages.send(player, "placed");
        log(player, "placed", villager);
    }

    /** Dispensers must not turn a villager egg into a blank villager like vanilla would. */
    @EventHandler(ignoreCancelled = true)
    public void onDispense(final BlockDispenseEvent event) {
        if (eggs.isEgg(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        lastAction.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ helpers

    private boolean onCooldown(final Player player) {
        final long now = System.currentTimeMillis();
        final Long last = lastAction.get(player.getUniqueId());
        if (last != null && now - last < plugin.settings().cooldownMs()) {
            return true;
        }
        lastAction.put(player.getUniqueId(), now);
        return false;
    }

    private boolean protectionAllows(final Player player, final Location at) {
        if (player.hasPermission(CarryCommand.ADMIN)) {
            return true;
        }
        final WorldGuardHook worldGuard = plugin.worldGuard();
        if (worldGuard == null || !plugin.settings().worldGuardCheck()) {
            return true;
        }
        return worldGuard.canBuild(player, at);
    }

    private void log(final Player player, final String action, final Villager villager) {
        if (!plugin.settings().log()) {
            return;
        }
        final Location loc = villager.getLocation();
        plugin.getSLF4JLogger().info("{} {} a villager: {} {} level {} ({} {}, {}, {})", player.getName(), action,
                villager.getProfession().key().value(), villager.getVillagerType().key().value(),
                villager.getVillagerLevel(), loc.getWorld().key().value(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private static boolean fits(final Block block) {
        final Block up = block.getRelative(BlockFace.UP);
        return block.isPassable() && !block.isLiquid() && up.isPassable() && !up.isLiquid();
    }

    /**
     * A removed villager does not release its workstation/bed (vanilla only does that on death),
     * so that block would stay "taken" forever. Calls the server's (Mojang-mapped) method.
     */
    private void releasePois(final Villager villager) {
        try {
            final Object handle = villager.getClass().getMethod("getHandle").invoke(villager);
            handle.getClass().getMethod("releaseAllPois").invoke(handle);
        } catch (final ReflectiveOperationException | RuntimeException ex) {
            if (!poiWarned) {
                poiWarned = true;
                plugin.getSLF4JLogger().warn("Could not release the villager's workstation/bed: {}", ex.toString());
            }
        }
    }
}
