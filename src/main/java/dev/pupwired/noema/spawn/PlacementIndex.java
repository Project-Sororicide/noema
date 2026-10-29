package dev.pupwired.noema.spawn;

import dev.pupwired.noema.config.NpcConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Placements from placements.yml, indexed by id and by world -> chunk key.
 * Mutated only from the main thread (add/remove via commands).
 */
public final class PlacementIndex {

    private final Map<String, Map<Long, List<Placement>>> byWorld = new HashMap<>();
    private final Map<String, Placement> byId = new HashMap<>();

    private PlacementIndex() {}

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    public static PlacementIndex load(JavaPlugin plugin, NpcConfig config) {
        Logger log = plugin.getLogger();
        File file = file(plugin);
        if (!file.exists()) plugin.saveResource("placements.yml", false);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        PlacementIndex index = new PlacementIndex();

        ConfigurationSection sec = yml.getConfigurationSection("placements");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection s = sec.getConfigurationSection(id);
                if (s == null) continue;

                String type = s.getString("type");
                String world = s.getString("world");
                if (type == null || world == null || !s.contains("x") || !s.contains("y") || !s.contains("z")) {
                    log.warning("placements.yml: '" + id + "' needs type, world, x, y and z; skipping it.");
                    continue;
                }
                if (!config.types().containsKey(type)) {
                    log.warning("placements.yml: '" + id + "' uses unknown type '" + type + "'.");
                }

                index.add(Placement.of(id, type, world,
                        s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                        (float) s.getDouble("yaw", 0.0), (float) s.getDouble("pitch", 0.0)));
            }
        }
        return index;
    }

    // ------------------------------------------------------------------
    // In-memory access
    // ------------------------------------------------------------------

    public void add(Placement p) {
        byId.put(p.id(), p);
        byWorld.computeIfAbsent(p.worldName(), w -> new HashMap<>())
                .computeIfAbsent(p.chunkKey(), k -> new ArrayList<>(2))
                .add(p);
    }

    public Placement remove(String id) {
        Placement p = byId.remove(id);
        if (p == null) return null;
        Map<Long, List<Placement>> chunks = byWorld.get(p.worldName());
        if (chunks != null) {
            List<Placement> list = chunks.get(p.chunkKey());
            if (list != null) {
                list.removeIf(x -> x.id().equals(id));
                if (list.isEmpty()) {
                    chunks.remove(p.chunkKey());
                    if (chunks.isEmpty()) byWorld.remove(p.worldName());
                }
            }
        }
        return p;
    }

    public Placement get(String id) {
        return byId.get(id);
    }

    public boolean has(String id) {
        return byId.containsKey(id);
    }

    public Collection<String> ids() {
        return Collections.unmodifiableCollection(byId.keySet());
    }

    public List<Placement> inChunk(String world, long chunkKey) {
        Map<Long, List<Placement>> chunks = byWorld.get(world);
        if (chunks == null) return List.of();
        List<Placement> list = chunks.get(chunkKey);
        return list == null ? List.of() : list;
    }

    /** Chunk key -> placements for one world (empty if the world has none). */
    public Map<Long, List<Placement>> chunks(String world) {
        return byWorld.getOrDefault(world, Map.of());
    }

    public int size() {
        return byId.size();
    }

    // ------------------------------------------------------------------
    // File persistence (small synchronous writes, triggered only by admin commands)
    // ------------------------------------------------------------------

    public static boolean writeEntry(JavaPlugin plugin, Placement p) {
        File f = file(plugin);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        String base = "placements." + p.id();
        yml.set(base + ".type", p.typeId());
        yml.set(base + ".world", p.worldName());
        yml.set(base + ".x", p.x());
        yml.set(base + ".y", p.y());
        yml.set(base + ".z", p.z());
        yml.set(base + ".yaw", (double) p.yaw());
        yml.set(base + ".pitch", (double) p.pitch());
        return save(plugin, yml, f);
    }

    public static boolean deleteEntry(JavaPlugin plugin, String id) {
        File f = file(plugin);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        yml.set("placements." + id, null);
        return save(plugin, yml, f);
    }

    private static File file(JavaPlugin plugin) {
        return new File(plugin.getDataFolder(), "placements.yml");
    }

    private static boolean save(JavaPlugin plugin, YamlConfiguration yml, File f) {
        try {
            yml.save(f);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save placements.yml", ex);
            return false;
        }
    }
}