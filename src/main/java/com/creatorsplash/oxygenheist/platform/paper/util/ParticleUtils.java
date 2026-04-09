package com.creatorsplash.oxygenheist.platform.paper.util;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.destroystokyo.paper.ParticleBuilder;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Particle helpers that scope visibility to match participants
 */
@UtilityClass
public class ParticleUtils {

    private static final int DEBUG_FALLBACK_RADIUS = 48;

    /**
     * Spawns a particle scoped to match participants
     */
    public void spawn(
        Particle particle,
        Location location,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double extra,
        @Nullable MatchSession session
    ) {
        builder(particle, location, count, offsetX, offsetY, offsetZ, extra, null, session)
            .spawn();
    }

    /**
     * Spawns a particle with custom data (e.g. {@link Particle.DustOptions}) scoped to match participants
     */
    public <T> void spawn(
        Particle particle,
        Location location,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double extra,
        @Nullable T data,
        @Nullable MatchSession session
    ) {
        builder(particle, location, count, offsetX, offsetY, offsetZ, extra, data, session)
            .spawn();
    }

    /**
     * Returns a pre-configured {@link ParticleBuilder} with receivers already set
     * <p>Use this when you need to chain additional builder calls before spawning</p>
     */
    public <T> ParticleBuilder builder(
        Particle particle,
        Location location,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double extra,
        @Nullable T data,
        @Nullable MatchSession session
    ) {
        ParticleBuilder builder = new ParticleBuilder(particle)
            .location(location)
            .count(count)
            .offset(offsetX, offsetY, offsetZ)
            .extra(extra);

        if (data != null) builder.data(data);

        if (session != null) {
            builder.receivers(resolveReceivers(session));
        } else {
            // Debug bypass - no session, fall back to radius
            builder.receivers(DEBUG_FALLBACK_RADIUS, true);
        }

        return builder;
    }

    /* == Internals == */

    private Collection<Player> resolveReceivers(MatchSession session) {
        return session.getPlayers().stream()
            .map(mp -> Bukkit.getPlayer(mp.getPlayerId()))
            .filter(p -> p != null && p.isOnline())
            .toList();
    }

}
