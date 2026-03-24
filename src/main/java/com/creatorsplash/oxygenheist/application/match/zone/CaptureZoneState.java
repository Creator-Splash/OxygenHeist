package com.creatorsplash.oxygenheist.application.match.zone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the runtime state of a captuire zone within a match
 *
 * <p>Tracks ownership, progress, and which team is currently attempting capture</p>
 * <p>It is mutable and updated throughout the match</p>
 */
@Getter
public class CaptureZoneState {

    private final String id;

    /* Min Point/Corner */
    private final double minX;
    private final double minY;
    private final double minZ;

    /* Max Point/Corner */
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    private String ownerTeamId;
    private String capturingTeamId;

    private double captureProgress = 0.0;

    // TODO zone team oxy state

    /* Constructor */

    public CaptureZoneState(
        String id,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ
    ) {
        this.id = id;

        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);

        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    /**
     * Creates a cuboid zone from a center point and radius
     */
    public static CaptureZoneState fromCenterRadius(
        String id,
        double x, double y, double z,
        double radius
    ) {
        return new CaptureZoneState(
            id,
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
    }

    /* == Domain == */

    /**
     * @return true is the zone currently has an owner
     */
    public boolean hasOwner() {
        return ownerTeamId != null;
    }

    /**
     * Progresses capture for a team
     */
    public void progressCapture(String teamId, double amount) {
        this.capturingTeamId = teamId;
        this.captureProgress = Math.min(100.0, this.captureProgress + amount);
    }

    /**
     * Regresses capture due to an opposing team
     */
    public void regressCapture(String teamId, double amount) {
        this.capturingTeamId = teamId;

        if (this.captureProgress > 0) {
            this.captureProgress = Math.max(0.0, this.captureProgress - amount);
        } else {
            this.ownerTeamId = null;
            this.captureProgress = 0.0;
        }
    }

    /**
     * Completes capture and assigns ownership
     */
    public void completeCapture() {
        this.ownerTeamId = this.capturingTeamId;
        this.captureProgress = 100.0;
    }

    /**
     * @return true if capture is complete
     */
    public boolean isFullyCaptured() {
        return this.captureProgress >= 100.0;
    }

    /**
     * Checks whether a position is inside this zone
     */
    public boolean isInside(double x, double y, double z) {
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

}
