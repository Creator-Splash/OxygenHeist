package com.creatorsplash.oxygenheist.application.bridge;

import com.creatorsplash.oxygenheist.domain.match.config.MatchConfig;

/**
 * Platform hook for match-world lifecycle
 */
public interface GameWorldService {

    /**
     * Called when a match begins
     */
    void onMatchStarted(MatchConfig config);

    /**
     * Called when the border shrink phase begins
     */
    void onBorderShrinkStart(MatchConfig config);

    /**
     * Called when a match ends
     */
    void onMatchEnded();

    void reset();

}
