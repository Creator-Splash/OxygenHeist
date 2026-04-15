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
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.match.MatchConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfig;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfigService;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.util.TeamUtils;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.*;

/**
 * Manages per-zone Text Display holograms and particle rings during a match
 *
 * <p>One TextDisplay entity per zone shows the zone name, owning team,
 * and capture progress. A repeating task drives both display updates
 * and particle effects from the current {@link MatchSnapshot}</p>
 */
@RequiredArgsConstructor
public final class ZoneDisplayManager implements MatchLifecycle {

    private final OxygenHeistPlugin plugin;
    private final TeamService teamService;
    private final ArenaConfigService arenaConfigService;
    private final MatchConfigService matchConfigService;
    private final MessageConfigService messages;
    private final MatchSnapshotProvider snapshotProvider;
    private final Scheduler scheduler;
    private final LogCenter log;

    /** Particle angle offset per zone id for rotation animation */
    private final Map<String, Double> particleOffsets = new HashMap<>();

    /** Keyed by zone id */
    private final Map<String, TextDisplay> displays = new HashMap<>();

    /**
     * Private team oxygen displays - zoneId -> teamId -> TextDisplay
     * <p>One entity per team per zone, visible only to that teams online members</p>
     */
    private final Map<String, Map<String, TextDisplay>> teamOxygenDisplays = new HashMap<>();

    /**
     * Current viewers of each team oxygen display - zoneId -> teamId -> Set<UUID>
     * Diffed every tick to manage showEntity/hideEntity calls
     */
    private final Map<String, Map<String, Set<UUID>>> teamOxygenViewers = new HashMap<>();

    /** Zone tick sound throttles */
    private final Map<String, Long> lastContestedSound = new HashMap<>();
    private final Map<String, Long> lastCapturingSound = new HashMap<>();

    private Scheduler.Task task;

    /* Lifecycle */

    @Override
    public void onMatchStart() {
        var zoneCfg = matchConfigService.get().zones();

        for (ZoneDefinition def : arenaConfigService.getZones()) {
            Location center = centerOf(def);
            if (center == null) {
                log.warn("Zone '" + def.id() + "' world '" + def.worldName()
                    + "' not loaded - display skipped");
                continue;
            }

            Location mainLoc = center.clone().add(0, zoneCfg.displayMainHeight(), 0);

            TextDisplay td = mainLoc.getWorld().spawn(mainLoc, TextDisplay.class, d -> {
                d.setBillboard(Display.Billboard.CENTER);
                d.setShadowed(true);
                d.setDefaultBackground(false);
                d.setViewRange(24f);
                d.text(buildMainText(def.displayName(), def.toRuntimeState().toSnapshot()));
                d.setPersistent(false);
            });

            displays.put(def.id(), td);

            Map<String, TextDisplay> zoneTeamDisplays = new HashMap<>();
            Map<String, Set<UUID>> zoneTeamViewers = new HashMap<>();

            for (Team team : teamService.getAllTeams()) {
                Location teamLoc = center.clone().add(0, zoneCfg.displayTeamHeight(), 0);

                TextDisplay teamTd = teamLoc.getWorld().spawn(teamLoc, TextDisplay.class, d -> {
                    d.setBillboard(Display.Billboard.CENTER);
                    d.setShadowed(true);
                    d.setDefaultBackground(false);
                    d.setViewRange(24f);
                    d.setVisibleByDefault(false);
                    d.text(Component.empty());
                    d.setPersistent(false);
                });
                zoneTeamDisplays.put(team.getId(), teamTd);
                zoneTeamViewers.put(team.getId(), new HashSet<>());
            }

            teamOxygenDisplays.put(def.id(), zoneTeamDisplays);
            teamOxygenViewers.put(def.id(), zoneTeamViewers);
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

        teamOxygenDisplays.values().forEach(byTeam ->
            byTeam.values().forEach(td -> { if (td.isValid()) td.remove(); }));
        teamOxygenDisplays.clear();
        teamOxygenViewers.clear();

        particleOffsets.clear();

        lastContestedSound.clear();
        lastCapturingSound.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        teamOxygenViewers.values().forEach(byTeam ->
            byTeam.forEach((teamId, viewers) -> viewers.remove(playerId)));
    }

    /* Custom Tick */

    private void tick() {
        MatchSnapshot snapshot = snapshotProvider.get();
        if (snapshot == null) return;

        int maxCooldownTicks = snapshot.config().zones().recaptureCooldownSeconds() * 20;

        for (ZoneDefinition def : arenaConfigService.getZones()) {
            ZoneSnapshot zs = snapshot.getZone(def.id());
            if (zs == null) continue;

            TextDisplay main = displays.get(def.id());
            if (main != null && main.isValid()) {
                main.text(buildMainText(def.displayName(), zs));
            }

            tickTeamDisplays(def.id(), zs, maxCooldownTicks);

            Location center = centerOf(def);
            if (center == null) continue;

            double offset = particleOffsets.merge(def.id(), 0.08, Double::sum) % (Math.PI * 2);
            particleOffsets.put(def.id(), offset);

            spawnParticles(def, center, zs.ownerTeamId(), zs.captureProgress(), offset, snapshot);
            tickZoneSounds(def.id(), zs, center);
        }
    }

    private void tickTeamDisplays(String zoneId, ZoneSnapshot zs, int maxCooldownTicks) {
        Map<String, TextDisplay> zoneDisplays = teamOxygenDisplays.get(zoneId);
        Map<String, Set<UUID>> zoneViewers = teamOxygenViewers.get(zoneId);
        if (zoneDisplays == null || zoneViewers == null) return;

        for (Team team : teamService.getAllTeams()) {
            String teamId = team.getId();

            TextDisplay td = zoneDisplays.get(teamId);
            if (td == null || !td.isValid()) continue;

            // Update text
            td.text(buildTeamOxygenText(teamId, zs, maxCooldownTicks));

            // Diff viewers - who should see this display vs who currently does
            Set<UUID> currentViewers = zoneViewers.computeIfAbsent(teamId, k -> new HashSet<>());
            Set<UUID> shouldSee = new HashSet<>();

            for (UUID memberId : team.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    shouldSee.add(memberId);
                }
            }

            // Show to new viewers
            for (UUID playerId : shouldSee) {
                if (currentViewers.add(playerId)) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) player.showEntity(plugin, td);
                }
            }

