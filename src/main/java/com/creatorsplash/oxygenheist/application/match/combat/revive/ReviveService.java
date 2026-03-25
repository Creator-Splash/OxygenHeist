package com.creatorsplash.oxygenheist.application.match.combat.revive;

import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.match.config.DownedConfig;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Handles revive interactions between players
 *
 * <p>This service manages active revive sessions, including validation,
 * progression, and cancellation</p>
 */
public class ReviveService {

    private static final double MAX_DISTANCE = 3.0; // todo cfg
    private static final int REQUIRED_PROGRESS = 60; // 3 sec todo config

    /** How many ticks a revive may continue without a fresh player interaction */
    private static final long INTENT_TTL = 5L;

    private final Map<UUID, ReviveSession> activeRevives = new HashMap<>();
    private long currentTick = 0L;

    private boolean isBeingRevived(UUID targetId) {
        return activeRevives.containsKey(targetId);
    }

    public @Nullable ReviveSession getSession(UUID target) {
        return activeRevives.get(target);
    }

    /**
     * Attempts to start or refresh reviving a downed player
     *
     * <p>If a revive session for the target does not exist, it is created.
     * If it already exists and belongs to the same reviver, its intent is refreshed.
     * If another reviver already owns the target, this call does nothing</p>
     *
     * @param session the current match session
     * @param reviverId the player initiating the revive
     * @param targetId the downed player being revived
     */
    public void startOrRefreshRevive(MatchSession session, UUID reviverId, UUID targetId) {
        if (!session.isPlaying()) return;
        if (reviverId.equals(targetId)) return;

        PlayerMatchState target = session.getPlayer(targetId).orElse(null);
        PlayerMatchState reviver = session.getPlayer(reviverId).orElse(null);

        if (target == null || reviver == null) return;
        if (!target.isDowned()) return;
        if (!reviver.isAlive() || reviver.isDowned()) return;

        ReviveSession existing = activeRevives.get(targetId);

        if (existing == null) {
            activeRevives.put(targetId, ReviveSession.of(reviverId, targetId, currentTick));
            return;
        }

        if (!existing.getReviverId().equals(reviverId)) return;

        existing.refreshIntent(currentTick);
    }


    /**
     * Cancels any revive currently targeting the given player
     *
     * @param targetId the player whose revive should be stopped
     */
    public void cancelRevive(UUID targetId) {
        activeRevives.remove(targetId);
    }

    // TODO call for player leaving
    /**
     * Cancels all revive sessions involving the given player, either as reviver or target
     */
    public void cancelRevivesInvolving(UUID playerId) {
        Iterator<Map.Entry<UUID, ReviveSession>> it = activeRevives.entrySet().iterator();

        while (it.hasNext()) {
            ReviveSession session = it.next().getValue();
            if (session.getTargetId().equals(playerId) || session.getReviverId().equals(playerId)) {
                it.remove();
            }
        }
    }

    /**
     * Ticks all active revive sessions
     *
     * <p>Sessions are canceled if they're no longer valid, or if revive intent has not refreshed recently</p>
     */
    public void tick(
        MatchSession session,
        PlayerPositionProvider locationProvider,
        Consumer<UUID> onReviveComplete
    ) {
        DownedConfig config = session.config().downed();

        currentTick++;

        Iterator<Map.Entry<UUID, ReviveSession>> it = activeRevives.entrySet().iterator();

        while (it.hasNext()) {
            ReviveSession revive = it.next().getValue();

            PlayerMatchState target = session.getPlayer(revive.getTargetId()).orElse(null);
            PlayerMatchState reviver = session.getPlayer(revive.getReviverId()).orElse(null);

            if (!isValid(target, reviver, locationProvider, config.reviveMaxDistance())) {
                it.remove();
                continue;
            }

            if (isIntentExpired(config, revive)) {
                it.remove();
                continue;
            }

            revive.increment();

            if (revive.getProgress() >= config.reviveTicks()) {
                target.revive();
                onReviveComplete.accept(target.getPlayerId());
                it.remove();
            }
        }
    }

    /* Internals */

    private boolean isValid(
        PlayerMatchState target,
        PlayerMatchState reviver,
        PlayerPositionProvider locationProvider,
        double maxDistance
    ) {
        if (target == null || reviver == null) return false;

        if (!target.isDowned()) return false;
        if (!reviver.isAlive()) return false;

        var a = locationProvider.getPosition(target.getPlayerId());
        var b = locationProvider.getPosition(reviver.getPlayerId());

        if (a == null || b == null) return false;

        return a.toPos3().distanceSquared(b.toPos3()) <= (maxDistance * maxDistance);
    }

    private boolean isIntentExpired(DownedConfig config, ReviveSession revive) {
        return (currentTick - revive.getLastIntentTick()) > config.reviveIntentTtlTicks();
    }

}
