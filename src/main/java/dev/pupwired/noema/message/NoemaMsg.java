package dev.pupwired.noema.message;

import dev.pupwired.luckylib.message.Color;
import dev.pupwired.luckylib.message.LuckyMessage;

public enum NoemaMsg implements LuckyMessage {
    COMMAND_USAGE("Usage: /%s <reload | place <type> [id] | remove <id>>", Color.WARNING),
    COMMAND_RELOADED("Reloaded %s NPC types and %s placements.", Color.PASS),
    COMMAND_NPCPLACE_USAGE("Usage: /%s place <type> [id]", Color.WARNING),
    COMMAND_NPCPLACE_UNKNOWN_TYPE("Unknown type '%s'. Types: %s", Color.ERROR),
    COMMAND_NPCPLACE_ID_NAME_INVALID("Ids may only use a-z, 0-9, '_' and '-' (max 40). " +
            "Pass an id explicitly: /%s place %s <id>", Color.ERROR),
    COMMAND_NPCPLACE_ID_EXISTS("A placement with id '%s' already exists.", Color.ERROR),
    COMMAND_PLACEMENTS_SAVE_FAILED("Couldn't save placements.yml, check the console.", Color.ERROR),
    COMMAND_NPCPLACE_SUCCESS("Placed '%s' (%s) at %s, %s, %s.", Color.PASS),
    COMMAND_NPCPLACE_NOT_PLAYER("Only players can place NPCs.", Color.ERROR),
    COMMAND_NPCREMOVE_USAGE("Usage: /%s remove <id>", Color.WARNING),
    COMMAND_NPCREMOVE_NO_SUCH_ID("No placement with id '%s'.", Color.ERROR),
    COMMAND_NPCREMOVE_SUCCESS("Removed '%s'.", Color.PASS),;

    private final String string;
    private final Color color;

    NoemaMsg(String string, Color color) {
        this.string = string;
        this.color = color;
    }

    public String getRaw() {
        return "<" + this.color + ">" + this.string + "</" + this.color + ">";
    }
}
