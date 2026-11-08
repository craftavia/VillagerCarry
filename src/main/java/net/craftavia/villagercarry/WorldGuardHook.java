package net.craftavia.villagercarry;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** All WorldGuard calls live here; the class is only loaded when WorldGuard is installed. */
public final class WorldGuardHook {

    /** Region build permission: wilderness is allowed, protected regions only for members / build flag. */
    public boolean canBuild(final Player player, final Location at) {
        final LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        final var platform = WorldGuard.getInstance().getPlatform();
        if (platform.getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(at.getWorld()))) {
            return true;
        }
        return platform.getRegionContainer().createQuery().testBuild(BukkitAdapter.adapt(at), localPlayer);
    }
}
