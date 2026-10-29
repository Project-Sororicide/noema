package dev.pupwired.noema.spawn;

import dev.pupwired.noema.NpcRegistry;
import dev.pupwired.noema.config.NpcConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Placements from placements.yml, indexed world name -> chunk key -> placements.
 * Built once on load and never mutated afterwards.
 */
public final class PlacementIndex {

    private final Map<String, Map<Long, List<Placement>>> byWorld;
    private final int count;

    private PlacementIndex(Map<String, Map<Long, List<Placement>>> byWorld, int count) {
        this.byWorld = byWorld;
        this.count = count;
    }

    public static PlacementIndex load(JavaPlugin plugin, NpcConfig config) {
        Logger log = plugin.getLogger();
        File file = new File(plugin.getDataFolder(), "placements.yml");
        if (!file.exists()) plugin.saveResource("placements.yml", false);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        Map<String, Map<Long, List<Placement>>> map = new HashMap<>();
        int count = 0;

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

                double x = s.getDouble("x"), y = s.getDouble("y"), z = s.getDouble("z");
                float yaw = (float) s.getDouble("yaw", 0.0);
                float pitch = (float) s.getDouble("pitch", 0.0);
                long key = NpcRegistry.chunkKey(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4);

                map.computeIfAbsent(world, w -> new HashMap<>())
                        .computeIfAbsent(key, k -> new ArrayList<>(2))
                        .add(new Placement(id, type, world, x, y, z, yaw, pitch, key));
                count++;
            }
        }
        return new PlacementIndex(map, count);
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
        return count;
    }
}