# OxygenHeist - What's New in the Rewrite

---

## The Short Version

The original OxygenHeist plugin was written as a quick prototype. It worked, but it was
built in a way that made bugs hard to track down, settings hard to change, and anything
new hard to add without breaking something else. The rewrite keeps the game feeling
identical to players while rebuilding everything underneath to be reliable, maintainable,
and easier to configure and extend.

---

## What Actually Changed

### How the Plugin is Organised Internally

**Before:** Everything lived in a handful of giant files. One file handled the game timer,
player damage, zone capture, oxygen levels, boss bars, and saving data - all at once.
When something broke it was hard to even find where the problem was.

**After:** The plugin is split into clear layers with one job each. Game rules live
separately from Minecraft-specific code. Display code lives separately from gameplay
logic. This means a bug in the boss bar display can't accidentally corrupt match state,
and a change to how oxygen drains doesn't require touching the command system.

---

### Team Colours

**Before:** Team colours were stored as raw RGB values and translated with a manual
lookup table - a hardcoded list of if-statements that tried to guess the closest chat
colour from the RGB numbers. This was fragile and produced wrong colours regularly.
Hex colours didn't work at all.

**After:** Colours use Minecrafts own colour system directly. You can use any named
colour (`red`, `dark_blue`, `gold`, etc.) or a full hex code like `#FF5500`. Whatever
you set is what the leather armour and chat text show. No guessing, no lookup table.

---

### Team Commands - What's New

The original plugin was missing several team management commands that were documented
but never built. These are all now implemented:

| Command | What it does |
|---|---|
| `/oh team create <name> <colour>` | Creates a new team with the given colour |
| `/oh team delete <name>` | Removes a team and clears all its members |
| `/oh team setbase <name>` | Sets the team's spawn point to where you're standing |
| `/oh team ff` | Toggles friendly fire on or off globally |

The `list` command now also shows at a glance whether each team has a base set, so you
can spot missing setup before trying to start a match.

---

### Team Configuration Saving

**Before:** When you saved teams, only the member list and captain were written to the
config file. If you restarted the server, name and colour changes were lost because
they were never saved.

**After:** Every team property - name, colour, base location, members, captain, and
friendly fire setting - is saved to `teams.yml` on every change. Deleting a team also
cleanly removes it from the file rather than leaving stale data behind.

---

### Match Start Validation

**Before:** Typing `/oh start` would attempt to start a match regardless of whether
everything was actually set up. Running it with no arena configured, no teams, or teams
missing their base locations would either crash the plugin or produce a silent broken
match with no useful error.

**After:** The start command checks everything before doing anything. If something is
missing you get a specific message telling you exactly what needs fixing:

```
No arena configured. Use /oh arena set first.
Team 'Blue' has no base set. Use /oh team setbase blue
```

---

### Player Preparation at Match Start

**Before:** When a match started, players were not reliably teleported, their gamemode
wasn't set, and their inventory wasn't cleared. Whether or not you spawned at your team
base depended on timing and luck.

**After:** When the countdown completes and the match begins, every team member is
teleported to their team's base, switched to Survival mode, given full health and food,
and has their inventory cleared. Team armour is applied immediately. This happens
cleanly for every player every time.

---

### Match End Cleanup

**Before:** When a match ended, players were left in whatever state they were in.
Players who had been downed or had potion effects still had those effects after the
match. Inventory wasn't cleared. Gamemodes weren't reset.

**After:** When a match ends, all players are switched to Adventure mode, every potion
effect is removed, sneaking is cancelled, inventory is cleared, and health and food are
restored to full. The world border is also reset to its original size.

---

### Oxygen and Air Bubbles

**Before:** Minecraft's vanilla drowning system ran in parallel with the oxygen system,
meaning players could take drowning damage from the vanilla engine at the same time the
plugin was managing their oxygen - causing double-damage or unexpected deaths from the
wrong source.

