package com.creatorsplash.oxygenheist.platform.paper.weapon;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Central registry mapping items to their {@link WeaponHandler}
 *
 * <p>All weapon handlers must be registered here at startup via {@link #register(WeaponHandler)}.
 * This is the single source of truth for which weapons exist in the game</p>
 */
public final class WeaponRegistry implements MatchLifecycle {

    private final Map<String, WeaponHandler> handlers = new LinkedHashMap<>();
    private final Random random = new Random();

    /**
     * Registers a weapon handler
     *
     * @throws IllegalArgumentException if a handler with the same id is already registered
     */
    public void register(WeaponHandler handler) {
        if (handlers.containsKey(handler.id())) {
            throw new IllegalArgumentException(
                "Weapon handler already registered with id '" + handler.id() + "'"
            );
        }
        handlers.put(handler.id(), handler);
    }

    /**
     * Finds the handler that owns the given item, or null if none match
     *
     * <p>Called on every relevant player event - implementations of
     * {@link WeaponHandler#handles} must be fast</p>
     */
    public @Nullable WeaponHandler find(ItemStack item) {
        if (item == null) return null;
        for (WeaponHandler handler : handlers.values()) {
            if (handler.handles(item)) return handler;
        }
        return null;
    }

    /**
     * Returns the handler registered under the given id, or null if not found
     */
    public @Nullable WeaponHandler get(String id) {
        return handlers.get(id);
    }

    /**
     * Returns a random handler from the registry
     *
     * <p>Used by the distribution and spawner systems to assign weapons randomly.
     * Caller should verify the registry is non-empty before calling</p>
     *
     * @throws IllegalStateException if no handlers are registered
     */
    public WeaponHandler random() {
        if (handlers.isEmpty()) {
            throw new IllegalStateException("Cannot select a random weapon - no handlers are registered");
        }
        List<WeaponHandler> list = new ArrayList<>(handlers.values());
        return list.get(random.nextInt(list.size()));
    }

    /**
     * Returns an unmodifiable view of all registered handlers
     */
    public List<WeaponHandler> all() {
        return List.copyOf(handlers.values());
    }

    /* Lifecycle */

    @Override
    public void onMatchEnd() {
        handlers.values().forEach(WeaponHandler::onMatchEnd);
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        handlers.values().forEach(h -> h.onPlayerLeave(playerId));
    }

}
