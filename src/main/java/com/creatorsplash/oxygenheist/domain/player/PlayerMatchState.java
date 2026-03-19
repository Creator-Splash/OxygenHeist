package com.creatorsplash.oxygenheist.domain.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents the runtime state of a single player within a match
 *
 * <p>This object tracks gameplay-specific state such as alive/downed status,
 * scoring, and combat interactions. It is mutable and updated throughout the match</p>
 */
@Getter
@RequiredArgsConstructor
public class PlayerMatchState {

    private final UUID playerId;

    private boolean alive = true;
    private boolean downed = false;

    private int score = 0;

    @Setter
    private UUID lastAttacker;

    /* Domain */

    /**
     * Marks the player as downed
     */
    public void down() {
        this.downed = true;
    }

    /**
     * Revives the player from a downed state
     */
    public void revive() {
        this.downed = false;
    }

    /**
     * Eliminates the player from the match
     * <p>Sets the player as no longer alive and clears downed state</p>
     */
    public void eliminate() {
        this.alive = false;
        this.downed = false;
    }

    /**
     * Adds score to the player
     *
     * @param amount amount to add
     */
    public void addScore(int amount) {
        this.score += amount;
    }

}