**After:** Vanilla drowning damage is fully suppressed for players in a match. The
oxygen system is the only thing that governs your air bar and downed state. No
interference from Minecraft's built-in mechanics.

---

### Capture Zone Events

**Before:** When a team captured a zone, the broadcast message and the oxygen restore
notification were never sent. The game logic for capturing worked, but the messages
telling players about it were silently dropped.

**After:** Zone capture events are fully wired. When a team captures a zone, the
broadcast goes out to the server, and the oxygen restore message is sent specifically to
the capturing team's members.

---

### Win Conditions

**Before:** The only way a match could end was by the timer running out. If one team
eliminated every other team's players entirely, the match would just keep running on an
empty server until the clock hit zero.

**After:** The match checks every tick whether only one team still has active players.
If all opposing teams have been fully eliminated, the match ends immediately and that
team wins. The timer is still there as a fallback if no team gets fully eliminated.

---

### Player Disconnect During Match

**Before:** If a player disconnected while they were in the downed state, their bleedout
timer kept counting in the background with no player attached to it. This left orphaned
timers running in memory for the rest of the match.

**After:** Disconnecting while downed immediately triggers elimination. No ghost timers,
no leaked state.

---

### Friendly Fire Handling

**Before:** Friendly fire was checked inside the team listener, which ran at the wrong
priority relative to the combat system. In some cases the friendly fire cancel would
fire too late, meaning damage had already been partially processed.

**After:** Friendly fire is cancelled at the lowest possible priority, before any combat
logic runs. The combat system is also set to completely ignore events that have already
been cancelled, so there's no overlap or double-processing.

---

### Boss Bar Display

**Before:** Boss bars used the legacy Bukkit API, which renders as plain text with
old-style `§` colour codes. Customisation was limited and the text format was fixed
in code.

**After:** All boss bars use the modern Adventure API with full MiniMessage formatting
support. The countdown bar, the match timer, and the per-player downed bars all use
the configurable message format from `messages.yml`, so you can change the text and
colours without touching any code.

---

### World Border

**Before:** The border shrink used incorrect time units internally, meaning the shrink
animation took 20× longer than configured. A setting of 300 seconds would produce a
shrink that lasted nearly 2 hours.

**After:** The border uses the correct time units. The `shrink-duration-seconds` value
in `config.yml` now means exactly what it says.

---

### Configuration

**Before:** All weapons, match settings, and display values were hardcoded. Changing
something like a weapon's damage required editing the source code and recompiling the
plugin.

**After:** Weapons have a dedicated `weapons.yml` where every stat - damage, cooldowns,
ammo, reload time, visual model IDs - can be edited without touching code. Match
settings, messages, and sounds are all in their respective config files.

---

### Messages and Sounds

**Before:** All messages were hardcoded English strings scattered throughout the code.
Changing a message meant finding it in the source and recompiling.

**After:** Every player-facing message, title, subtitle, and sound lives in `messages.yml`.
Messages use MiniMessage formatting so you can use any colour, gradient, bold, italic,
or hover text. Sounds have configurable volume and pitch.

---

### Commands System

**Before:** Commands were registered manually in Bukkit's legacy system with handwritten
tab-completion lists that frequently went out of sync with what commands actually existed.

**After:** Commands use the Cloud v2 framework, which handles tab-completion, argument
parsing, and error messages automatically. Invalid arguments get a useful error rather
than silently failing. All permission checks are declared in one place.

---

### PlaceholderAPI Support

**Before:** A basic set of placeholders existed but team and zone information was largely
missing. The `%oxygenheist_player_oxygen%` placeholder returned team-average oxygen
rather than the individual player's oxygen level.

**After:** The existing placeholders are correct. A full set of team placeholders
(`player_team`, `player_team_points`, `player_is_captain`, `team_<id>_points`, etc.)
and additional zone placeholders are on the roadmap for the next session.

---
