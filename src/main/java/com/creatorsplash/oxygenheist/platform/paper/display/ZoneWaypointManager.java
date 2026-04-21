package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.domain.zone.ZoneSnapshot;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.match.MatchConfigService;
import com.creatorsplash.oxygenheist.platform.paper.util.LocationUtils;
import com.creatorsplash.oxygenheist.platform.paper.util.TeamUtils;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives per-zone waypoints on the player locator bar
 */
@RequiredArgsConstructor
public final class ZoneWaypointManager implements MatchLifecycle, Listener {

    private static final double TRANSMIT_MAX = 6e7;
    private static final double TRANSMIT_OFF = 0.0;
    private static final int TICK_INTERVAL = 5;

    private static final String STYLE_CAPTURED = "square";
    private static final String STYLE_CAPTURING = "circle";
    private static final String STYLE_NEUTRAL = "small_circle";

    private static final String SB_PREFIX = "oh_zone_";
    private static final String NEUTRAL_KEY = SB_PREFIX + "neutral";

    private final OxygenHeistPlugin plugin;
    private final MatchService matchService;
    private final TeamService teamService;
    private final ArenaConfigService arenaConfig;
    private final MatchConfigService matchConfig;
    private final LogCenter log;

    /** zoneId -> waypoint armor stand */
    private final Map<String, ArmorStand> zoneStands = new HashMap<>();

    /** Diff cache: zoneId -> last applied state key ("teamId:style:visible") */
    private final Map<String, String> lastState = new HashMap<>();

    /** teamId -> scoreboard team used to color this team's zone dots */
    private final Map<String, Team> sbTeams = new HashMap<>();
    private @Nullable Team neutralSbTeam;

    private int tickCount;

    /* == Match Lifecycle == */

    @Override
    public void onCountdownStart() {
        suppressAllPlayers();
        setupScoreboardTeams();
        spawnZoneMarkers();
    }

    @Override
    public void onMatchStart() {
        tickCount = 0;
    }

    @Override
    public void readGameTick(MatchSnapshot snapshot) {
        tickInternal(snapshot);
    }

    @Override
    public void cleanUp() {
        removeStands();
        restoreAllPlayers();
        teardownScoreboardTeams();
        lastState.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        /* need? */
    }

