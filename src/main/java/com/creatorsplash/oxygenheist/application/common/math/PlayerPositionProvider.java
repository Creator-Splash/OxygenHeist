package com.creatorsplash.oxygenheist.application.common.math;

import java.util.UUID;

@FunctionalInterface
public interface PlayerPositionProvider {
    FullPosition getPosition(UUID playerId);
}
