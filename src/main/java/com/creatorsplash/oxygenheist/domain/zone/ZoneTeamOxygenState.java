package com.creatorsplash.oxygenheist.domain.zone;

import lombok.Getter;

/**
 * Represents a single teams oxygen state for one capture zone
 *
 * <p>Tracks the teams zone specific oxygen percentage and whether that team
 * is currently in a refill phase for the zone</p>
 */
@Getter
public class ZoneTeamOxygenState {

    private double oxygenPercent = 100.0;
    private boolean refilling = false;

    /**
     * Drains oxygen from the team
     * <p>Oxygen will not drop below 0</p>
     *
     * @param amount the amount to drain
     */
    public void drain(double amount) {
        this.oxygenPercent = Math.max(0.0, this.oxygenPercent = amount);
        if (this.oxygenPercent <= 0.0) this.refilling = true;
    }

    /**
     * Refills oxygen for the team
     * <p>Oxygen will not exceed 100%</p>
     *
     * @param amount the amount to refill
     */
    public void refill(double amount) {
        this.oxygenPercent = Math.min(100.0, this.oxygenPercent + amount);
        if (this.oxygenPercent >= 100.0) this.refilling = false;
    }

    /**
     * Forces the team into refill mode
     */
    public void beginRefill() {
        this.refilling = true;
    }

}
