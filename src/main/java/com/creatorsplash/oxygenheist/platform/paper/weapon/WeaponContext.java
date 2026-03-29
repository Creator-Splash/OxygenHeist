package com.creatorsplash.oxygenheist.platform.paper.weapon;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Contextual data passed into every weapon handler invocation
 *
 * <p>{@code effectsActive} is the key flag handlers should check before applying
 * any gameplay consequence (damage, knockback, status effects, ammo consumption)</p>
 */
public record WeaponContext(
    Player player,
    ItemStack item,
    MatchSession session,
    boolean effectsActive
) {}
