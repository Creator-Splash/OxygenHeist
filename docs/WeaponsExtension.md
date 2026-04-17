# Future Weapon System Extensions

This document covers two planned post-MVP extensions to the weapon system:
a config-driven base projectile handler, and a Skript integration layer

---

## 1. BaseProjectileHandler

### Motivation

Several weapons share identical structure - fire a projectile on right-click,
apply effects on hit, reload on right-click. The only differences are numbers
and effect types. Writing a full Java handler for each is unnecessary once
the config system is expressive enough to describe that behaviour

### New Config Fields

Add a `projectile` block to `weapons.yml`:

```yaml
generic_rifle:
  enabled: true
  item-id: "oxygenheist:generic_rifle"
  reload-frames: 2
  ammo: 6
  reload-ms: 3000
  shot-cooldown-ms: 500
  damage: 8.0
  max-range: 15.0
  projectile:
    type: ARROW                  # Bukkit EntityType - ARROW, SNOWBALL, TRIDENT etc
    speed: 3.0
    gravity: true
    burst-count: 1               # >1 enables burst fire mode
    burst-cooldown-ms: 0
    on-hit-effects:
      - "POISON:60:0"            # type:durationTicks:amplifier
      - "SLOWNESS:40:1"
    particles:
      trail: "DUST:GREEN:1.2"
      hit: "EXPLOSION"
    sounds:
      fire: "entity.arrow.shoot"
      hit: "entity.arrow.hit"
    aim-spread-multiplier: 0.7
```

### New Record

Add `ProjectileConfig` as an inner record of `WeaponTypeConfig`:

```java
/**
* Present only for weapons using BaseProjectileHandler.
* Null for weapons with custom Java handler implementations.
*/
public record ProjectileConfig(
  EntityType type,
  double speed,
  boolean gravity,
  int burstCount,
  long burstCooldownMs,
  List<OnHitEffect> onHitEffects,
  @Nullable String trailParticle,
  @Nullable String hitParticle
) {
public record OnHitEffect(PotionEffectType type, int durationTicks, int amplifier) {}
}
```

### Implementation

`BaseProjectileHandler` extends `ReloadableWeaponHandler` - it gets ammo,
reload animation, sounds, and lifecycle for free. It only implements:

- `id()` - returns the config id
- `onRightClick()` - fires projectile(s) with configured speed/gravity
- `onProjectileHit()` - applies configured damage, effects, particles

```java
public final class BaseProjectileHandler extends ReloadableWeaponHandler {

    private final String id;

    public BaseProjectileHandler(WeaponTypeConfig config, WeaponItemProvider provider) {
        super(config, provider);
        this.id = config.id();
    }

    @Override public String id() { return id; }

    @Override
    public void onRightClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        // fire projectile(s) from config.projectile()
        // burst logic driven by config.projectile().burstCount()
    }

    @Override
    public void onProjectileHit(WeaponProjectileContext ctx) {
        // apply config.projectile().onHitEffects()
        // apply damage from config.combat().damage()
        // spawn config.projectile().hitParticle()
    }
}
```

### Auto-Registration

In `OxygenHeistPlugin.onEnable`, after explicit handlers are registered,
sweep the config for any weapon with a `projectile:` block that has no
registered handler yet:

```java
weaponConfig.getAll().stream()
    .filter(c -> c.enabled())
    .filter(c -> c.projectile() != null)
    .filter(c -> !weaponRegistry.has(c.id()))
    .forEach(c -> weaponRegistry.register(
        new BaseProjectileHandler(c, weaponItemProvider)
    ));
```

This means:
- Adding a new projectile weapon requires only a `weapons.yml` entry
- No Java code, no recompile, no server restart beyond a `/oh reload`
- Custom handlers always take precedence — auto-registration only fills gaps

### What BaseProjectileHandler Cannot Cover

These weapons will always need explicit Java handlers:

| Weapon | Reason |
|---|---|
| ClawCannon | Launches the player, not a projectile |
| VenomSpitter | Continuous per-tick raycast spray |
| SiltBlaster | Area cloud, no projectile |
| StealCrossbow | Steals items from the target — unique logic |

