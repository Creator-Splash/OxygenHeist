package com.creatorsplash.oxygenheist.domain.zone;

import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.Position3;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the runtime state of a capture zone within a match
 *
 * <p>Tracks ownership, progress, and which team is currently attempting capture</p>
 * <p>It is mutable and updated throughout the match</p>
 */
@Getter
public class CaptureZoneState {

    private final String id;

    private final String worldName;

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

    private final Map<String, ZoneTeamOxygenState> zoneOxygen = new HashMap<>();

    /* Constructor */

    public CaptureZoneState(
        String id, String worldName,
        Position3 min,
        Position3 max
    ) {
        this.id = id;
        this.worldName = worldName;

        this.minX = Math.min(min.x(), max.x());
        this.minY = Math.min(min.y(), max.y());
        this.minZ = Math.min(min.z(), max.z());

        this.maxX = Math.max(min.x(), max.x());
        this.maxY = Math.max(min.y(), max.y());
        this.maxZ = Math.max(min.z(), max.z());
    }

    /**
     * Creates a cuboid zone from a center point and radius
     */
    public static CaptureZoneState fromCenterRadius(
        String id, String worldName,
        Position3 center,
        double radius
    ) {
        return new CaptureZoneState(
            id, worldName,
            new Position3(center.x() - radius, center.y() - radius, center.z() - radius),
            new Position3(center.x() + radius, center.y() + radius, center.z() + radius)
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
     * Gets or creates zone oxygen state for a team
     *
     * @param teamId the team id
     * @return the teams zone oxygen state
     */
    public ZoneTeamOxygenState getOrCreateZoneOxygen(String teamId) {
        return zoneOxygen.computeIfAbsent(teamId, ignored -> new ZoneTeamOxygenState());
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
    public boolean isInside(FullPosition position) {
        return isInside(position.world(), position.x(), position.y(), position.z());
    }

    /**
     * Checks whether a position is inside this zone
     */
    public boolean isInside(String world, double x, double y, double z) {
        return world.equals(worldName)
            && x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    /* Snapshot */

    /**
     * @return an immutable read-only snapshot of the state
     */
    public ZoneSnapshot toSnapshot() {
        Map<String, Double> teamOxygen = new HashMap<>();
        for (Map.Entry<String, ZoneTeamOxygenState> entry : getZoneOxygen().entrySet()) {
            teamOxygen.put(entry.getKey(), entry.getValue().getOxygenPercent());
        }

        return new ZoneSnapshot(
            getId(),
            getOwnerTeamId(),
            getCaptureProgress(),
            teamOxygen
        );
    }

}
