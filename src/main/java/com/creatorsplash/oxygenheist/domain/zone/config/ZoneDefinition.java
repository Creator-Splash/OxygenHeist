package com.creatorsplash.oxygenheist.domain.zone.config;

import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import org.checkerframework.checker.units.qual.C;

/**
 * Immutable definition of a capture zone
 *
 * <p>Two zone shapes are supported:</p>
 * <ul>
 *   <li>{@link Cuboid} - AABB check on all three axes, defined by two corner points</li>
 *   <li>{@link Circle} - horizontal cylinder check on XZ axes only, defined by center + radius,
 *       Y is ignored - any player within the radius column regardless of height is inside</li>
 * </ul>
 */
public sealed interface ZoneDefinition {

    String id();
    String displayName();
    String worldName();

    /**
     * @return true if the given world-space position is inside this zone
     */
    boolean contains(FullPosition pos);

    /**
     * Converts this definition into a fresh runtime {@link CaptureZoneState}
     */
    default CaptureZoneState toRuntimeState() {
        return new CaptureZoneState(this);
    }

    /**
     * A zone defined by two corner points - containment uses a full 3D AABB check
     */
    record Cuboid(
        String id,
        String displayName,
        String worldName,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ
    ) implements ZoneDefinition {
        @Override
        public boolean contains(FullPosition pos) {
            return pos.world().equals(worldName)
                && pos.x() >= minX && pos.x() <= maxX
                && pos.y() >= minY && pos.y() <= maxY
                && pos.z() >= minZ && pos.z() <= maxZ;
        }
    }

    /**
     * A zone defined by a center point and radius - containment uses an XZ-only
     * horizontal distance check, making this a vertical cylinder of infinite height
     */
    record Circle(
        String id,
        String displayName,
        String worldName,
        double centerX, double centerY, double centerZ,
        double radius
    ) implements ZoneDefinition {
        @Override
        public boolean contains(FullPosition pos) {
            if (!pos.world().equals(worldName)) return false;
            double dx = pos.x() - centerX;
            double dz = pos.z() - centerZ;
            return (dx * dx + dz * dz) <= (radius * radius);
        }
    }

}
