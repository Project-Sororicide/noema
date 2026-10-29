package dev.pupwired.noema.behaviour;

import dev.pupwired.noema.NoemaPlugin;
import dev.pupwired.noema.NpcRegistry;
import dev.pupwired.noema.NpcRegistry.Entry;
import dev.pupwired.noema.config.NpcConfig;
import dev.pupwired.noema.config.NpcType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One global task (not one per NPC). For each player it only inspects NPCs in nearby chunks,
 * compares squared distance, and rate-limits per player per NPC.
 */
public final class PassiveMessageTask implements Runnable {

    private final NoemaPlugin plugin;
    /** player -> (npc entity id -> millis of last passive message). */
    private final Map<UUID, Map<UUID, Long>> lastSaid = new HashMap<>();

    public PassiveMessageTask(NoemaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        NpcRegistry registry = plugin.registry();
        if (registry.isEmpty()) return;

        NpcConfig config = plugin.config();
        int radius = config.passiveChunkRadius();
        if (radius <= 0 && config.types().values().stream().noneMatch(t -> t.passive() != null)) return;

        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            UUID worldId = loc.getWorld().getUID();
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            double px = loc.getX(), py = loc.getY(), pz = loc.getZ();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    List<Entry> entries = registry.inChunk(worldId, NpcRegistry.chunkKey(cx + dx, cz + dz));
                    for (Entry entry : entries) {
                        NpcType type = config.types().get(entry.typeId());
                        if (type == null || type.passive() == null) continue;
                        NpcType.Passive passive = type.passive();

                        double ddx = entry.x() - px, ddy = entry.y() - py, ddz = entry.z() - pz;
                        if (ddx * ddx + ddy * ddy + ddz * ddz > passive.rangeSq()) continue;

                        Map<UUID, Long> seen = lastSaid.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
                        Long last = seen.get(entry.entityId());
                        if (last != null && now - last < passive.intervalMillis()) continue;
                        seen.put(entry.entityId(), now);

                        player.sendMessage(passive.messages().random().render(player.getName()));
                    }
                }
            }
        }
    }

    public void forget(UUID playerId) {
        lastSaid.remove(playerId);
    }
}
