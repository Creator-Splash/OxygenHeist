package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.MatchSnapshotProvider;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.domain.zone.ZoneSnapshot;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.util.TeamUtils;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages per-zone Text Display holograms and particle rings during a match.
 *
 * <p>One TextDisplay entity per zone shows the zone name, owning team,
 * and capture progress. A repeating task drives both display updates
 * and particle effects from the current {@link MatchSnapshot}</p>
 */
@RequiredArgsConstructor
public final class ZoneDisplayManager implements MatchLifecycle {

    private final Server server;
    private final TeamService teamService;
    private final ArenaConfigService arenaConfigService;
    private final MatchSnapshotProvider snapshotProvider;
    private final Scheduler scheduler;
    private final LogCenter log;

    /** Keyed by zone id */
    private final Map<String, TextDisplay> displays = new HashMap<>();
    /** Particle angle offset per zone id for rotation animation */
    private final Map<String, Double> particleOffsets = new HashMap<>();

    private Scheduler.Task task;

    /* Lifecycle */

    @Override
    public void onMatchStart() {
        for (ZoneDefinition def : arenaConfigService.getZones()) {
            Location loc = centerOf(def);
            if (loc == null) {
                log.warn("Zone '" + def.id() + "' world '" + def.worldName()
                    + "' not loaded - display skipped");
                continue;
            }

            loc.add(0, 2.6, 0); // float above ground

            TextDisplay td = loc.getWorld().spawn(loc, TextDisplay.class, d -> {
                d.setBillboard(Display.Billboard.CENTER);
                d.setShadowed(true);
                d.setDefaultBackground(false);
                d.setViewRange(24f);
                d.text(buildText(def.id(), def.displayName(), 0.0));
            });

            displays.put(def.id(), td);
            particleOffsets.put(def.id(), 0.0);
        }

        // Single task drives both display updates and particles
        task = scheduler.runRepeating(this::tick, 1L, 4L);

        log.info("Zone displays spawned for " + displays.size() + " zone(s)");
    }

    @Override
    public void onMatchEnd() {
        if (task != null) { task.cancel(); task = null; }
        displays.values().forEach(td -> { if (td.isValid()) td.remove(); });
        displays.clear();
        particleOffsets.clear();
    }

    /* Custom Tick */

    private void tick() {
        MatchSnapshot snapshot = snapshotProvider.get();
        if (snapshot == null) return;

        for (ZoneDefinition def : arenaConfigService.getZones()) {
            ZoneSnapshot zs = snapshot.getZone(def.id());
            if (zs == null) continue;

            TextDisplay td = displays.get(def.id());
            if (td != null && td.isValid()) {
                td.text(buildText(def.displayName(), zs.ownerTeamId(), zs.captureProgress()));
            }

            Location center = centerOf(def);
            if (center == null) continue;

            double offset = particleOffsets.merge(def.id(), 0.08, Double::sum) % (Math.PI * 2);
            particleOffsets.put(def.id(), offset);

            spawnParticleRing(center, radiusOf(def), zs.ownerTeamId(), zs.captureProgress(), offset, snapshot);
        }
    }

    /* Internals */

    private void spawnParticleRing(
            Location center, double radius,
            String ownerTeamId, double captureProgress,
            double angleOffset, MatchSnapshot snapshot
    ) {
        Color color = ownerTeamId != null
            ? TeamUtils.colorTagToRgb(teamColorTag(ownerTeamId))
            : Color.SILVER;

        Particle.DustOptions dust = new Particle.DustOptions(color, 1.3f);

        int points = Math.max(16, (int) (radius * 4));
        double step = (Math.PI * 2) / points;
        for (int i = 0; i < points; i++) {
            double angle = i * step + angleOffset;
            ParticleUtils.spawn(Particle.DUST,
                center.clone().add(radius * Math.cos(angle), 0.2, radius * Math.sin(angle)),
                1, 0, 0, 0, 0, dust, snapshot);
        }

        if (captureProgress > 0 && captureProgress < 100) {
            double height = (captureProgress / 100.0) * 3.0;
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i + angleOffset;
                double ox = radius * Math.cos(angle);
                double oz = radius * Math.sin(angle);
                for (double y = 0; y < height; y += 0.4) {
                    ParticleUtils.spawn(Particle.DUST,
                        center.clone().add(ox, y, oz),
                        1, 0, 0, 0, 0, dust, snapshot
                    );
                }
            }
        }
    }

    private Component buildText(String displayName, String ownerTeamId, double captureProgress) {
        String nameLine = "<white><bold>" + displayName + "</bold></white>";

        String statusLine;
        if (ownerTeamId != null) {
            Team team = teamService.getTeam(ownerTeamId);
            String color = team != null ? team.getColor() : "white";
            String name  = team != null ? team.getName()  : ownerTeamId;
            statusLine = "<" + color + ">▶ " + name + "</" + color + ">";
        } else {
            statusLine = "<gray>◆ Neutral</gray>";
        }

        String progressLine = buildProgressBar(captureProgress, ownerTeamId);

        return MM.msg(nameLine + "\n" + statusLine + "\n" + progressLine);
    }

    private String buildProgressBar(double progress, String ownerTeamId) {
        int filled = (int) Math.round((progress / 100.0) * 20);
        String color = ownerTeamId != null ? teamColorTag(ownerTeamId) : "gray";
        return "<" + color + ">" + "█".repeat(filled) + "</" + color + ">"
                + "<dark_gray>" + "░".repeat(20 - filled) + "</dark_gray>"
                + " <white>" + (int) progress + "%</white>";
    }

    /* Helpers */

    private Location centerOf(ZoneDefinition def) {
        World world = server.getWorld(def.worldName());
        if (world == null) return null;
        return switch (def) {
            case ZoneDefinition.Circle c ->
                new Location(world, c.centerX(), c.centerY(), c.centerZ());
            case ZoneDefinition.Cuboid c ->
                new Location(world,
                    (c.minX() + c.maxX()) / 2.0,
                    (c.minY() + c.maxY()) / 2.0,
                    (c.minZ() + c.maxZ()) / 2.0);
        };
    }

    private double radiusOf(ZoneDefinition def) {
        return switch (def) {
            case ZoneDefinition.Circle c -> c.radius();
            case ZoneDefinition.Cuboid c -> Math.max(
                (c.maxX() - c.minX()) / 2.0,
                (c.maxZ() - c.minZ()) / 2.0);
        };
    }

    private String teamColorTag(String teamId) {
        Team team = teamService.getTeam(teamId);
        return team != null ? team.getColor() : "white";
    }

}