    /* == Listener == */

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (matchService.isMatchActive()) {
            suppressPlayer(event.getPlayer());
        }
    }

    /* Tick */

    private void tickInternal(MatchSnapshot snapshot) {
        if (++tickCount % TICK_INTERVAL != 0) return;

        for (Map.Entry<String, ArmorStand> entry : zoneStands.entrySet()) {
            String zoneId = entry.getKey();
            ArmorStand stand = entry.getValue();
            if (!stand.isValid()) continue;

            ZoneSnapshot zs = snapshot.getZone(zoneId);
            if (zs == null) continue;

            updateWaypoint(zoneId, stand, zs);
        }
    }

    private void updateWaypoint(
        String zoneId,
        ArmorStand stand,
        ZoneSnapshot zs
    ) {
        boolean showUncaptured = matchConfig.get().zones().waypointShowUncaptured();

        String activeTeamId;
        String style;
        boolean visible;

        if (zs.ownerTeamId() != null) {
            activeTeamId = zs.ownerTeamId();
            style = STYLE_CAPTURED;
            visible = true;
        } else if (zs.capturingTeamId() != null) {
            activeTeamId = zs.capturingTeamId();
            style = STYLE_CAPTURING;
            visible = true;
        } else {
            activeTeamId = null;
            style = STYLE_NEUTRAL;
            visible = showUncaptured;
        }

        // Diff - skip if nothing has changed since last tick
        String stateKey = activeTeamId + ":" + style + ":" + visible;
        if (stateKey.equals(lastState.get(zoneId))) return;
        lastState.put(zoneId, stateKey);

        setTransmit(stand, visible
            ? (zs.ownerTeamId() != null
                ? TRANSMIT_MAX
                : matchConfig.get().zones().waypointBaseTransmitRange())
            : TRANSMIT_OFF);

        NamedTextColor teamColor = resolveWaypointColor(activeTeamId);

        if (!visible) return;

        reassignTeam(stand, activeTeamId);
        applyColor(stand, teamColor);

        //applyStyle(stand, style);
    }

    /* -- Scoreboard teams -- */

    private void setupScoreboardTeams() {
        Scoreboard sb = plugin.getServer().getScoreboardManager().getMainScoreboard();
        this.neutralSbTeam = recreateTeam(sb, NEUTRAL_KEY, NamedTextColor.GRAY);

        for (com.creatorsplash.oxygenheist.domain.team.Team team : teamService.getAllTeams()) {
            sbTeams.put(team.getId(), recreateTeam(sb, SB_PREFIX + team.getId(),
                TeamUtils.colorTagToNearestNamed(team.getColor())));
        }
    }

    private void teardownScoreboardTeams() {
        if (this.neutralSbTeam != null) {
            this.neutralSbTeam.unregister();
            this.neutralSbTeam = null;
        }
        this.sbTeams.values().forEach(Team::unregister);
        this.sbTeams.clear();
    }

    private @Nullable NamedTextColor resolveWaypointColor(@Nullable String teamId) {
        if (teamId == null) return NamedTextColor.GRAY;
        com.creatorsplash.oxygenheist.domain.team.Team team = teamService.getTeam(teamId);
        if (team == null) return NamedTextColor.GRAY;
        return TeamUtils.colorTagToNearestNamed(team.getColor());
    }

    /**
     * Removes the stand from all managed teams then adds it to the appropriate one
     */
    private void reassignTeam(ArmorStand stand, @Nullable String teamId) {
        String entry = stand.getUniqueId().toString();

        if (this.neutralSbTeam != null) this.neutralSbTeam.removeEntry(entry);
        this.sbTeams.values().forEach(t -> t.removeEntry(entry));

        Team target = (teamId != null) ? this.sbTeams.get(teamId) : this.neutralSbTeam;
        if (target != null) target.addEntry(entry);
    }

    /**
     * Unregisters any existing team with this name, then registers fresh with the given color
     * <p>Prevents stale team state from a previous match</p>
     */
    private Team recreateTeam(Scoreboard sb, String name, NamedTextColor color) {
        Team existing = sb.getTeam(name);
        if (existing != null) existing.unregister();
        Team team = sb.registerNewTeam(name);
        team.color(color);
        return team;
    }

    /* -- Markers -- */

    private void spawnZoneMarkers() {
        for (ZoneDefinition def : arenaConfig.getZones()) {
            Location center = LocationUtils.centerOf(def);
            if (center == null) {
                log.warn("Zone '" + def.id() + "' world '" + def.worldName() + "' not loaded");
                continue;
            }

            ArmorStand stand = center.getWorld().spawn(center, ArmorStand.class, s -> {
                s.setVisible(false);
                s.setGravity(false);
                s.setInvulnerable(true);
                s.setSilent(true);
                //s.setMarker(true);
                s.setCanTick(true);
                s.setPersistent(false);
                s.setCustomNameVisible(false);
                s.setAI(false);

                setTransmit(s, TRANSMIT_OFF);

                if (this.neutralSbTeam != null) {
                    this.neutralSbTeam.addEntry(s.getUniqueId().toString());
                }
            });

            zoneStands.put(def.id(), stand);
        }
    }

    private void removeStands() {
        zoneStands.values().forEach(s -> { if (s.isValid()) s.remove(); });
        zoneStands.clear();
    }

    /* -- Player -- */

    private void suppressAllPlayers() {
        plugin.getServer().getOnlinePlayers()
            .forEach(this::suppressPlayer);
    }

    private void restoreAllPlayers() {
        plugin.getServer().getOnlinePlayers()
            .forEach(p -> setPlayerTransmit(p, TRANSMIT_MAX));
    }

    private void suppressPlayer(Player player) {
        setPlayerTransmit(player, TRANSMIT_OFF);
    }

    private void setPlayerTransmit(Player player, double value) {
        AttributeInstance attr = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (attr != null) attr.setBaseValue(value);
    }

    /* -- Attribute Helpers -- */

    private void setTransmit(ArmorStand stand, double value) {
        AttributeInstance attr = stand.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (attr == null) {
            log.warn("WaypointTransmitRange is null for ArmorStand");
            return;
        }
        attr.setBaseValue(value);
    }

    /* 1.21.8 command requirement */

    private void applyColor(ArmorStand stand, @Nullable NamedTextColor color) {
        String cmd = color != null
            ? "waypoint modify " + stand.getUniqueId() + " color " + color
            : "waypoint modify " + stand.getUniqueId() + " color reset";

        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
    }

}
