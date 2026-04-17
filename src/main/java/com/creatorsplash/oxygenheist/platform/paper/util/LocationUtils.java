package com.creatorsplash.oxygenheist.platform.paper.util;

import com.creatorsplash.oxygenheist.application.common.math.Position3;
import lombok.experimental.UtilityClass;
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


}
