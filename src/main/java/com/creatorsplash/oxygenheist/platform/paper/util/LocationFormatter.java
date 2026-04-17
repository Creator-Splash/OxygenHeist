package com.creatorsplash.oxygenheist.platform.paper.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;

@UtilityClass
public class LocationFormatter {

    /**
     * Formats a location as {@code world (x, y, z)}
     *
     * <p>Example: {@code world (12, 64, -30)}</p>
     */
    public static String full(Location loc) {
        return loc.getWorld().getName() +
            " (" + loc.getBlockX() +
            ", " + loc.getBlockY() +
            ", " + loc.getBlockZ() + ")";
    }

    /**
     * Formats a location as {@code (x, y, z)} without the world name
     */
    public static String coords(Location loc) {
        return "(" + loc.getBlockX() +
            ", " + loc.getBlockY() +
            ", " + loc.getBlockZ() + ")";
    }

    /**
     * Formats a location as {@code (x, z)} for flat/top-down contexts
     */
    public static String coordsXZ(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockZ() + ")";
    }

}
