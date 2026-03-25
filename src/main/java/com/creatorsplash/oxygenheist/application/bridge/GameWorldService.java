package com.creatorsplash.oxygenheist.application.bridge;

import com.creatorsplash.oxygenheist.domain.match.config.MatchConfig;

/**
 * Platform hook for match-world lifecycle
 */
public interface GameWorldService {

    void onMatchStarted(MatchConfig config);

    void onBorderShrinkStart(MatchConfig config);

    void onMatchEnded();

}
