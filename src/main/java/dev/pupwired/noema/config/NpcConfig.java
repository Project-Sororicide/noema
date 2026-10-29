package dev.pupwired.noema.config;

import dev.pupwired.noema.config.NpcType.Interact;
import dev.pupwired.noema.config.NpcType.Line;
import dev.pupwired.noema.config.NpcType.Lines;
import dev.pupwired.noema.config.NpcType.Passive;
import dev.pupwired.noema.config.NpcType.RunAs;
import dev.pupwired.noema.config.NpcType.Skin;
import dev.pupwired.noema.config.NpcType.Look;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Immutable snapshot of everything loaded from skins.yml + npcs.yml. */
public record NpcConfig(Map<String, NpcType> types, int passiveChunkRadius, int lookChunkRadius) {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static NpcConfig load(JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        YamlConfiguration skinsYml = read(plugin, "skins.yml");
        YamlConfiguration npcsYml = read(plugin, "npcs.yml");

        // ---- skins ----
        Map<String, Skin> skins = new HashMap<>();
        ConfigurationSection skinSec = skinsYml.getConfigurationSection("skins");
        if (skinSec != null) {
            for (String id : skinSec.getKeys(false)) {
                String value = skinSec.getString(id + ".value");
                String signature = skinSec.getString(id + ".signature");
                if (value == null || value.isBlank()) {
                    log.warning("skins.yml: skin '" + id + "' has no value, ignoring it.");
                    continue;
                }
                if (signature != null && signature.isBlank()) signature = null;
                skins.put(id, new Skin(value, signature));
            }
        }

        // ---- types ----
        ConfigurationSection defaults = npcsYml.getConfigurationSection("defaults");
        ConfigurationSection typesSec = npcsYml.getConfigurationSection("types");
        Map<String, NpcType> types = new HashMap<>();
        double maxPassiveRange = 0;
        double maxLookRange = 0;

        if (typesSec != null) {
            for (String id : typesSec.getKeys(false)) {
                ConfigurationSection s = typesSec.getConfigurationSection(id);
                if (s == null) continue;

                Component name = MM.deserialize(s.getString("name", id));
                Component description = MM.deserialize(s.getString("description", ""));

                String skinId = s.getString("skin");
                Skin skin = null;
                if (skinId != null) {
                    skin = skins.get(skinId);
                    if (skin == null) {
                        log.warning("npcs.yml: type '" + id + "' references unknown skin '" + skinId + "'.");
                    }
                }

                int cooldown = getInt(s, defaults, "cooldown-ticks", 10);
                boolean invulnerable = getBool(s, defaults, "invulnerable", true);
                boolean immovable = getBool(s, defaults, "immovable", true);

                Interact interact = null;
                ConfigurationSection oi = s.getConfigurationSection("on-interact");
                if (oi != null) {
                    String event = oi.getString("event", id);
                    RunAs runAs = "player".equalsIgnoreCase(oi.getString("run-as", "console"))
                            ? RunAs.PLAYER : RunAs.CONSOLE;
                    List<String> commands = List.copyOf(oi.getStringList("commands"));
                    interact = new Interact(event, runAs, commands, lines(oi.getStringList("messages")));
                }

                Passive passive = null;
                ConfigurationSection ps = s.getConfigurationSection("passive");
                if (ps != null) {
                    Lines msgs = lines(ps.getStringList("messages"));
                    if (!msgs.isEmpty()) {
                        double range = ps.getDouble("range", 6.0);
                        long intervalMs = (long) (ps.getDouble("interval-seconds", 45.0) * 1000.0);
                        passive = new Passive(range, range * range, intervalMs, msgs);
                        maxPassiveRange = Math.max(maxPassiveRange, range);
                    }
                }

                Look look = null;
                ConfigurationSection ls = s.getConfigurationSection("look");
                if (ls != null) {
                    double range = ls.getDouble("range", 5.0);
                    look = new Look(range, range * range);
                    maxLookRange = Math.max(maxLookRange, range);
                }

                types.put(id, new NpcType(id, name, description, skinId, skin, Math.max(0, cooldown),
                        invulnerable, immovable, interact, passive, look));
            }
        }

        int chunkRadius = maxPassiveRange <= 0 ? 0 : (int) Math.ceil(maxPassiveRange / 16.0);
        int lookRadius = maxLookRange <= 0 ? -1 : (int) Math.ceil(maxLookRange / 16.0);
        return new NpcConfig(Map.copyOf(types), chunkRadius, lookRadius);
    }

    private static YamlConfiguration read(JavaPlugin plugin, String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) plugin.saveResource(name, false);
        return YamlConfiguration.loadConfiguration(f);
    }

    private static Lines lines(List<String> raw) {
        Line[] out = new Line[raw.size()];
        for (int i = 0; i < out.length; i++) {
            String str = raw.get(i);
            out[i] = new Line(MM.deserialize(str), str.contains("%player%"));
        }
        return new Lines(out);
    }

    private static int getInt(ConfigurationSection s, ConfigurationSection d, String key, int fallback) {
        if (s.contains(key)) return s.getInt(key);
        return d == null ? fallback : d.getInt(key, fallback);
    }

    private static boolean getBool(ConfigurationSection s, ConfigurationSection d, String key, boolean fallback) {
        if (s.contains(key)) return s.getBoolean(key);
        return d == null ? fallback : d.getBoolean(key, fallback);
    }
}

