package com.creatorsplash.oxygenheist.platform.paper.integration;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import creatorsplash.creatorsplashcore.api.CreatorSplashCore;
import creatorsplash.creatorsplashcore.api.EndCondition;
import creatorsplash.creatorsplashcore.api.GameAdapter;
import creatorsplash.creatorsplashcore.api.GameRequirements;
import creatorsplash.creatorsplashcore.api.TeamMode;
import creatorsplash.creatorsplashcore.event.GameContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OxygenHeistGameAdapter implements GameAdapter, MatchLifecycle {

    private static final long EVACUATION_DELAY_TICKS = 40L;

    private final OxygenHeistPlugin plugin;
    private final MatchService matchService;
    private final TeamService teamService;
    private final String proxyLobbyServer;

    public OxygenHeistGameAdapter(OxygenHeistPlugin plugin,
                                  MatchService matchService,
                                  TeamService teamService) {
        this.plugin = plugin;
        this.matchService = matchService;
        this.teamService = teamService;
        this.proxyLobbyServer = plugin.getConfig()
                .getString("event-mode.proxy-lobby-server", "creatorsplash");
        matchService.registerLifecycle(this);
    }

    @Override
    public String gameId() {
        return "oxygenheist";
    }

    @Override
    public String displayName() {
        return "Oxygen Heist";
    }

    @Override
    public GameRequirements requirements() {
        return GameRequirements.builder()
                .teamMode(TeamMode.TEAM_VS_TEAM)
                .teamRange(2, 8)
                .playersPerTeamRange(0, 6)
                .endCondition(EndCondition.OBJECTIVE)
                .build();
    }

    @Override
    public void onGameStart(GameContext ctx) {
        // ALWAYS start from scratch. If a match is somehow still active (server
        // reload, GAME_START_FAILED earlier, evac raced the next bootstrap),
        // force-end first so the new round runs on clean state.
        if (matchService.isMatchActive()) {
            plugin.getLogger().info(
                    "[OxygenHeistGameAdapter] Forcing match end before round start (previous still active).");
            try {
                matchService.endMatch("");
            } catch (Throwable t) {
                plugin.getLogger().warning("[OxygenHeistGameAdapter] endMatch threw during reset: "
                        + t.getMessage());
            }
        }
        seedTeamsFromContext(ctx);

        Set<UUID> active = new HashSet<>();
        for (List<Player> members : ctx.onlineTeamMembers().values()) {
            for (Player p : members) active.add(p.getUniqueId());
        }

        try {
            matchService.createMatch();
            matchService.startMatch(active);
            plugin.getLogger().info(
                    "[OxygenHeistGameAdapter] Match started for round "
                            + ctx.round() + "/" + ctx.totalRounds() + " with "
                            + active.size() + " online player(s).");
        } catch (Throwable ex) {
            String reason = ex.getClass().getSimpleName() + ": "
                    + (ex.getMessage() == null ? "" : ex.getMessage());
            plugin.getLogger().warning(
                    "[OxygenHeistGameAdapter] Match start failed: " + reason);
            try {
                creatorsplash.creatorsplashcore.api.ProxyConnector.getInstance()
                        .notifyGameStartFailed(ctx.eventId(), gameId(), reason);
            } catch (Throwable t) {
                plugin.getLogger().warning(
                        "[OxygenHeistGameAdapter] Could not publish GAME_START_FAILED: " + t.getMessage());
            }
        }
    }

    @Override
    public void onPlayerArrive(Player player, String teamName, GameContext ctx) {
        if (teamName != null && !teamName.isBlank()) {
            String teamId = teamName.toLowerCase();
            if (teamService.getTeam(teamId) != null
                    && !teamService.isOnTeam(player.getUniqueId())) {
                teamService.addPlayerToTeam(player.getUniqueId(), teamId);
            }
        }

        // Use hasActiveSession() (SETUP or PLAYING), not isMatchActive() (PLAYING only).
        // In event mode the round transitions are: WAITING -> SETUP (cooldown) ->
        // PLAYING. Players transfer in DURING SETUP, so isMatchActive() returns
        // false at exactly the moment we need to set them up. Without this fix,
        // late arrivals get no team-spawn teleport, no armor, no full health.
        if (!matchService.hasActiveSession()) return;

        // Init oxygen + register in session.
        try {
            matchService.addPlayer(player.getUniqueId());
        } catch (IllegalStateException ignored) {
            // race: match ended between check and add: safe to drop
            return;
        }

        // Run the per-player teleport-to-base + setupPlayer (gamemode +
        // health + food + clear inventory + apply armor) + worldborder
        // setup that startMatch's player loop would have done if this player
        // had been online when the round started.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && matchService.hasActiveSession()) {
                matchService.prepareLateArrival(player.getUniqueId());
            }
        });
    }

    @Override
    public void onGameEnd(GameContext ctx) {
        if (matchService.isMatchActive()) {
            plugin.getLogger().info(
                    "[OxygenHeistGameAdapter] Core signalled game end; ending active match.");
            matchService.endMatch("");
        } else {
            scheduleEvacuation();
        }
    }

    /** MatchLifecycle hook: fires on every match end (natural or forced)
     *  so evacuation happens even when the game ends internally before the
     *  Core round-end arrives. */
    @Override
    public void onMatchEnd() {
        scheduleEvacuation();
    }

    private void scheduleEvacuation() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            CreatorSplashCore.evacuateToLobby(plugin, Bukkit.getOnlinePlayers(), proxyLobbyServer);
            clearRoster();
        }, EVACUATION_DELAY_TICKS);
    }

    private void seedTeamsFromContext(GameContext ctx) {
        clearRoster();
        for (Map.Entry<String, List<UUID>> entry : ctx.teamAssignments().entrySet()) {
            String teamName = entry.getKey();
            if (teamName == null || teamName.isBlank()) continue;
            String teamId = teamName.toLowerCase();
            if (teamService.getTeam(teamId) == null) {
                plugin.getLogger().warning(
                        "[OxygenHeistGameAdapter] Core team '" + teamName
                                + "' has no matching OH team config; skipping.");
                continue;
            }
            for (UUID uuid : entry.getValue()) {
                teamService.addPlayerToTeam(uuid, teamId);
            }
        }
    }

    private void clearRoster() {
        Set<UUID> assigned = new HashSet<>();
        for (Team team : teamService.getAllTeams()) {
            assigned.addAll(team.getMembers());
        }
        for (UUID id : assigned) {
            teamService.removePlayerFromTeam(id);
        }
    }
}
