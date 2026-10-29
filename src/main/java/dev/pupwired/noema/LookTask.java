package dev.pupwired.noema;

import dev.pupwired.noema.NpcRegistry.Entry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Turns NPCs toward the nearest player in range, then back to their home rotation. */
public final class LookTask implements Runnable {

    private static final float MAX_STEP = 30f;   // degrees per run
    private static final float DEADZONE = 4f;    // skip tiny corrections
    private static final float MAX_PITCH = 45f;

    private record Target(Entry entry, double px, double py, double pz, double distSq) {}

    private final NoemaPlugin plugin;
    private final Map<UUID, Target> targets = new HashMap<>();
    private final Set<UUID> turned = new HashSet<>();

    public LookTask(NoemaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        NpcConfig config = plugin.config();
        NpcRegistry registry = plugin.registry();
        int radius = config.lookChunkRadius();
        if (radius < 0 || registry.isEmpty()) return;

        // Find the nearest player for each NPC that has one in range.
        targets.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            Location loc = p.getLocation();
            UUID worldId = loc.getWorld().getUID();
            int cx = loc.getBlockX() >> 4, cz = loc.getBlockZ() >> 4;
            double px = loc.getX(), pz = loc.getZ();
            double eyeY = loc.getY() + p.getEyeHeight();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (Entry entry : registry.inChunk(worldId, NpcRegistry.chunkKey(cx + dx, cz + dz))) {
                        NpcType type = config.types().get(entry.typeId());
                        if (type == null || type.look() == null) continue;

                        double ddx = entry.x() - px, ddy = entry.y() - loc.getY(), ddz = entry.z() - pz;
                        double distSq = ddx * ddx + ddy * ddy + ddz * ddz;
                        if (distSq > type.look().rangeSq()) continue;

                        Target cur = targets.get(entry.entityId());
                        if (cur == null || distSq < cur.distSq()) {
                            targets.put(entry.entityId(), new Target(entry, px, eyeY, pz, distSq));
                        }
                    }
                }
            }
        }

        // Turn each NPC toward its target.
        for (Target t : targets.values()) {
            Entity npc = Bukkit.getEntity(t.entry().entityId());
            if (npc == null) continue;

            double dx = t.px() - t.entry().x();
            double dz = t.pz() - t.entry().z();
            double dy = t.py() - (t.entry().y() + 1.6);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            pitch = clamp(pitch, -MAX_PITCH, MAX_PITCH);

            turn(npc, yaw, pitch);
            turned.add(t.entry().entityId());
        }
    }

    /** Rotates one capped step toward the target. Returns true when already close enough. */
    private static boolean turn(Entity npc, float targetYaw, float targetPitch) {
        Location l = npc.getLocation();
        float dYaw = wrap(targetYaw - l.getYaw());
        float dPitch = targetPitch - l.getPitch();
        if (Math.abs(dYaw) < DEADZONE && Math.abs(dPitch) < DEADZONE) return true;

        npc.setRotation(
                l.getYaw() + clamp(dYaw, -MAX_STEP, MAX_STEP),
                l.getPitch() + clamp(dPitch, -MAX_STEP, MAX_STEP));
        return false;
    }

    private static float wrap(float d) {
        d %= 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}