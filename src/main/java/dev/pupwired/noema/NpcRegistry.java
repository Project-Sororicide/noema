package dev.pupwired.noema;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight index of currently spawned NPCs, keyed by world and chunk so the
 * passive-message task only looks at chunks near each player.
 * Main thread only; no synchronization.
 */
public final class NpcRegistry {

    public record Entry(UUID entityId, UUID worldId, long chunkKey, String typeId,
                        double x, double y, double z) {}

    private final Map<UUID, Map<Long, List<Entry>>> worlds = new HashMap<>();
    private final Map<UUID, Entry> byEntity = new HashMap<>();

    public static long chunkKey(int cx, int cz) {
        return ((long) cz << 32) | (cx & 0xFFFFFFFFL);
    }

    public void add(Entity entity, String typeId) {
        remove(entity.getUniqueId()); // re-tracking replaces the old entry
        Location loc = entity.getLocation();
        UUID worldId = loc.getWorld().getUID();
        long key = chunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        Entry entry = new Entry(entity.getUniqueId(), worldId, key, typeId,
                loc.getX(), loc.getY(), loc.getZ());
        byEntity.put(entry.entityId(), entry);
        worlds.computeIfAbsent(worldId, w -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>(2))
                .add(entry);
    }

    public Entry get(UUID entityId) {
        return byEntity.get(entityId);
    }

    public void remove(UUID entityId) {
        Entry entry = byEntity.remove(entityId);
        if (entry == null) return;
        Map<Long, List<Entry>> chunks = worlds.get(entry.worldId());
        if (chunks == null) return;
        List<Entry> list = chunks.get(entry.chunkKey());
        if (list == null) return;
        list.removeIf(e -> e.entityId().equals(entityId));
        if (list.isEmpty()) {
            chunks.remove(entry.chunkKey());
            if (chunks.isEmpty()) worlds.remove(entry.worldId());
        }
    }

    public List<Entry> inChunk(UUID worldId, long chunkKey) {
        Map<Long, List<Entry>> chunks = worlds.get(worldId);
        if (chunks == null) return List.of();
        List<Entry> list = chunks.get(chunkKey);
        return list == null ? List.of() : list;
    }

    public boolean isEmpty() {
        return byEntity.isEmpty();
    }

    public int size() {
        return byEntity.size();
    }
}
