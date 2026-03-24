package com.creatorsplash.oxygenheist.application.common.math;

public record FullPosition(
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) {
    public Position3 toPos3() {
        return new Position3(x, y, z);
    }
}
