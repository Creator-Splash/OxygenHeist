package com.creatorsplash.oxygenheist.application.match.combat.revive;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Represents an active revive interaction between two players
 */
@Getter
@RequiredArgsConstructor
public class ReviveSession {

    private final UUID reviverId;
    private final UUID targetId;

    private int progress;
    private long lastIntentTick;

    public void increment() {
        this.progress++;
    }

    public void refreshIntent(long currentTick) {
        this.lastIntentTick = currentTick;
    }

    public static ReviveSession of(UUID reviverId, UUID targetId) {
        return new ReviveSession(reviverId, targetId);
    }

    public static ReviveSession of(UUID reviverId, UUID targetId, long currentTick) {
        var session = new ReviveSession(reviverId, targetId);
        session.refreshIntent(currentTick);
        return session;
    }

}
