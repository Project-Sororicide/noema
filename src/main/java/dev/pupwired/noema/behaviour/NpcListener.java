package dev.pupwired.noema.behaviour;

import dev.pupwired.noema.NoemaPlugin;
import dev.pupwired.noema.config.NpcType;
import dev.pupwired.noema.config.NpcType.Interact;
import dev.pupwired.noema.api.NpcInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcListener implements Listener {

    private final NoemaPlugin plugin;
    /** player -> server tick of last accepted interaction (shared across NPCs). */
    private final Map<UUID, Integer> lastInteract = new HashMap<>();

    public NpcListener(NoemaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        // PlayerInteractAtEntityEvent extends this event; skip it so we only handle once.
        if (e instanceof PlayerInteractAtEntityEvent) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Entity entity = e.getRightClicked();
        String typeId = entity.getPersistentDataContainer()
                .get(plugin.typeKey(), PersistentDataType.STRING);
        if (typeId == null) return;

        e.setCancelled(true);

        NpcType type = plugin.config().types().get(typeId);
        if (type == null || type.interact() == null) return;
        Interact interact = type.interact();

        Player player = e.getPlayer();
        int now = Bukkit.getCurrentTick();
        Integer last = lastInteract.get(player.getUniqueId());
        if (last != null && now - last < type.cooldownTicks()) return;
        lastInteract.put(player.getUniqueId(), now);

        NpcInteractEvent event = new NpcInteractEvent(player, entity, type.id(), interact.event());
        if (!event.callEvent()) return; // cancelled by another plugin

        String playerName = player.getName();

        if (!interact.messages().isEmpty()) {
            player.sendMessage(interact.messages().random().render(playerName));
        }

        if (!interact.commands().isEmpty()) {
            String uuid = player.getUniqueId().toString();
            for (String raw : interact.commands()) {
                String cmd = raw.replace("%player%", playerName)
                        .replace("%uuid%", uuid)
                        .replace("%npc%", type.id());
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                Bukkit.dispatchCommand(
                        interact.runAs() == NpcType.RunAs.PLAYER ? player : Bukkit.getConsoleSender(),
                        cmd);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        lastInteract.remove(id);
        plugin.passiveTask().forget(id);
    }
}

