package com.creatorsplash.oxygenheist.domain.team;

/**
 * Immutable base spawn location for a team
 */
public record TeamBase(
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) {
}
