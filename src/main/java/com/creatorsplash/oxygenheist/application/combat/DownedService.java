package com.creatorsplash.oxygenheist.application.combat;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Handles downed-state gameplay logic for players within an active match
 *
 * <p>This service is responsible for transitioning players into a downed state,
 * progressing bleedout timers, reviving players, and converting bleedout completion
 * into full elimination</p>
 *
 * <p>Scheduling is owned by the match lifecycle rather than this service itself.
 * This class only contains the domain/application logic for downed-state behavior</p>
 */
public class DownedService {

    private static final int DEFAULT_BLEEDOUT_TICKS = 20 * 15; // 15 sec todo cfg

    /**
     * Places a player into the downed state if they are eligible
     *
     * <p>A player cannot be downed if there is no active match, the match is not
     * in the playing state, the player is already eliminated, or the player is
     * already downed</p>
     *
     * @param playerId the UUID of the player to down
     */
    public void downPlayer(MatchSession session, UUID playerId) {
        if (!session.isPlaying()) return;

        PlayerMatchState player = session.getOrCreatePlayer(playerId);

        if (!player.isAlive() || player.isDowned()) return;

        player.down(DEFAULT_BLEEDOUT_TICKS);
    }

    /**
     * Ticks bleedout progress for all downed players in the current match
     *
     * <p>If a players bleedout timer reaches zero, they are eliminated</p>
     */
    public void tick(MatchSession session, Consumer<UUID> onBleedout) {
        for (PlayerMatchState player : session.getPlayers()) {
            if (!player.isDowned()) continue;

            player.tickBleedout();

            if (player.isBleedoutComplete()) {
                onBleedout.accept(player.getPlayerId());
            }
        }
    }

    /**
     * Revives a downed player if they are currently downed
     *
     * @param playerId the UUID of the player to revive
     */
    public void revivePlayer(MatchSession session, UUID playerId) {
        PlayerMatchState player = session.getOrCreatePlayer(playerId);

        if (!player.isDowned()) return;

        player.revive();
    }

}
