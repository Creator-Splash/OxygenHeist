package com.creatorsplash.oxygenheist.domain.zone;

import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the runtime state of a capture zone within a match
 *
 * <p>Tracks ownership, progress, and which team is currently attempting capture</p>
 * <p>It is mutable and updated throughout the match</p>
 */
@Getter
@RequiredArgsConstructor
public class CaptureZoneState {

    private final ZoneDefinition definition;

    private String ownerTeamId;
    private String capturingTeamId;

    @Setter
    private boolean contested;

    private double captureProgress = 0.0;

    private int restoreCooldownTicks = -1;

    private final Map<String, ZoneTeamOxygenState> zoneOxygen = new HashMap<>();

    /* == Identity == */

    public String getId() { return definition.id(); }
    public String getWorldName() { return definition.worldName(); }
    public String getDisplayName() { return definition.displayName(); }

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
    public void regressCapture(double amount) {
        if (this.captureProgress > 0) {
            this.captureProgress = Math.max(0.0, this.captureProgress - amount);
            // Complete regression
            if (this.captureProgress <= 0.0) {
                this.capturingTeamId = null;
            }
        } else {
            this.ownerTeamId = null;
            this.capturingTeamId = null;
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
     * Immediately strips ownership and resets capture state
     * <p>Called when the owning teams zone oxygen fully depletes</p>
     */
    public void neutralize() {
        this.ownerTeamId = null;
        this.capturingTeamId = null;
        this.captureProgress = 0.0;
        this.contested = false;
    }

    /* == Geometry == */

    /**
     * Checks whether a position is inside this zone
     */
    public boolean isInside(FullPosition position) {
        return definition.contains(position);
    }

    /**
     * Checks whether a position is inside this zone
     */
    public boolean isInside(String world, double x, double y, double z) {
        return definition.contains(new FullPosition(world, x, y, z, 0F, 0F));
    }

    /* == Restoration == */

    public void tickRestoreCooldown() {
        if (restoreCooldownTicks > 0) restoreCooldownTicks--;
    }

    public boolean isRestoreCooldownActive() { return restoreCooldownTicks >= 0; }
    public boolean isRestoreCooldownComplete() { return restoreCooldownTicks == 0; }

    public void startRestoreCooldown(int ticks) {
        if (restoreCooldownTicks < 0) restoreCooldownTicks = ticks;
    }

    public void clearRestoreCooldown() {
        this.restoreCooldownTicks = -1;
    }

    /* == Snapshot == */

    /**
     * @return an immutable read-only snapshot of the state
     */
    public ZoneSnapshot toSnapshot(Set<String> presentTeamIds) {
        Map<String, Double> teamOxygen = new HashMap<>();
        Set<String> evacuatingTeamIds = new HashSet<>();
        Set<String> refillingTeamIds = new HashSet<>();
        Map<String, Integer> teamCooldownTicks = new HashMap<>();

        for (Map.Entry<String, ZoneTeamOxygenState> entry : getZoneOxygen().entrySet()) {
            String teamId = entry.getKey();
            ZoneTeamOxygenState state = entry.getValue();

            teamOxygen.put(teamId, state.getOxygenPercent());

            switch (state.getPhase()) {
                case EVACUATING -> evacuatingTeamIds.add(teamId);
                case REFILLING  -> {
                    refillingTeamIds.add(teamId);
                    teamCooldownTicks.put(teamId, state.getRecaptureCooldownTicks());
                }
                case NORMAL -> { /* nothing extra needed */ }
            }
        }

        return new ZoneSnapshot(
            getId(),
            getOwnerTeamId(),
            getCapturingTeamId(),
            isContested(),
            getCaptureProgress(),
            teamOxygen,
            Set.copyOf(presentTeamIds),
            Set.copyOf(evacuatingTeamIds),
            Set.copyOf(refillingTeamIds),
            Map.copyOf(teamCooldownTicks)
        );
    }

    /**
     * @return convenience snapshot load with no presence data
     */
    public ZoneSnapshot toSnapshot() {
        return toSnapshot(Set.of());
    }

}
