package dev.pupwired.noema.message;

import dev.pupwired.noema.NoemaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;

public enum NoemaMsg {
    COMMAND_USAGE("Usage: /%s <reload | place <type> [id] | remove <id>>", NoemaMsgColor.WARNING),
    COMMAND_RELOADED("Reloaded %s NPC types and %s placements.", NoemaMsgColor.PASS),
    COMMAND_NPCPLACE_USAGE("Usage: /%s place <type> [id]", NoemaMsgColor.WARNING),
    COMMAND_NPCPLACE_UNKNOWN_TYPE("Unknown type '%s'. Types: %s", NoemaMsgColor.ERROR),
    COMMAND_NPCPLACE_ID_NAME_INVALID("Ids may only use a-z, 0-9, '_' and '-' (max 40). " +
            "Pass an id explicitly: /%s place %s <id>", NoemaMsgColor.ERROR),
    COMMAND_NPCPLACE_ID_EXISTS("A placement with id '%s' already exists.", NoemaMsgColor.ERROR),
    COMMAND_PLACEMENTS_SAVE_FAILED("Couldn't save placements.yml, check the console.", NoemaMsgColor.ERROR),
    COMMAND_NPCPLACE_SUCCESS("Placed '%s' (%s) at %s, %s, %s.", NoemaMsgColor.PASS),
    COMMAND_NPCPLACE_NOT_PLAYER("Only players can place NPCs.", NoemaMsgColor.ERROR),
    COMMAND_NPCREMOVE_USAGE("Usage: /%s remove <id>", NoemaMsgColor.WARNING),
    COMMAND_NPCREMOVE_NO_SUCH_ID("No placement with id '%s'.", NoemaMsgColor.ERROR),
    COMMAND_NPCREMOVE_SUCCESS("Removed '%s'.", NoemaMsgColor.PASS),;



    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final String string;
    private final NoemaMsgColor color;

    NoemaMsg(String string, NoemaMsgColor color) {
        this.string = string;
        this.color = color;
    }

    public String getRaw() {
        return "<" + this.color + ">" + this.string + "</" + this.color + ">";
    }

    public String getRaw(Object... strings) {
        int matches = StringUtils.countMatches(getRaw(), "%s");

        if (matches != strings.length) {
            NoemaPlugin.getInstance().getLogger().severe("Mismatch in messages, incorrect parameters supplied for " + name() +
                    ". Please inform plugin author.");
        }
        return String.format(getRaw(), strings);
    }

    public Component getComponent() {
        return MM.deserialize(getRaw());
    }

    public Component getComponent(Object... strings) {
        return MM.deserialize(getRaw(strings));
    }

    public void sendMessage(CommandSender sender) {
        sender.sendMessage(getComponent());
    }

    public void sendMessage(CommandSender sender, Object... strings) {
        sender.sendMessage(getComponent(strings));
    }
}
