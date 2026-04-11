package com.creatorsplash.oxygenheist.application.bridge;

import com.creatorsplash.oxygenheist.domain.match.config.MatchConfig;

public class StandaloneGameWorldService implements GameWorldService {
    @Override
    public void onMatchStarted(MatchConfig config) {}

    @Override
    public void onBorderShrinkStart(MatchConfig config) {}

    @Override
    public void onMatchEnded() {}

    @Override
    public void reset() {

    }
}
