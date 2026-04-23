package com.creatorsplash.oxygenheist.platform.paper.eventcore;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import creatorsplash.creatorsplashcore.event.EventGameEndEvent;
import creatorsplash.creatorsplashcore.event.EventGameStartEvent;
import creatorsplash.creatorsplashcore.event.EventPlayerArriveEvent;
import creatorsplash.creatorsplashcore.event.GameContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EventModeBridge implements Listener, MatchLifecycle {

    private static final long EVACUATION_DELAY_TICKS = 40L;

    private final OxygenHeistPlugin plugin;
    private final MatchService matchService;
    private final TeamService teamService;
    private final boolean enabled;
    private final String proxyLobbyServer;

    public EventModeBridge(OxygenHeistPlugin plugin, MatchService matchService, TeamService teamService) {
        this.plugin = plugin;
        this.matchService = matchService;
        this.teamService = teamService;
        this.enabled = plugin.getConfig().getBoolean("event-mode.enabled", false)
                && Bukkit.getPluginManager().getPlugin("CreatorSplashCore") != null;
        this.proxyLobbyServer = plugin.getConfig()
                .getString("event-mode.proxy-lobby-server", "creatorsplash");
    }

    public boolean isActive() {
        return enabled;
    }

    public void initialize() {
        if (!enabled) {
            plugin.getLogger().info("Event-mode disabled (dev mode). Games start via /oh start only.");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        matchService.registerLifecycle(this);
        plugin.getLogger().info("EventModeBridge registered for OxygenHeist lifecycle events.");
    }

    @EventHandler
    public void onGameStart(EventGameStartEvent event) {
        GameContext ctx = event.getContext();
        if (matchService.isMatchActive()) {
            plugin.getLogger().warning(
                    "[EventModeBridge] EventGameStartEvent received while a match is already active; ignoring.");
            return;
        }

        seedTeamsFromContext(ctx);

        Set<UUID> active = new HashSet<>();
        for (List<Player> members : ctx.onlineTeamMembers().values()) {
            for (Player p : members) {
                active.add(p.getUniqueId());
            }
        }

        if (active.isEmpty()) {
            plugin.getLogger().warning(
                    "[EventModeBridge] EventGameStartEvent fired with no online players yet; starting with empty roster, arrivals will join via EventPlayerArriveEvent.");
        }

        try {
            matchService.createMatch();
            matchService.startMatch(active);
            plugin.getLogger().info("[EventModeBridge] Match started with " + active.size() + " online player(s).");
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("[EventModeBridge] Match start rejected: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onPlayerArrive(EventPlayerArriveEvent event) {
        Player player = event.getPlayer();
        String teamName = event.getTeamName();

        if (teamName != null && !teamName.isBlank()) {
            String teamId = teamName.toLowerCase();
            if (teamService.getTeam(teamId) != null && !teamService.isOnTeam(player.getUniqueId())) {
                teamService.addPlayerToTeam(player.getUniqueId(), teamId);
            }
        }

        if (matchService.isMatchActive()) {
            try {
                matchService.addPlayer(player.getUniqueId());
            } catch (IllegalStateException ignored) {
                // match ended between isMatchActive check and addPlayer — safe to ignore
            }
        }
    }

    @EventHandler
    public void onGameEnd(EventGameEndEvent event) {
        if (matchService.isMatchActive()) {
            plugin.getLogger().info("[EventModeBridge] Core signalled game end; ending active match.");
            matchService.endMatch("");
        } else {
            scheduleEvacuation();
        }
    }

    @Override
    public void onMatchEnd() {
        if (!enabled) return;
        scheduleEvacuation();
    }

    private void scheduleEvacuation() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sendPlayersToLobby(Bukkit.getOnlinePlayers());
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
                        "[EventModeBridge] Core team '" + teamName + "' has no matching OH team config; skipping.");
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

    public void sendPlayersToLobby(Collection<? extends Player> players) {
        if (proxyLobbyServer == null || proxyLobbyServer.isBlank()) {
            plugin.getLogger().warning("[EventModeBridge] proxy-lobby-server not set; skipping lobby transfer.");
            return;
        }
        int moved = 0;
        for (Player player : players) {
            if (player == null || !player.isOnline()) continue;
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF("Connect");
                out.writeUTF(proxyLobbyServer);
                player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
                moved++;
            } catch (IOException | IllegalArgumentException ex) {
                plugin.getLogger().warning("[EventModeBridge] Failed to transfer "
                        + player.getName() + " to '" + proxyLobbyServer + "': " + ex.getMessage());
            }
        }
        plugin.getLogger().info("[EventModeBridge] Sent " + moved + " player(s) to '" + proxyLobbyServer + "'.");
    }
}
