package dev.pupwired.noema;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Immutable, shared definition of an NPC kind. Every NPC instance only stores a type id.
 */
public record NpcType(
        String id,
        Component name,
        String skinId,
        Skin skin,              // nullable (unknown/missing skin id)
        int cooldownTicks,
        boolean invulnerable,
        boolean immovable,
        Interact interact,      // nullable
        Passive passive         // nullable
) {

    public record Skin(String value, String signature) {}

    public enum RunAs { CONSOLE, PLAYER }

    /** A pre-parsed message. Only %player% is substituted at send time. */
    public record Line(Component component, boolean hasPlayer) {
        public Component render(String playerName) {
            if (!hasPlayer) return component;
            return component.replaceText(b -> b.matchLiteral("%player%").replacement(playerName));
        }
    }

    public record Lines(Line[] lines) {
        public boolean isEmpty() {
            return lines.length == 0;
        }

        public Line random() {
            return lines[ThreadLocalRandom.current().nextInt(lines.length)];
        }
    }

    public record Interact(String event, RunAs runAs, List<String> commands, Lines messages) {}

    public record Passive(double range, double rangeSq, long intervalMillis, Lines messages) {}
}

