package com.creatorsplash.oxygenheist.platform.paper.util;

import com.creatorsplash.oxygenheist.application.common.math.Position3;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@UtilityClass
public class LocationUtils {

    public Position3 toPos3(Location loc) {
        return new Position3(loc.getX(), loc.getY(), loc.getZ());
    }

    public Location toLoc(World world, Position3 pos) {
        return new Location(world, pos.x(), pos.y(), pos.z());
    }

    public Location centerOf(ZoneDefinition def) {
        World world = Bukkit.getWorld(def.worldName());
        if (world == null) return null;
        return switch (def) {
            case ZoneDefinition.Circle c ->
                new Location(world, c.centerX(), c.centerY(), c.centerZ());
            case ZoneDefinition.Cuboid c ->
                new Location(world,
                    (c.minX() + c.maxX() + 1.0) / 2.0,
                    (c.minY() + c.maxY() + 1.0) / 2.0,
                    (c.minZ() + c.maxZ() + 1.0) / 2.0);
        };
    }

    public double radiusOf(ZoneDefinition def) {
        return switch (def) {
            case ZoneDefinition.Circle c -> c.radius();
            case ZoneDefinition.Cuboid c -> Math.max(
                (c.maxX() - c.minX()) / 2.0,
                (c.maxZ() - c.minZ()) / 2.0);
        };
    }

}