            // Hide from players who should no longer see it
            Iterator<UUID> it = currentViewers.iterator();
            while (it.hasNext()) {
                UUID playerId = it.next();
                if (!shouldSee.contains(playerId)) {
                    it.remove();
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) player.hideEntity(plugin, td);
                }
            }
        }
    }

    /* Internals */

    /* == Sounds == */

    private void tickZoneSounds(String zoneId, ZoneSnapshot zs, Location center) {
        long now = System.currentTimeMillis();
        MessageConfig.ZoneMessages zone = messages.get().zone();

        if (zs.contested()) {
            if (now - lastContestedSound.getOrDefault(zoneId, 0L) >= 1000) {
                lastContestedSound.put(zoneId, now);
                if (zone.contestedSound() != null) zone.contestedSound().playAt(center);
            }
            return;
        }

        if (zs.capturingTeamId() != null && zs.ownerTeamId() == null) {
            if (now - lastCapturingSound.getOrDefault(zoneId, 0L) >= 1000) {
                lastCapturingSound.put(zoneId, now);
                if (zone.capturingSound() != null) zone.capturingSound().playAt(center);
            }
        }
    }

    /* == Particles == */

    private void spawnParticles(
        ZoneDefinition def, Location center,
        String ownerTeamId, double captureProgress,
        double angleOffset, MatchSnapshot snapshot
    ) {
        Color color = ownerTeamId != null
            ? TeamUtils.colorTagToRgb(teamColorTag(ownerTeamId))
            : Color.SILVER;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.3f);

        switch (def) {
            case ZoneDefinition.Circle c ->
                spawnCircleRing(center, c.radius(), captureProgress, angleOffset, dust, snapshot);
            case ZoneDefinition.Cuboid c ->
                spawnCuboidOutline(c, captureProgress, dust, snapshot);
        }
    }

    private void spawnCircleRing(
        Location center, double radius, double captureProgress,
        double angleOffset, Particle.DustOptions dust, MatchSnapshot snapshot
    ) {
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
                for (double y = 0; y < height; y += 0.4) {
                    ParticleUtils.spawn(Particle.DUST,
                        center.clone().add(radius * Math.cos(angle), y, radius * Math.sin(angle)),
                        1, 0, 0, 0, 0, dust, snapshot);
                }
            }
        }
    }

    private void spawnCuboidOutline(
        ZoneDefinition.Cuboid c,
        double captureProgress,
        Particle.DustOptions dust,
        MatchSnapshot snapshot
    ) {
        World world = plugin.getServer().getWorld(c.worldName());
        if (world == null) return;

        double y = c.maxY() + 0.2;
        double step = 0.5;

        // North/South edges (along X axis, fixed Z)
        for (double x = c.minX(); x <= c.maxX() + 1.0; x += step) {
            ParticleUtils.spawn(Particle.DUST,
                new Location(world, x, y, c.minZ()),
                1, 0, 0, 0, 0, dust, snapshot);
            ParticleUtils.spawn(Particle.DUST,
                new Location(world, x, y, c.maxZ() + 1.0),
                1, 0, 0, 0, 0, dust, snapshot);
        }

        // East/West edges (along Z axis, fixed X)
        for (double z = c.minZ(); z <= c.maxZ() + 1.0; z += step) {
            ParticleUtils.spawn(Particle.DUST,
                new Location(world, c.minX(), y, z),
                1, 0, 0, 0, 0, dust, snapshot);
            ParticleUtils.spawn(Particle.DUST,
                new Location(world, c.maxX() + 1.0, y, z),
                1, 0, 0, 0, 0, dust, snapshot);
        }

        // Capture progress pillars at corners
        if (captureProgress > 0 && captureProgress < 100) {
            double height = (captureProgress / 100.0) * 3.0;
            double[][] corners = {
                {c.minX(), c.minZ()},
                {c.maxX() + 1.0, c.minZ()},
                {c.maxX() + 1.0, c.maxZ() + 1.0},
                {c.minX(), c.maxZ() + 1.0}
            };
            for (double[] corner : corners) {
                for (double py = 0; py < height; py += 0.4) {
                    ParticleUtils.spawn(Particle.DUST,
                        new Location(world, corner[0], c.maxY() + py, corner[1]),
                        1, 0, 0, 0, 0, dust, snapshot);
                }
            }
        }
    }

    /* == Hologram Builders == */

    private Component buildMainText(String displayName, ZoneSnapshot zs) {
        String nameLine = "<white><bold>" + displayName + "</bold></white>";

        if (zs.contested()) return buildContestedText(nameLine, zs);

        boolean enemyRegressing = zs.ownerTeamId() != null
            && zs.presentTeamIds().size() == 1
            && !zs.presentTeamIds().contains(zs.ownerTeamId());
        if (enemyRegressing) return buildUnderAttackText(nameLine, zs);

        if (zs.ownerTeamId() != null) return buildOwnedText(nameLine, zs);
        if (zs.capturingTeamId() != null) return buildCapturingText(nameLine, zs);
        return buildNeutralText(nameLine);
    }

    private Component buildOwnedText(String nameLine, ZoneSnapshot zs) {
        Team team = teamService.getTeam(zs.ownerTeamId());
        String color = team != null ? team.getColor() : "white";
        String name = team != null ? team.getName() : zs.ownerTeamId();

        double zoneOxygen = zs.teamOxygen().getOrDefault(zs.ownerTeamId(), 100.0);
        String oxygenColor = zoneOxygen > 50 ? "aqua" : zoneOxygen > 20 ? "yellow" : "red";

        String statusLine = "<" + color + ">" + sym().zoneOwned() + " " + name + "</" + color + ">";
        String labelLine = "<dark_gray>" + messages.get().zone().labelZoneOxygen() + "</dark_gray>";
        String barLine = buildProgressBar(zoneOxygen, oxygenColor);

        return MM.msg(nameLine + "\n" + statusLine + "\n\n" + labelLine + "\n" + barLine);
    }

    private Component buildUnderAttackText(String nameLine, ZoneSnapshot zs) {
        Team owner = teamService.getTeam(zs.ownerTeamId());
        String ownerColor = owner != null ? owner.getColor() : "white";
        String ownerName = owner != null ? owner.getName() : zs.ownerTeamId();

        String progressColor = zs.captureProgress() > 50 ? ownerColor
            : zs.captureProgress() > 20 ? "yellow" : "red";

        String statusLine = "<" + ownerColor + ">" + sym().zoneOwned() + " " + ownerName
            + "</" + ownerColor + ">"
            + " <red>" + sym().zoneCapturing() + " UNDER ATTACK</red>";
        String labelLine = "<dark_gray>" + messages.get().zone().labelRemainingHold() + "</dark_gray>";
        String barLine = buildProgressBar(zs.captureProgress(), progressColor);

        return MM.msg(nameLine + "\n" + statusLine + "\n\n" + labelLine + "\n" + barLine);
    }

    private Component buildContestedText(String nameLine, ZoneSnapshot zs) {
        boolean hasOwner = zs.ownerTeamId() != null;

        String holdingTeamId = hasOwner ? zs.ownerTeamId() : zs.capturingTeamId();
        Team holding = holdingTeamId != null ? teamService.getTeam(holdingTeamId) : null;
        String color = holding != null ? holding.getColor() : "gray";
        String name = holding != null ? holding.getName() : "Neutral";

        double progress = hasOwner
            ? zs.teamOxygen().getOrDefault(zs.ownerTeamId(), 100.0)
            : zs.captureProgress();
        String progressColor = hasOwner
            ? (progress > 50 ? "aqua" : progress > 20 ? "yellow" : "red")
            : color;
        String labelLine = "<dark_gray>" + (hasOwner
            ? messages.get().zone().labelZoneOxygen()
            : messages.get().zone().labelCaptureProgress()) + "</dark_gray>";

        String statusLine = "<" + color + ">" + name + "</" + color + ">"
            + " <red>" + sym().zoneCapturing() + " CONTESTED</red>";
        String barLine = buildProgressBar(progress, progressColor);

        return MM.msg(nameLine + "\n" + statusLine + "\n\n" + labelLine + "\n" + barLine);
    }

    private Component buildCapturingText(String nameLine, ZoneSnapshot zs) {
        Team team = teamService.getTeam(zs.capturingTeamId());
        String color = team != null ? team.getColor() : "white";
        String name = team != null ? team.getName() : zs.capturingTeamId();

        String statusLine = "<" + color + ">" + sym().zoneCapturing() + " " + name + " capturing</" + color + ">";
        String labelLine = "<dark_gray>" + messages.get().zone().labelCaptureProgress() + "</dark_gray>";
        String barLine = buildProgressBar(zs.captureProgress(), color);

        return MM.msg(nameLine + "\n" + statusLine + "\n\n" + labelLine + "\n" + barLine);
    }

    private Component buildNeutralText(String nameLine) {
        return MM.msg(nameLine + "\n" + "<gray>" + sym().zoneNeutral() + " Neutral</gray>");
    }

    /**
     * Builds the private oxygen bar shown only to a specific teams members
     * <p>Always rendered regardless of zone state - draining, evacuating, refilling, or neutral</p>
     */
    private Component buildTeamOxygenText(String teamId, ZoneSnapshot zs, int maxCooldownTicks) {
        Team team = teamService.getTeam(teamId);
        double oxygen = zs.teamOxygen().getOrDefault(teamId, 100.0);
        String color = team != null ? team.getColor() : "gray";
        String name = team != null ? team.getName() : teamId;

        // EVACUATING - team still physically in zone after depletion
        if (zs.evacuatingTeamIds().contains(teamId)) {
            return MM.msg(
                "<" + color + ">" + sym().zoneOxygen() + " " + name + "</" + color + ">"
                + "\n<red><bold>" + sym().downedWarning() + " Evacuate the zone!</bold></red>"
            );
        }

        // REFILLING - oxygen recovering, cooldown may or may not be active
        if (zs.refillingTeamIds().contains(teamId)) {
            int cooldownTicks = zs.teamCooldownTicks().getOrDefault(teamId, 0);
            String oxygenColor = oxygen > 50 ? "aqua" : oxygen > 20 ? "yellow" : "red";

            String labelLine = "<" + color + ">" + sym().zoneOxygen() + " " + name
                + " " + sym().zoneRefilling() + "</" + color + ">";

            if (cooldownTicks > 0) {
                // Cooldown still active - show countdown bar
                int secondsLeft = (int) Math.ceil(cooldownTicks / 20.0);
                float cooldownProgress = maxCooldownTicks > 0
                    ? Math.clamp((float) cooldownTicks / maxCooldownTicks, 0f, 1f)
                    : 0f;

                String cooldownLabel = "<gray>Recapture ready in <white>" + secondsLeft + "s</white></gray>";
                String cooldownBar = buildProgressBar(cooldownProgress * 100, "gray");

                return MM.msg(labelLine + "\n" + buildProgressBar(oxygen, oxygenColor)
                    + "\n\n" + cooldownLabel + "\n" + cooldownBar);
            } else {
                // Cooldown expired - ready to recapture
                String readyLabel = "<green><bold>Ready to recapture!</bold></green>";
                return MM.msg(labelLine + "\n" + buildProgressBar(oxygen, oxygenColor)
                    + "\n\n" + readyLabel);
            }
        }

        // NORMAL - standard oxygen bar
        String oxygenColor = oxygen > 50 ? "aqua" : oxygen > 20 ? "yellow" : "red";
        String labelLine   = "<" + color + ">" + sym().zoneOxygen() + " " + name + "</" + color + ">";
        return MM.msg(labelLine + "\n" + buildProgressBar(oxygen, oxygenColor));
    }

    private String buildProgressBar(double progress, String color) {
        int filled = (int) Math.round((progress / 100.0) * 20);
        return "<" + color + ">" + sym().barFilled().repeat(filled) + "</" + color + ">"
            + "<dark_gray>" + sym().barEmpty().repeat(20 - filled) + "</dark_gray>"
            + " <white>" + (int) progress + "%</white>";
    }

    /* Helpers */

    private Location centerOf(ZoneDefinition def) {
        World world = plugin.getServer().getWorld(def.worldName());
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

    private MessageConfig.UiSymbols sym() { return messages.get().symbols(); }

}
