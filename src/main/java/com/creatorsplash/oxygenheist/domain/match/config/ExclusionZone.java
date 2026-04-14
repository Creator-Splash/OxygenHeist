package com.creatorsplash.oxygenheist.domain.match.config;

public record ExclusionZone(
    String id,
    String world,
    double minX, double minZ,
    double maxX, double maxZ
) {
    public boolean contains(double x, double z, String worldName) {
        return worldName.equals(world)
            && x >= minX && x <= maxX
            && z >= minZ && z <= maxZ;
    }
}
