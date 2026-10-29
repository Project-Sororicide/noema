package dev.pupwired.noema.api;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player right-clicks an NPC. Other plugins (shops, quests, ...) can listen
 * and filter on {@link #getAction()}. Cancelling it stops the configured messages and commands.
 */
public final class NpcInteractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Entity npc;
    private final String npcType;
    private final String action;
    private boolean cancelled;

    public NpcInteractEvent(Player player, Entity npc, String npcType, String action) {
        this.player = player;
        this.npc = npc;
        this.npcType = npcType;
        this.action = action;
    }

    public Player getPlayer() { return player; }

    public Entity getNpc() { return npc; }

    /** The NPC type id from npcs.yml. */
    public String getNpcType() { return npcType; }

    /** The "event" string from the type's on-interact section (e.g. "shop", "quest"). */
    public String getAction() { return action; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
