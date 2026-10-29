package dev.pupwired.noema.spawn;

import dev.pupwired.noema.NpcRegistry;

/** One NPC placement: which type goes where. Pure data. */
public record Placement(
        String id,
        String typeId,
        String worldName,
        double x, double y, double z,
        float yaw, float pitch,
        long chunkKey) {

    /** Builds a placement and derives its chunk key from the coordinates. */
    public static Placement of(String id, String typeId, String worldName,
                               double x, double y, double z, float yaw, float pitch) {
        long key = NpcRegistry.chunkKey(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4);
        return new Placement(id, typeId, worldName, x, y, z, yaw, pitch, key);
    }
}