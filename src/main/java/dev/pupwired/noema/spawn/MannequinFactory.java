package dev.pupwired.noema.spawn;

import com.destroystokyo.paper.profile.ProfileProperty;
import dev.pupwired.noema.config.NpcType;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;

/**
 * The only class that touches the Mannequin API, so any API mismatch is fixed in one place.
 *
 * Verify as Paper API version goes up:
 *   - Mannequin#setProfile(ResolvableProfile)
 *   - ResolvableProfile.resolvableProfile().addProperty(ProfileProperty).build()
 *   - Mannequin#setImmovable(boolean)
 * Mannequins also show a description line under the name by default; hide or set it here
 * if the API exposes a setter for it.
 */
final class MannequinFactory {

    private MannequinFactory() {}

    static Mannequin spawn(Location loc, NpcType type) {
        return loc.getWorld().spawn(loc, Mannequin.class, m -> {
            // Applied before the entity enters the world, so it is never saved.
            m.setPersistent(false);
            m.setInvulnerable(type.invulnerable());
            m.setImmovable(type.immovable());

            m.customName(type.name());
            m.setCustomNameVisible(true);

            m.setDescription(type.description());

            NpcType.Skin skin = type.skin();
            if (skin != null) {
                ProfileProperty texture = new ProfileProperty("textures", skin.value(), skin.signature());
                m.setProfile(ResolvableProfile.resolvableProfile().addProperty(texture).build());
            }
        });
    }
}