package dev.pupwired.noema.command;

import dev.pupwired.noema.NoemaPlugin;
import dev.pupwired.noema.message.NoemaMsg;
import dev.pupwired.noema.spawn.NpcSpawner;
import dev.pupwired.noema.spawn.Placement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;


/**
 * /noema reload
 * /noema place <type> [id]   - places an NPC at your position, facing your direction
 * /noema remove <id>
 *
 * Permission is enforced by the command's permission in plugin.yml (noema.admin).
 */

public class NoemaCommand implements CommandExecutor, TabCompleter {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,40}");
    private static final List<String> SUBCOMMANDS = List.of("reload", "place", "remove");

    private final NoemaPlugin plugin;

    public NoemaCommand(NoemaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            usage(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "place" -> place(sender, label, args);
            case "remove" -> remove(sender, label, args);
            default -> usage(sender, label);
        }
        return true;
    }

    // ------------------------------------------------------------------

    private void reload(CommandSender sender) {
        plugin.reload();
        NoemaMsg.COMMAND_RELOADED.sendMessage(sender, plugin.config().types().size(), plugin.spawner().placementCount());
    }

    private void place(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            NoemaMsg.COMMAND_NPCPLACE_NOT_PLAYER.sendMessage(sender);
            return;
        }
        if (args.length < 2 || args.length > 3) {
            NoemaMsg.COMMAND_NPCPLACE_USAGE.sendMessage(sender, label);
            return;
        }

        String typeId = args[1];
        if (!plugin.config().types().containsKey(typeId)) {
            NoemaMsg.COMMAND_NPCPLACE_UNKNOWN_TYPE.sendMessage(sender, typeId, String.join(", ", new TreeSet<>(plugin.config().types().keySet())));
            return;
        }

        NpcSpawner spawner = plugin.spawner();
        String id = args.length == 3
                ? args[2].toLowerCase(Locale.ROOT)
                : spawner.nextId(typeId.toLowerCase(Locale.ROOT));

        if (!ID.matcher(id).matches()) {
            NoemaMsg.COMMAND_NPCPLACE_ID_NAME_INVALID.sendMessage(sender, label, typeId);
            return;
        }
        if (spawner.hasPlacement(id)) {
            NoemaMsg.COMMAND_NPCPLACE_ID_EXISTS.sendMessage(sender, id);
            return;
        }

        Location loc = player.getLocation();
        Placement placement = Placement.of(id, typeId, loc.getWorld().getName(),
                round(loc.getX(), 2), round(loc.getY(), 2), round(loc.getZ(), 2),
                (float) round(loc.getYaw(), 1), 0f);

        if (!spawner.addPlacement(placement)) {
            NoemaMsg.COMMAND_PLACEMENTS_SAVE_FAILED.sendMessage(sender);
            return;
        }
        NoemaMsg.COMMAND_NPCPLACE_SUCCESS.sendMessage(sender, id, typeId, placement.x(), placement.y(), placement.z());
    }

    private void remove(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            NoemaMsg.COMMAND_NPCREMOVE_USAGE.sendMessage(sender, label);
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        NpcSpawner spawner = plugin.spawner();

        if (!spawner.hasPlacement(id)) {
            NoemaMsg.COMMAND_NPCREMOVE_NO_SUCH_ID.sendMessage(sender, id);
        } else if (!spawner.removePlacement(id)) {
            NoemaMsg.COMMAND_PLACEMENTS_SAVE_FAILED.sendMessage(sender);
        } else {
            NoemaMsg.COMMAND_NPCREMOVE_SUCCESS.sendMessage(sender, id);

        }
    }

    // ------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(SUBCOMMANDS, args[0]);
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "place": return filter(plugin.config().types().keySet(), args[1]);
                case "remove": return filter(plugin.spawner().placementIds(), args[1]);
                default: break;
            }
        }
        return List.of();
    }

    private static List<String> filter(Collection<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(p)) out.add(option);
        }
        out.sort(String::compareTo);
        return out;
    }

    private static void usage(CommandSender sender, String label) {
        NoemaMsg.COMMAND_USAGE.sendMessage(sender, label);
    }

    private static double round(double v, int places) {
        double f = Math.pow(10, places);
        return Math.round(v * f) / f;
    }

}
