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
        if (matchService.isMatchActive()) {
            plugin.getLogger().warning(
                    "[OxygenHeistGameAdapter] onGameStart received while a match is already active; ignoring.");
            return;
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
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning(
                    "[OxygenHeistGameAdapter] Match start rejected: " + ex.getMessage());
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
        if (matchService.isMatchActive()) {
            try {
                matchService.addPlayer(player.getUniqueId());
            } catch (IllegalStateException ignored) {
                // race: match ended between check and add — safe to drop
            }
        }
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

    /** MatchLifecycle hook — fires on every match end (natural or forced)
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
