package com.creatorsplash.oxygenheist.platform.paper.world;

import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

/**
 * Interprets {@link PlayerSelectionService} selections as {@link ZoneDefinition} instances
 *
 * <p>Two creation modes are supported:</p>
 * <ul>
 *   <li>{@link #confirmCuboid} — uses both selection points as opposite corners</li>
 *   <li>{@link #confirmCircle} — uses the first selection point as center with a given radius</li>
 * </ul>
 */
@RequiredArgsConstructor
public final class ZoneSelectionService {

    private final PlayerSelectionService selectionService;

    /**
     * Attempts to confirm a cuboid zone from the players current two-point selection
     *
     * <p>Returns empty if the player has not set both points, or if the points
     * are in different worlds</p>
     *
     * @param playerId the player confirming the selection
     * @param id the stable lowercase zone id
     * @param displayName the human-readable display name
     */
    public Optional<ZoneDefinition.Cuboid> confirmCuboid(
        UUID playerId,
        String id,
        String displayName
    ) {
        if (!selectionService.hasSelection(playerId)) return Optional.empty();

        Location p1 = selectionService.getFirstPoint(playerId).orElseThrow();
        Location p2 = selectionService.getSecondPoint(playerId).orElseThrow();

        if (!p1.getWorld().equals(p2.getWorld())) return Optional.empty();

        return Optional.of(new ZoneDefinition.Cuboid(
            id,
            displayName,
            p1.getWorld().getName(),
            Math.min(p1.getX(), p2.getX()),
            Math.min(p1.getY(), p2.getY()),
            Math.min(p1.getZ(), p2.getZ()),
            Math.max(p1.getX(), p2.getX()),
            Math.max(p1.getY(), p2.getY()),
            Math.max(p1.getZ(), p2.getZ())
        ));
    }

    /**
     * Attempts to confirm a circle zone from the players first selection point
     *
     * <p>Returns empty if the player has not set their first point</p>
     *
     * @param playerId the player confirming the selection
     * @param id the stable lowercase zone id
     * @param displayName the human-readable display name
     * @param radius the zone radius in blocks
     */
    public Optional<ZoneDefinition.Circle> confirmCircle(
        UUID playerId,
        String id,
        String displayName,
        double radius
    ) {
        Optional<Location> p1 = selectionService.getFirstPoint(playerId);
        if (p1.isEmpty()) return Optional.empty();

        Location center = p1.get();
        return Optional.of(new ZoneDefinition.Circle(
            id,
            displayName,
            center.getWorld().getName(),
            center.getX(),
            center.getY(),
            center.getZ(),
            radius
        ));
    }

    /**
     * Clears the players selection after a successful zone confirmation
     */
    public void clearSelection(UUID playerId) {
        selectionService.clearSelection(playerId);
    }

}
