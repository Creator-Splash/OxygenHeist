package com.creatorsplash.oxygenheist.application.common.math;

public record Position3(double x, double y, double z) {
    public double distanceSquared(Position3 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return (dx * dx) + (dy * dy) + (dz * dz);
    }
}
