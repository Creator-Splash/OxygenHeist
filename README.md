# OxygenHeist

A competitive team-based minigame for PaperMC 1.21.8 where teams fight to capture
zones and drain each other's oxygen supply. The last team with oxygen remaining wins.

---

## Gameplay Overview

Players are divided into teams and dropped into an arena. Scattered across the map are
capture zones. When your team holds a zone, players standing on it are protected from
oxygen drain — but holding a zone consumes its oxygen supply. When a zone's oxygen hits
zero, it stops protecting players and begins refilling.

Capturing a zone restores oxygen to your whole team immediately. The strategic tension
is between using zones as safe areas (burning their oxygen) and capturing new zones to
restore your team's personal supply.

Players who run out of personal oxygen are downed and must be revived by a teammate
before they bleed out and are eliminated. The last team standing wins, or the team with
the most points when the timer expires.

The arena border shrinks as the match progresses, forcing teams into closer
confrontation. Nine custom weapons with unique mechanics are available as pickups
scattered throughout the map.

**Core loop:**
- Capture zones to restore your team's oxygen
- Hold zones to protect players from personal oxygen drain
- Down and eliminate enemy players with custom weapons
- Revive downed teammates before they bleed out
- Survive the shrinking border

---

## Requirements

| Requirement | Version |
|---|---------|
| PaperMC | 1.21.8  |
| Java | 21      |

**Optional dependencies:**
- PlaceholderAPI — enables placeholders for scoreboards and other plugins

**Custom item plugin (one required):**
- ItemsAdder
- Nexo

---

## Installation

1. Download the latest `OxygenHeist.jar` from the releases page
2. Drop it into your server's `plugins/` folder
3. Start the server once to generate the default config files
4. Follow the setup guide below or refer to the [Command Reference](docs/Commands.md)

---

## Setup

Full step-by-step setup instructions including all commands are in the
[Command Reference](docs/Commands.md)

At a high level, setup involves:

1. Defining the arena boundary with the selection wand
2. Placing capture zones around the map
3. Creating teams and setting their base spawn locations
4. Configuring your custom item plugin and setting `weapons.item-provider` in `config.yml`
5. Running `/oh start` when ready

---

## Weapons

Nine weapons are available as map pickups. Each has unique mechanics, configurable
stats, and animated visual states driven by your custom item plugin.

| Weapon | Style | Mechanic |
|---|---|---|
| Silt Blaster | Thrown grenade | Blindness, slowness, nausea and inverted controls on detonation |
| Venom Spitter | Hold to spray | Stacking poison on hit, continuous fire while holding right-click |
| Claw Cannon | Rocket launcher | Explosive projectile with melee knockback |
| Dart Slingshot | Semi-auto | Stacking poison darts, tight spread when aiming |
| Needle Rifle | Slow semi-auto | Range-scaled damage — bonus at close range, penalty at long range |
| Spike Shooter | Burst fire | Fire a burst of three spikes while aiming, movement cancels burst |
| Reef Harpoon Gun | Single shot | High damage, slowness on hit, recoil on fire, auto-reloads |
| Manta Ray | Electric blast | Raycast beam expanding into a damage cone, reflects off solid blocks |
| Reclaimer Crossbow | Steal | Fires a bolt that steals the target's held weapon on hit |

All weapons share a unified control scheme:
- **Right-click** — shoot / throw
- **Left-click** — reload
- **Shift** — aim (tightens spread, never required to fire)

---

## Configuration

All configuration lives in `plugins/OxygenHeist/`

| File | Purpose |
|---|---|
| `config.yml` | Match tuning, weapon provider, downed config, border timing |
| `weapons.yml` | Per-weapon stats, frame IDs, sounds, ammo, and display names |
| `arena.yml` | Arena geometry and capture zone definitions — written by in-game commands |
| `teams.yml` | Team definitions — written by in-game commands |
| `messages.yml` | All player-facing messages, titles, and sounds |

### config.yml Overview

```yaml
match:
  duration-seconds: 600
  countdown-seconds: 10
  instant-death-seconds-remaining: 120

border:
  shrink-delay-seconds: 60
  shrink-duration-seconds: 300
  shrink-size-percent: 20.0
  minimum-size: 10.0

oxygen:
  max: 300.0
  drain-per-tick: 0.1

downed:
  bleedout-seconds: 30
  revive-ticks: 100
  revive-max-distance: 3.0
  intent-ttl-ticks: 5
  invulnerable-while-downed: true
  kill-credit-window-seconds: 30
  down-credit-window-seconds: 10

weapons:
  item-provider: itemsadder    # itemsadder | nexo
  friendly-fire: false
  physical-ammo-display: true  # shows ammo count in offhand slot
  spawner:
    initial-count: 12
    minimum-on-field: 6
    maximum-on-field: 16
    pickup-radius: 1.5
    pickup-cooldown-seconds: 3
```

