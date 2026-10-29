package dev.pupwired.noema.spawn;

import dev.pupwired.noema.NoemaPlugin;
import dev.pupwired.noema.NpcRegistry;
import dev.pupwired.noema.config.NpcType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Event-driven spawner: no clock. A placement is live exactly while its chunk's entities are loaded.
 * Main thread only.
 */
public final class NpcSpawner implements Listener {

    private final NoemaPlugin plugin;
    private final NamespacedKey placementKey;
    private PlacementIndex index;

    /** placement id -> entity uuid, for NPCs that currently exist. */
    private final Map<String, UUID> spawned = new HashMap<>();
    private final Set<String> warnedTypes = new HashSet<>();

    public NpcSpawner(NoemaPlugin plugin) {
        this.plugin = plugin;
        this.placementKey = new NamespacedKey(plugin, "npc_placement");
        this.index = PlacementIndex.load(plugin, plugin.config());
        plugin.getLogger().info("Loaded " + index.size() + " NPC placements.");
    }

    /** Call after registering this listener. Picks up chunks that were already loaded. */
    public void start() {
        Bukkit.getScheduler().runTask(plugin, this::scanLoadedChunks);
    }

    /** Re-read placements and respawn everything (used by /noema reload). */
    public void reload() {
        despawnAll();
        warnedTypes.clear();
        index = PlacementIndex.load(plugin, plugin.config());
        plugin.getLogger().info("Loaded " + index.size() + " NPC placements.");
        scanLoadedChunks();
    }

    public void shutdown() {
        despawnAll();
    }

    // ------------------------------------------------------------------
    // Placement management (used by /npcs place and /npcs remove)
    // ------------------------------------------------------------------

    public boolean hasPlacement(String id) {
        return index.has(id);
    }

    public Collection<String> placementIds() {
        return index.ids();
    }

    public int placementCount() {
        return index.size();
    }

    /** First unused "<type>_<n>" id. */
    public String nextId(String typeId) {
        int n = 1;
        while (index.has(typeId + "_" + n)) n++;
        return typeId + "_" + n;
    }

    /** Saves the placement, adds it to the index and spawns it right away if its chunk is loaded. */
    public boolean addPlacement(Placement p) {
        if (index.has(p.id())) return false;
        if (!PlacementIndex.writeEntry(plugin, p)) return false;
        index.add(p);

        World world = Bukkit.getWorld(p.worldName());
        if (world != null) {
            spawnChunk(world, (int) p.chunkKey(), (int) (p.chunkKey() >>> 32));
        }
        return true;
    }

    /** Removes the placement from the file and index and despawns its NPC if it exists. */
    public boolean removePlacement(String id) {
        if (!index.has(id)) return false;
        if (!PlacementIndex.deleteEntry(plugin, id)) return false;
        index.remove(id);

        UUID uuid = spawned.remove(id);
        if (uuid != null) {
            plugin.registry().remove(uuid);
            Entity ent = Bukkit.getEntity(uuid);
            if (ent != null) ent.remove();
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        // Orphan cleanup: NPCs are non-persistent, so any tagged entity coming off disk is a stray.
        for (Entity ent : e.getEntities()) {
            if (!(ent instanceof Player)
                    && ent.getPersistentDataContainer().has(plugin.typeKey(), PersistentDataType.STRING)) {
                ent.remove();
            }
        }

        Chunk chunk = e.getChunk();
        World world = e.getWorld();
        int cx = chunk.getX(), cz = chunk.getZ();
        if (index.inChunk(world.getName(), NpcRegistry.chunkKey(cx, cz)).isEmpty()) return;

        // Spawn next tick instead of inside the load event.
        Bukkit.getScheduler().runTask(plugin, () -> spawnChunk(world, cx, cz));
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent e) {
        for (Entity ent : e.getEntities()) {
            String pid = ent.getPersistentDataContainer().get(placementKey, PersistentDataType.STRING);
            if (pid == null) continue;
            plugin.untrack(ent);
            spawned.remove(pid, ent.getUniqueId());
        }
    }

    /** Backup for the above: guarantees bookkeeping is cleared even if the entity list was incomplete. */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        Chunk c = e.getChunk();
        for (Placement p : index.inChunk(c.getWorld().getName(), NpcRegistry.chunkKey(c.getX(), c.getZ()))) {
            UUID id = spawned.remove(p.id());
            if (id != null) plugin.registry().remove(id);
        }
    }

    // ------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------

    private void scanLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (long key : index.chunks(world.getName()).keySet()) {
                int cx = (int) key;
                int cz = (int) (key >>> 32);
                if (!world.isChunkLoaded(cx, cz)) continue;
                // If entities aren't loaded yet, EntitiesLoadEvent will handle this chunk.
                if (!world.getChunkAt(cx, cz, false).isEntitiesLoaded()) continue;
                spawnChunk(world, cx, cz);
            }
        }
    }

    private void spawnChunk(World world, int cx, int cz) {
        if (!world.isChunkLoaded(cx, cz)) return;

        for (Placement p : index.inChunk(world.getName(), NpcRegistry.chunkKey(cx, cz))) {
            if (spawned.containsKey(p.id())) continue;

            NpcType type = plugin.config().types().get(p.typeId());
            if (type == null) {
                if (warnedTypes.add(p.typeId())) {
                    plugin.getLogger().warning("Placement '" + p.id() + "' uses unknown type '" + p.typeId() + "'.");
                }
                continue;
            }
            spawn(world, p, type);
        }
    }

    private void spawn(World world, Placement p, NpcType type) {
        Entity npc;
        try {
            npc = MannequinFactory.spawn(new Location(world, p.x(), p.y(), p.z(), p.yaw(), p.pitch()), type);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to spawn NPC '" + p.id() + "'", ex);
            return;
        }
        npc.getPersistentDataContainer().set(placementKey, PersistentDataType.STRING, p.id());
        plugin.track(npc, type.id());
        spawned.put(p.id(), npc.getUniqueId());
    }

    private void despawnAll() {
        for (UUID id : spawned.values()) {
            plugin.registry().remove(id);
            Entity ent = Bukkit.getEntity(id);
            if (ent != null) ent.remove();
        }
        spawned.clear();
    }
}