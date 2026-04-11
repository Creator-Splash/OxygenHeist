# OxygenHeist

A competitive team-based minigame for PaperMC 1.21.1 where teams fight to capture
zones and drain each others oxygen supply. The last team with oxygen remaining wins

---

## Gameplay Overview

Players are divided into teams and dropped into an arena. Scattered across the map
are capture zones - holding a zone drains oxygen from every opposing team. When a
team's oxygen hits zero it begins refilling, but while it refills that team cannot
sustain its players. Players who run out of personal oxygen are downed and must be
revived by a teammate before they bleed out and are eliminated.

The arena border shrinks as the match progresses, forcing teams into closer
confrontation. A variety of custom weapons with unique mechanics are available as
pickups throughout the map.

**Core loop:**
- Capture zones to drain enemy oxygen
- Down and eliminate enemy players
- Revive downed teammates before they bleed out
- Survive the shrinking border

---

## Requirements

| Requirement | Version |
|---|---|
| PaperMC | 1.21.1 |
| Java | 21 |

**Dependencies:**
- PlaceholderAPI - enables placeholders for scoreboards and other plugins

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
4. Running `/oh start` when ready

---

## Configuration

All configuration lives in `plugins/OxygenHeist/`

| File | Purpose                                                                         |
|---|---------------------------------------------------------------------------------|
| `config.yml` | Match tuning - duration, oxygen values, border timing, zone rates, downed config |
| `arena.yml` | Arena geometry and capture zone definitions - written by in-game commands       |
| `teams.yml` | Team definitions - written by in-game commands                                  |

### config.yml Overview

```yaml
match:
  duration-seconds: 600        # Total match length
  countdown-seconds: 10        # Pre-match countdown
  instant-death-seconds-remaining: 120  # When instant death begins

border:
  shrink-delay-seconds: 60     # How long before the border starts shrinking
  shrink-duration-seconds: 300 # How long the shrink takes
  shrink-size-percent: 20.0    # Shrink TO this percent of initial size (e.g. 500 -> 100)
  minimum-size: 10.0           # Border will never shrink below this size

oxygen:
  max: 300.0                   # Maximum personal oxygen per player
  drain-per-tick: 0.1          # How fast personal oxygen drains
  depletion-down-ticks: 200    # Ticks after depletion before player is downed

downed:
  bleedout-seconds: 30         # Time before a downed player is eliminated
  revive-ticks: 100            # Ticks required to complete a revive
  revive-max-distance: 3.0     # Maximum distance to revive a player
  intent-ttl-ticks: 5          # How long revive intent lasts without refreshing

zones:
  capture-rate-per-tick: 0.05
  drain-percent-per-second: 0.833
  max-drain-multiplier: 5
  refill-percent-per-second: 0.417
  capture-oxygen-restore: 50

weapons:
  friendly-fire: false
  spawner:
  initial-count: 3
  max-active: 8
  spawn-interval-seconds: 45
  pickup-radius: 1.5
  pickup-cooldown-seconds: 3
```

---

## Placeholders

Requires PlaceholderAPI. Use these in scoreboards, tab lists, or any other
plugin that supports PAPI

### Game

| Placeholder | Description |
|---|---|
| `%oxygenheist_game_state%` | Current match state (`WAITING`, `SETUP`, `PLAYING`, `ENDING`) |
| `%oxygenheist_game_state_display%` | Human-readable match state (`Waiting`, `Starting`, `In Progress`, `Ended`) |
| `%oxygenheist_game_time%` | Remaining time formatted as `MM:SS` |
| `%oxygenheist_game_time_seconds%` | Remaining time in seconds |
| `%oxygenheist_game_instant_death%` | `true` if instant death is active |

### Player

| Placeholder | Description |
|---|---|
| `%oxygenheist_player_team%` | The viewing player's team name, or `None` |
| `%oxygenheist_player_team_color%` | The viewing player's team color, or `gray` |
| `%oxygenheist_player_is_captain%` | `true` if the viewing player is their team's captain |
| `%oxygenheist_player_oxygen%` | The viewing player's current oxygen level |
| `%oxygenheist_player_is_downed%` | `true` if the viewing player is downed |
| `%oxygenheist_player_is_dead%` | `true` if the viewing player is eliminated |

### Teams

Replace `<id>` with the team's ID as defined in `teams.yml`

| Placeholder | Description |
|---|---|
| `%oxygenheist_team_<id>_score%` | Score for the given team |
| `%oxygenheist_team_<id>_members%` | Number of members on the given team |
| `%oxygenheist_team_<id>_captain%` | Name of the given team's captain, or `None` |
| `%oxygenheist_leading_team%` | Name of the team with the highest score, or `None` |
| `%oxygenheist_leading_team_score%` | Score of the leading team |
| `%oxygenheist_top_<n>_name%` | Name of the Nth-place team by score (e.g. `top_1_name`) |
| `%oxygenheist_top_<n>_score%` | Score of the Nth-place team (e.g. `top_1_score`) |
| `%oxygenheist_top_<n>_members%` | Member count of the Nth-place team (e.g. `top_1_members`) |

### Zones

Replace `<id>` with the zone's ID as defined in your arena config

| Placeholder | Description |
|---|---|
| `%oxygenheist_zone_count%` | Number of active zones |
| `%oxygenheist_zone_<id>_progress%` | Capture progress of the zone (0–100) |
| `%oxygenheist_zone_<id>_owner%` | Name of the team that owns the zone, or `Neutral` |
| `%oxygenheist_zone_<id>_capturing%` | Name of the team currently capturing the zone, or `None` |

---

## Architecture

OxygenHeist is built on a layered architecture with a strict separation of concerns

```
domain/          Pure Java game rules and state — no Bukkit dependency
application/     Orchestration services — coordinates domain objects
platform/paper/  Bukkit/Paper implementations — listeners, commands, display
```

The domain and application layers have no knowledge of PaperMC. This keeps
game logic portable, testable, and easy to reason about independently of the
platform

---

## Documentation

- [Command Reference](docs/Commands.md) - Full setup and command guide for server admins

---

## Credits

Built by CreatorSplash.
Original concept and gameplay design by the OxygenHeist team