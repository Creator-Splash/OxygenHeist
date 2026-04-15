package com.creatorsplash.oxygenheist.domain.zone;

import lombok.Getter;

/**
 * Represents a single teams oxygen state for one capture zone
 *
 * <p>Tracks the teams zone specific oxygen percentage and whether that team
 * is currently in a refill phase for the zone</p>
 *
 * <p>Phase lifecycle:
 * <pre>
 *   NORMAL ──(hits 0%)──▶ EVACUATING ──(team fully leaves)──▶ REFILLING
 * </pre>
 * </p>
 */
@Getter
public class ZoneTeamOxygenState {

    public enum OxygenPhase { NORMAL, EVACUATING, REFILLING }

    private double oxygenPercent = 100.0;
    private OxygenPhase phase = OxygenPhase.NORMAL;

    /** Ticks remaining before the team may recapture. <p>0 = ready */
    private int recaptureCooldownTicks = 0;

    /**
     * Drains oxygen from the team
     * <p>Oxygen will not drop below 0</p>
     * <p>Only acts in NORMAL phase</p>
     *
     * @param amount the amount to drain
     */
    public boolean drain(double amount) {
        if (amount <= 0.0 || phase != OxygenPhase.NORMAL) return false;

        boolean wasAboveZero = oxygenPercent > 0;
        this.oxygenPercent = Math.max(0.0, this.oxygenPercent - amount);

        if (this.oxygenPercent <= 0.0 && wasAboveZero) {
            this.phase = OxygenPhase.EVACUATING;
            return true;
        }

        return false;
    }

    /**
     * Transitions from EVACUATING to REFILLING
     * <p>Called when the last team member has left the zone after depletion.
     * No-op if not currently EVACUATING</p>
     */
    public void beginRefill(int cooldownTicks) {
        if (this.phase != OxygenPhase.EVACUATING) return;
        this.phase = OxygenPhase.REFILLING;
        this.recaptureCooldownTicks = cooldownTicks;
    }

    public void tickCooldown() {
        if (phase == OxygenPhase.REFILLING && recaptureCooldownTicks > 0) {
            recaptureCooldownTicks--;
        }
    }

    /**
     * Refills oxygen
     * <p>Only acts in REFILLING phase. Oxygen will not exceed 100%.
     * Automatically returns to NORMAL when full</p>
     *
     * @param amount the amount to refill
     */
    public void refill(double amount) {
        if (phase != OxygenPhase.REFILLING) return;
        this.oxygenPercent = Math.min(100.0, this.oxygenPercent + amount);
        if (this.oxygenPercent >= 100.0) {
            resetToNormal();
        }
    }

    /**
     * Forces a transition back to NORMAL regardless of current phase
     */
    public void resetToNormal() {
        this.phase = OxygenPhase.NORMAL;
        this.recaptureCooldownTicks = 0;
    }

    /**
     * @return true if this team can attempt to recapture the zone
     * (fully refilled and in NORMAL phase)
     */
    public boolean canRecapture() {
        return (phase == OxygenPhase.REFILLING || phase == OxygenPhase.NORMAL)
            && recaptureCooldownTicks <= 0;
    }

    /**
     * @return true if the team is in EVACUATING or REFILLING phase
     */
    public boolean isRefilling() {
        return phase == OxygenPhase.EVACUATING || phase == OxygenPhase.REFILLING;
    }

}