Everything else is a reasonable candidate for config-driven behaviour.

---

## 2. Skript Weapon Integration

### Motivation

The server admin or a Skript developer should be able to add simple weapons
or augment existing ones without writing Java. The weapon system already
exposes the right hooks — we just need to surface them.

### Approach

Skript weapons are **not** registered as `WeaponHandler` implementations.
Instead they are declared in `weapons.yml` with a `skript: true` flag,
which tells the system to stamp the PDC identity key on the item (so it is
recognised as a weapon), but skip Java handler registration. All game logic
is driven by Skript via the command API.

### Skript Flag in weapons.yml

```yaml
shockwave_rod:
  enabled: true
  item-id: "oxygenheist:shockwave_rod"
  reload-frames: 0
  skript: true          # no Java handler - behaviour implemented in Skript
  cooldown-ms: 3000
  # other numeric config still works - readable via PAPI placeholders in Skript
```

### Command API

Expose a set of console-callable commands that Skript can invoke via
`run console command`. These are thin wrappers around the existing Java
services:

```
/oh weapon setammo <player> <amount>
/oh weapon cooldown <player> <weaponId> <ms>
/oh weapon reload <player>
/oh weapon give <player> <weaponId>
/oh weapon cleareffects <player>
```

A Skript weapon then looks like (or idk I don't use Skript):

```
on right click holding {_item}:
    set {_id} to papi placeholder "oxygenheist_weapon_id" for player
    if {_id} is "shockwave_rod":
        run console command "/oh weapon cooldown %player% shockwave_rod 3000"
    # custom shockwave logic here
    run console command "/oh weapon setammo %player% -1"
```

### PAPI Placeholders Required

These placeholders are needed for Skript to read weapon state:

| Placeholder | Returns |
|---|---|
| `%oxygenheist_weapon_id%` | Config id of held weapon, or empty |
| `%oxygenheist_weapon_ammo%` | Current ammo count |
| `%oxygenheist_weapon_maxammo%` | Max ammo for held weapon |
| `%oxygenheist_weapon_oncooldown%` | `true` / `false` |
| `%oxygenheist_weapon_cooldown_remaining%` | Remaining ms, or 0 |
| `%oxygenheist_weapon_reloading%` | `true` / `false` |

Most of these are already useful regardless of Skript and should be
implemented during the PAPI pass.

### ScriptWeaponRegistry

A simple set of weapon IDs that are Skript-owned. `WeaponListener` uses
this to skip "unknown weapon" warnings for these IDs, and the debug give
command uses it to create items for Skript weapons via the provider:

```java
public final class ScriptWeaponRegistry {
private final Set<String> scriptWeaponIds = new HashSet<>();

    public void register(String weaponId) {
        scriptWeaponIds.add(weaponId);
    }

    public boolean isScriptWeapon(String weaponId) {
        return scriptWeaponIds.contains(weaponId);
    }
}
```

Populated at startup from `weapons.yml` entries marked `skript: true`.

### What Skript Weapons Can Reasonably Do

- One-shot or cooldown-based effects (sounds, particles, potions)
- Simple area effects via Skripts entity query system
- Triggering other Skript scripts or functions
- Reading and writing ammo/cooldown state via the command API

### What Skript Weapons Cannot Reasonably Do

- Per-tick continuous logic (tick rate too unpredictable from Skript)
- Projectile tracking integrated with `WeaponProjectileTracker`
- Match lifecycle hooks (onMatchEnd, onPlayerLeave)
- Anything requiring reliable millisecond timing

For weapons that need any of the above, a Java handler is the correct path.

---

## Implementation Order

When the time comes, the recommended order is:

1. PAPI placeholders - unblocks Skript integration and is useful standalone
2. Command API endpoints - unblocks Skript weapons and debug tooling
3. `ScriptWeaponRegistry` + `skript: true` flag parsing
4. `ProjectileConfig` record + parser additions to `WeaponConfigService`
5. `BaseProjectileHandler` implementation
6. Auto-registration sweep in `onEnable`

Neither feature depends on the other, but the PAPI + command work in steps
1–3 has value independent of both and should come first