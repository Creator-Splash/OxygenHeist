package com.creatorsplash.oxygenheist.application.match.revive;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Handles revive-related gameplay logic
 *
 * <p>This service is responsible for initiating, progressing, and completing
 * revive interactions between players. It operates purely on match state and
 * does not perform orchestration or side effects</p>
 */
public class ReviveService {

    private static final int REQUIRED_PROGRESS = 60; // 3 sec todo config

    /**
     * Attempts to start reviving a downed player
     *
     * @param session the current match session
     * @param reviverId the player initiating the revive
     * @param targetId the downed player being revived
     */
    public void startRevive(MatchSession session, UUID reviverId, UUID targetId) {
        if (!session.isPlaying()) return;

        PlayerMatchState target = session.getOrCreatePlayer(targetId);

        if (!target.isDowned()) return;
        if (target.isBeingRevived()) return;

        target.startRevive(reviverId);
    }

    /**
     * Stops an active revive on a player, if one exists
     *
     * @param session the current match session
     * @param targetId the player whose revive should be stopped
     */
    public void stopRevive(MatchSession session, UUID targetId) {
        PlayerMatchState target = session.getOrCreatePlayer(targetId);

        if (!target.isBeingRevived()) return;

        target.stopRevive();
    }

    /**
     * Ticks all active revive interactions
     *
     * <p>This method progresses revive timers and triggers completion callbacks
     * when a revive finishes</p>
     *
     * @param session the current match session
     * @param onReviveComplete callback invoked when a player is revived
     */
    public void tick(MatchSession session, Consumer<UUID> onReviveComplete) {
        for (PlayerMatchState player : session.getPlayers()) {
            if (!player.isBeingRevived()) continue;

            player.addReviveProgress(1);

            if (player.getReviveProgress() >= REQUIRED_PROGRESS) {
                UUID revivedId = player.getPlayerId();

                player.revive();

                onReviveComplete.accept(revivedId);
            }
        }
    }

}
