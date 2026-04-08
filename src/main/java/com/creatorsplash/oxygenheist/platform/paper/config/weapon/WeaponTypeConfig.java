package com.creatorsplash.oxygenheist.platform.paper.config.weapon;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Fully parsed, typed configuration for a single weapon type
 */
public record WeaponTypeConfig(
   String id,
   boolean enabled,
   String itemId,
   int reloadFrames,
   AmmoConfig ammo,
   TimingConfig timing,
   CombatConfig combat,
   PhysicsConfig physics,
   EffectConfig effects
) {

    /* Ammo config */

    /**
     * Ammo capacity and starting ammo for this weapon
     *
     * <p>{@code startAmmo} defaults to {@code maxAmmo} in the loader unless
     * explicitly overridden (e.g. StealCrossbow starts at 0 - unloaded)</p>
     */
    public record AmmoConfig(int maxAmmo, int startAmmo) {}

    /* Timing */

    /**
     * All timing durations for this weapon, in milliseconds
     *
     * <p>Not every field is used by every weapon - handlers only read what
     * is relevant to them. Unused fields remain at their default (0)</p>
     *
     * <ul>
     *   <li>{@code reloadMs} - full reload duration</li>
     *   <li>{@code shotCooldownMs} - minimum time between individual shots</li>
     *   <li>{@code cooldownMs} - single-use cooldown (e.g, ClawCannon, SiltBlaster)</li>
     *   <li>{@code burstCooldownMs} - delay between bursts (e.g, SpikeShooter)</li>
     * </ul>
     */
    public record TimingConfig(
        long reloadMs,
        long shotCooldownMs,
        long cooldownMs,
        long burstCooldownMs
    ) {}

    /* Combat config */

    /**
     * Damage and range values for this weapon
     *
     * <p>Range-scaling fields ({@code closeRange*}, {@code longRange*}) are only
     * used by weapons with distance-based damage modifiers (e.g, NeedleRifle).
     * They default to 0 and have no effect if the handler does not read them.</p>
     */
    public record CombatConfig(
        double damage,
        double maxRange,
        int burstCount,
        double damagePerShot,
        double explosionRadius,
        double closeRangeDistance,
        double closeRangeBonusDamage,
        double longRangeDistance,
        double longRangeNerfDamage
    ) {}

    /* Physics config */

    /**
     * Motion and spatial values for this weapon
     *
     * <p>Fields are weapon-specific - only the relevant ones are read by each handler.
     * {@code aimSpreadMultiplier} defaults to {@code 0.7} (tighter spread when aiming)</p>
     */
    public record PhysicsConfig(
        double launchSpeed,
        double launchY,
        double meleeKnockback,
        double meleeKnockbackY,
        double coneAngle,
        double cloudRadius,
        double aimSpreadMultiplier
    ) {}

    /* Effects config */

    /**
     * Durations for status effects applied by this weapon, in ticks
     *
     * <p>{@code effectDurationTicks} - generic effect duration (e.g, SiltBlaster blindness/slowness)</p>
     * <p>{@code poisonDurationTicks} - poison effect duration (e.g, DartSlingshot, VenomSpitter)</p>
     */
    public record EffectConfig(
        int effectDurationTicks,
        int poisonDurationTicks
    ) {}

}
