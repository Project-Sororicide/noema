package dev.pupwired.noema.spawn;

/** One NPC placement: which type goes where. Pure data. */
public record Placement(
        String id,
        String typeId,
        String worldName,
        double x, double y, double z,
        float yaw, float pitch,
        long chunkKey) {
}