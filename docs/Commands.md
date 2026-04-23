# OxygenHeist — Admin Command Reference

A complete guide to setting up and running OxygenHeist on your server  
All commands use the `/oxygenheight` or `/oh` prefix

---

## Quick Start Checklist

Before you can run a match, you need to complete these steps in order:

- [ ] Get the selection wand
- [ ] Set your arena boundary
- [ ] Create at least one capture zone
- [ ] Configure your teams in `teams.yml`, then set their bases — `/oh team setbase`
- [ ] Add players to teams — `/oh team add`
- [ ] Start the match

---

## Reload

Reloads all configuration files from disk without restarting the server.
Safe to run at any time, including during a match - changes take effect
immediately on the next event that reads from config.
```
/oh reload
```

> **Note:** Reloading during an active match will not affect the current
> match session (e.g. already-configured durations). Display messages and
> sounds update immediately.

---

## The Selection Wand

The selection wand is the tool used to define areas in the world - both the arena
boundary and capture zones use it. You only need one wand for everything

```
/oh wand
```

Left-click a block to set **Point 1**
Right-click a block to set **Point 2**

Once both points are set, a message will appear in chat showing you which setup
command to run next. Your selection stays active until you run a command that
consumes it, or until you clear it manually

> **Tip:** Your selection persists between commands - you can set Point 1, walk
> to the other side of your arena, then set Point 2

---

## Arena Setup

The arena defines the world border boundary for the match. You need exactly one
arena configured before any match can start

### Set the Arena

Stand inside your intended arena area and use the wand to select two opposite
corners - one at the north-west edge and one at the south-east edge. Height
does not matter for the border

```
/oh arena set
```

This saves the arena to `arena.yml`. The border center and size are calculated
automatically from your two selected points

### View Arena Info

```
/oh arena info
```

Shows the currently saved arena world, center coordinates, and border size

### Clear Your Selection

If you made a mistake and want to start your selection over:

```
/oh arena clear
```

This only clears your pending wand selection - it does not delete the saved arena

---

## Zone Setup

Capture zones are the points on the map that teams fight over. You can have as
many zones as you like. Two shapes are supported

### Cuboid Zone (Recommended)

Use the wand to select two opposite corners of the zone area - the zone will
cover everything inside that box. This is the most precise option and works
well for rooms, platforms, and defined areas

```
/oh zone set <id> [displayName]
```

| Argument | Description |
|---|---|
| `id` | A unique lowercase name for this zone used internally, e.g. `center` |
| `displayName` | Optional. The name shown to players in the UI, e.g. `"Center Point"`. Defaults to the id if not provided. |

**Example:**
```
/oh zone set center "Center Point"
/oh zone set north_tower "North Tower"
/oh zone set bunker
```

### Circle Zone

If you want a roughly circular zone, use the wand to set just **Point 1** as
the center of the zone, then provide a radius. The zone will be a vertical
cylinder - any player within the radius at any height is counted as inside

```
/oh zone setcircle <id> <radius> [displayName]
```

| Argument | Description |
|---|---|
| `id` | A unique lowercase name for this zone |
| `radius` | Radius in blocks. Must be between 1 and 100 |
| `displayName` | Optional display name shown to players |

**Example:**
```
/oh zone setcircle mid 12 "Mid"
/oh zone setcircle spawn 8 "Spawn Point"
```

### Remove a Zone

```
/oh zone remove <id>
```

Permanently removes the zone from `arena.yml`. This takes effect on the next
match start — it does not affect a match that is already running

### List All Zones

```
/oh zone list
```

Shows all configured zones with their shape, id, display name, and world

---

## Team Setup

Teams persist between matches and are defined in `teams.yml`. Use the commands
below to manage them at runtime. Each team needs at least one member and a base
location before a match can start.

> **Note:** To create or delete teams, edit `teams.yml` directly and run `/oh reload`

### Set a Team's Base Spawn

Stand at the location where the team should spawn at match start, then run:

```
/oh team setbase <name>
```

**Example:**
```
/oh team setbase red
```

### Add a Player to a Team

```
/oh team add <name> <player>
```
Applies team armour immediately and moves the player out of spectator mode.

### Remove a Player from a Team

```
/oh team remove <name> <player>
```
Removes team armour and places the player into spectator mode

### Change a Teams Color

Changes the armour dye color and display color for the team.

```
/oh team color <team> <color>
```
Valid colors are standard MiniMessage color names, e.g. `red`, `blue`, `green`,
`yellow`, `aqua`, `gold`, `light_purple`, `white`.

### View Team Info

```
/oh team info <team>
```

Shows team ID, member count, captain, and base location status.

### List All Teams

```
/oh team list
```

Shows all teams with member counts and whether their base has been set

---

## Match Commands

Once the arena, zones, and teams are all configured you are ready to run a match

### Start a Match

```
/oh start
```

Creates the match session, loads all configured zones, assigns players to their
teams, teleports everyone to their team bases, and begins the countdown

### End a Match

```
/oh end
```

Immediately ends the current match and returns players to the lobby. Can be used
at any time during a match

---

## Debug Commands

These commands are intended for testing and troubleshooting. They require the
`oxygenheist.debug` permission

### Force Down a Player

Puts a player into the downed state as if their oxygen ran out

```
/oh down <player> [ticks]
```

`ticks` is optional - defaults to 200 ticks (10 seconds) if not provided

### Force Revive a Player

Immediately revives a downed player

```
/oh revive <player>
```

---

## Permissions

| Permission              | Description | Default |
|-------------------------|---|---|
| `com.oxygenheist.arena` | Access to all `/oh arena` commands | OP |
| `com.oxygenheist.zone`      | Access to all `/oh zone` commands | OP |
| `com.oxygenheist.selection` | Use the selection wand | OP |
| `com.oxygenheist.team`      | Access to all `/oh team` commands | OP |
| `com.oxygenheist.game`      | Access to `/oh start` and `/oh end` | OP |
| `com.oxygenheist.debug`     | Access to debug commands | OP |
| `com.oxygenheist.admin` | Access to all setup and reload commands | OP |

---

## Full Setup Example

```
/oh wand
```
Left-click one corner of your arena, right-click the opposite corner.
```
/oh arena set
```
Walk to the center of your first zone, left-click, walk to the far edge, right-click.
```
/oh zone set center "Center"
/oh zone set north "North Tower"
```
Stand at the red team spawn:
```
/oh team setbase red
```
Stand at the blue team spawn:
```
/oh team setbase blue
```
Add players:
```
/oh team add Steve red
/oh team add Alex blue
```
Start the match:
```
/oh start
```

The countdown will begin and players will be teleported to their team bases