package dev.pupwired.noema;

import dev.pupwired.noema.behaviour.LookTask;
import dev.pupwired.noema.behaviour.NpcListener;
import dev.pupwired.noema.behaviour.PassiveMessageTask;
import dev.pupwired.noema.command.NoemaCommand;
import dev.pupwired.noema.config.NpcConfig;
import dev.pupwired.noema.config.NpcType;
import dev.pupwired.noema.spawn.NpcSpawner;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class NoemaPlugin extends JavaPlugin {
    private static NoemaPlugin instance;
    private NamespacedKey typeKey;
    private final NpcRegistry registry = new NpcRegistry();
    private volatile NpcConfig config;
    private PassiveMessageTask passiveTask;
    private LookTask lookTask;
    private NpcSpawner spawner;
    private NpcListener npcListener;

    @Override
    public void onEnable() {
        // Common
        instance = this;
        typeKey = new NamespacedKey(this, "npc_type");
        config = NpcConfig.load(this);

        // Listeners
        spawner = new NpcSpawner(this);
        npcListener = new NpcListener(this);

        getServer().getPluginManager().registerEvents(spawner, this);
        getServer().getPluginManager().registerEvents(npcListener, this);

        spawner.start();

        // Tasks
        passiveTask = new PassiveMessageTask(this);
        lookTask = new LookTask(this);

        getServer().getScheduler().runTaskTimer(this, passiveTask, 40L, 20L);
        getServer().getScheduler().runTaskTimer(this, lookTask, 40L, 5L);

        // Commands
        NoemaCommand noemaCommand = new NoemaCommand(this);
        getCommand("noema").setExecutor(noemaCommand);
        getCommand("noema").setTabCompleter(noemaCommand);

        // End
        getLogger().info("Loaded " + config.types().size() + " NPC types.");
    }

    @Override
    public void onDisable() {
        if (spawner != null) spawner.shutdown();
    }

    public void reload() {
        config = NpcConfig.load(this);
        spawner.reload();
    }

    // TODO: Spawner Logic

    /** Tag an entity as an NPC of the given type and start tracking it. */
    public void track(Entity entity, String typeId) {
        NpcType type = config.types().get(typeId);
        entity.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, typeId);
        entity.setPersistent(false);
        if (type != null) entity.setInvulnerable(type.invulnerable());
        registry.add(entity, typeId);
    }

    /** Stop tracking an NPC entity (call on despawn or chunk unload). */
    public void untrack(Entity entity) {
        registry.remove(entity.getUniqueId());
    }

    public static NoemaPlugin getInstance() {
        return instance;
    }

    public NamespacedKey typeKey() { return typeKey; }

    public NpcConfig config() { return config; }

    public NpcRegistry registry() { return registry; }

    public PassiveMessageTask passiveTask() { return passiveTask; }

    public NpcSpawner spawner() { return spawner; }
}