### weapons.yml Overview

Each weapon entry supports the following top-level fields:

```yaml
venom_spitter:
  display-name: "Venom Spitter"
  enabled: true
  reload-frames: 1
  ammo:
    max: 25
    start: 25            # defaults to max if omitted
    display-item: SLIME_BALL  # frame key or plain material name
  reload-ms: 5000
  shot-cooldown-ms: 200
  damage-per-shot: 0.5
  max-range: 7.0
  poison-duration-ticks: 20
  frames:
    idle: "oxygenheist:venom_spitter_idle"
    charged: "oxygenheist:venom_spitter_charged"
    # reload_0 derived automatically by convention if not listed
  sounds:
    fire:
      sound: "entity.blaze.shoot"
      volume: 1.0
      pitch: 1.0
```

Frame IDs follow your custom item plugin's format:
- **ItemsAdder:** namespaced ID e.g. `"oxygenheist:venom_spitter_idle"`
- **Nexo:** bare item ID e.g. `"venom_spitter_idle"`

Reload frames are derived automatically by convention from the idle frame prefix
(`venom_spitter_idle` → `venom_spitter_reload_0`, `venom_spitter_reload_1`, etc.)
unless explicitly overridden in the `frames` block.

---

## Placeholders

Requires PlaceholderAPI.

### Game

| Placeholder | Description |
|---|---|
| `%oxygenheist_game_state%` | Current match state (`WAITING`, `SETUP`, `PLAYING`, `ENDING`) |
| `%oxygenheist_game_state_display%` | Human-readable match state |
| `%oxygenheist_game_time%` | Remaining time formatted as `MM:SS` |
| `%oxygenheist_game_time_seconds%` | Remaining time in seconds |
| `%oxygenheist_game_instant_death%` | `true` if instant death is active |

### Player

| Placeholder | Description |
|---|---|
| `%oxygenheist_player_team%` | The player's team name, or `None` |
| `%oxygenheist_player_team_color%` | The player's team colour, or `gray` |
| `%oxygenheist_player_oxygen%` | The player's current oxygen level |
| `%oxygenheist_player_is_downed%` | `true` if the player is downed |
| `%oxygenheist_player_is_dead%` | `true` if the player is eliminated |
| `%oxygenheist_player_is_captain%` | `true` if the player is their team's captain |
| `%oxygenheist_player_score%` | The player's current score |

### Weapons

| Placeholder | Description |
|---|---|
| `%oxygenheist_weapon_id%` | Config ID of the held weapon, or empty |
| `%oxygenheist_weapon_ammo%` | Current ammo count |
| `%oxygenheist_weapon_maxammo%` | Max ammo for the held weapon |
| `%oxygenheist_weapon_reloading%` | `true` if the weapon is reloading |

### Teams

Replace `` with the team's ID as defined in `teams.yml`

| Placeholder | Description |
|---|---|
| `%oxygenheist_team__score%` | Score for the given team |
| `%oxygenheist_team__members%` | Number of members on the given team |
| `%oxygenheist_leading_team%` | Name of the team with the highest score, or `None` |
| `%oxygenheist_leading_team_score%` | Score of the leading team |
| `%oxygenheist_top__name%` | Name of the Nth-place team by score |
| `%oxygenheist_top__score%` | Score of the Nth-place team |
| `%oxygenheist_top__members%` | Member count of the Nth-place team |

### Zones

Replace `` with the zone's ID as defined in `arena.yml`

| Placeholder | Description |
|---|---|
| `%oxygenheist_zone_count%` | Number of active zones |
| `%oxygenheist_zone__progress%` | Capture progress of the zone (0–100) |
| `%oxygenheist_zone__owner%` | Name of the owning team, or `Neutral` |
| `%oxygenheist_zone__capturing%` | Name of the team currently capturing, or `None` |
| `%oxygenheist_zone__oxygen_%` | Zone oxygen percentage for a specific team (0–100) |

---

## Architecture

OxygenHeist is built on a layered architecture with a strict separation of concerns:

```
domain/          Pure Java game rules and state - no Bukkit dependency
application/     Orchestration services - coordinates domain objects
platform/paper/  Bukkit/Paper implementations - listeners, commands, display
```

The domain and application layers have no knowledge of PaperMC. This keeps game logic
portable, testable, and easy to reason about independently of the platform.

Weapon behaviour is implemented via a handler interface with a shared base class
providing reload, ammo, and frame switching. Item appearance is fully decoupled from
game logic through a `WeaponItemProvider` abstraction supporting multiple custom item
plugins.

---

## Documentation

- [Command Reference](docs/Commands.md) - Full setup and command guide for server admins
- [What's New](docs/WhatsNew.md) - Full changelog comparing the rewrite to the original

---

## Credits

Built by CreatorSplash.
Original concept and gameplay design by the OxygenHeist team.