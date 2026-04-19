package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfigService;
import com.creatorsplash.oxygenheist.platform.paper.display.LobbyDisplayManager;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.TeamUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class TeamListener implements Listener {

    private final Scheduler scheduler;
    private final TeamService teamService;
    private final MatchService matchService;
    private final MessageConfigService messages;

    private final LobbyDisplayManager lobbyDisplayManager;

    /**
     * Runs at LOW priority so friendly fire cancellation happens before
     * combat listeners (NORMAL priority) process the event
     * <p>
     * <!-- Per-team FF: to support per-team friendly fire rules in the future,
     *      replace the global teamService.isFriendlyFireEnabled() check below
     *      with a lookup on the attacker's specific team.
     *      The areTeammates() guard already isolates the pair correctly. -->
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (teamService.isFriendlyFireEnabled()) return;

        if (teamService.areTeammates(victim.getUniqueId(), attacker.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage(MM.msg(messages.get().player().friendlyFireDenied()));
        }
    }

    /* Player join / quit */

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Team team = teamService.getPlayerTeam(player.getUniqueId());

        if (team != null) {
            boolean matchActive = matchService.getSession()
                .map(s -> s.isPlaying() || s.isCountdown())
                .orElse(false);

            if (!matchActive) {
                TeamUtils.applyArmor(player, team);
            }

            lobbyDisplayManager.hideWaitingBar(player);
        } else {
            enforceSpectator(player);
            lobbyDisplayManager.showWaitingBar(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        matchService.getSession().ifPresent(session -> {
            if (session.getPlayer(player.getUniqueId()).isPresent()) {
                TeamUtils.removeArmor(player);
                matchService.removePlayer(player.getUniqueId());
            }
        });
        lobbyDisplayManager.hideWaitingBar(event.getPlayer());
    }

    /* Helpers */

    /**
     * Retries spectator enforcement over several ticks to handle
     * join-timing edge cases where GameMode changes are silently reverted
     */
    private void enforceSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);

        // Retry a few times to handle join-timing races where the client reverts the gamemode
        for (int i = 1; i <= 3; i++) {
            scheduler.runLater(() -> {
                if (player.isOnline() && teamService.getPlayerTeam(player.getUniqueId()) == null) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }, i * 5L);
        }
    }

}
