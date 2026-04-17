package com.creatorsplash.oxygenheist.platform.paper.world;

/**
 * Immutable snapshot of the configured arena geometry
 */
public record ArenaSetup(
    String worldName,
    double centerX,
    double centerZ,
    double initialSize
) {
}
