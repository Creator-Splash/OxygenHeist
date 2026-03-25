package com.creatorsplash.oxygenheist.application.bridge.display;

import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Default implementation of match display service
 */
@RequiredArgsConstructor
public class DefaultMatchDisplayService implements MatchDisplayService {

    private final MatchDisplayGateway gateway;

    @Override
    public void render(MatchSnapshot snapshot) {
        gateway.renderSnapshot(snapshot);
    }

    @Override
    public void removePlayer(UUID playerId) {
        gateway.removePlayer(playerId);
    }

    @Override
    public void onMatchEnd() {
        gateway.clearAll();
    }

    @Override
    public void reset() {
        gateway.clearAll();
    }

}
