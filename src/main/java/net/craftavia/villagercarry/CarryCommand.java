package net.craftavia.villagercarry;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** /villagercarry reload */
final class CarryCommand implements BasicCommand {

    static final String ADMIN = "villagercarry.admin";

    private final VillagerCarryPlugin plugin;

    CarryCommand(final VillagerCarryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final CommandSourceStack source, final String[] args) {
        final CommandSender sender = source.getSender();
        if (!sender.hasPermission(ADMIN)) {
            Messages.send(sender, "no-permission");
            return;
        }
        if (args.length == 1 && args[0].toLowerCase(Locale.ROOT).equals("reload")) {
            plugin.reload(sender);
            return;
        }
        Messages.send(sender, "usage");
    }

    @Override
    public @NotNull Collection<String> suggest(final CommandSourceStack source, final String[] args) {
        if (args.length <= 1 && source.getSender().hasPermission(ADMIN)) {
            final String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return "reload".startsWith(prefix) ? List.of("reload") : List.of();
        }
        return List.of();
    }

    @Override
    public boolean canUse(final CommandSender sender) {
        return sender.hasPermission(ADMIN);
    }
}
