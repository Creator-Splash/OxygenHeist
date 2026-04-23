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

**After:** Colours use Minecraft's own colour system directly. You can use any named
colour (`red`, `dark_blue`, `gold`, etc.) or a full hex code like `#FF5500`. Whatever
you set is what the leather armour and chat text show. No guessing, no lookup table.

---

### Team Commands

The original plugin was missing several team management commands that were documented
but never built. These are all now implemented:

| Command | What it does |
|---|---|
| `/oh team create  ` | Creates a new team with the given colour |
| `/oh team delete ` | Removes a team and clears all its members |
| `/oh team setbase ` | Sets the team's spawn point to where you're standing |

The `list` command now also shows at a glance whether each team has a base set, so you
can spot missing setup before trying to start a match.

---

### Team Configuration Saving

**Before:** When you saved teams, only the member list and captain were written to the
config file. If you restarted the server, name and colour changes were lost.

**After:** Every team property - name, colour, base location, members, captain, and
friendly fire setting - is saved to `teams.yml` on every change. Deleting a team also
cleanly removes it from the file rather than leaving stale data behind.

---

### Match Start Validation

**Before:** Typing `/oh start` would attempt to start a match regardless of whether
everything was actually set up, producing silent broken matches with no useful error.

**After:** The start command checks everything before doing anything. If something is
missing you get a specific message telling you exactly what needs fixing:

```
No arena configured. Use /oh arena set first.
Team 'Blue' has no base set. Use /oh team setbase blue
```

---

### Player Preparation at Match Start

**Before:** When a match started, players were not reliably teleported, their gamemode
wasn't set, and their inventory wasn't cleared.

**After:** When the countdown completes, every team member is teleported to their team's
base, switched to Survival mode, given full health and food, and has their inventory
cleared. Team armour is applied immediately. This happens cleanly for every player every
time.

---

### Match End Cleanup

**Before:** Players were left in whatever state they were in when the match ended.

**After:** When a match ends, all players are switched to Adventure mode, every potion
effect is removed, sneaking is cancelled, inventory is cleared, and health and food are
restored to full. The world border is also reset to its original size.

---

### Oxygen and Air Bubbles

**Before:** Minecraft's vanilla drowning system ran in parallel with the oxygen system,
causing double-damage or unexpected deaths from the wrong source.

**After:** Vanilla drowning damage is fully suppressed for players in a match. The
oxygen system is the only thing that governs your air bar and downed state.

---

### Capture Zone Events

**Before:** When a team captured a zone, the broadcast message and the oxygen restore
notification were never sent.

**After:** Zone capture events are fully wired. When a team captures a zone, the
broadcast goes out to the server, and the oxygen restore message is sent specifically to
the capturing team's members.

---

### Win Conditions

**Before:** The only way a match could end was by the timer running out. Full team
eliminations were ignored.

**After:** The match checks every tick whether only one team still has active players.
If all opposing teams have been fully eliminated, the match ends immediately and that
team wins. The timer is still there as a fallback.

---

### Player Disconnect During Match

**Before:** Disconnecting while downed left orphaned bleedout timers running in memory
for the rest of the match.

**After:** Disconnecting while downed immediately triggers elimination. No ghost timers,
no leaked state.

---

### Friendly Fire Handling

**Before:** Friendly fire was checked at the wrong priority, meaning damage had already
been partially processed before it could be cancelled.

**After:** Friendly fire is cancelled before any combat logic runs. The combat system
ignores events that have already been cancelled.

---

### Boss Bar Display

**Before:** Boss bars used the legacy Bukkit API with plain text and `§` colour codes.

**After:** All boss bars use the modern Adventure API with full MiniMessage formatting
support. The countdown bar, match timer, and per-player downed bars all use configurable
message formats from `messages.yml`.

---

### World Border

**Before:** The border shrink used incorrect time units, meaning the shrink animation
took 20x longer than configured.

**After:** The border uses the correct time units. `shrink-duration-seconds` in
`config.yml` now means exactly what it says.

---

### Configuration

**Before:** All weapons, match settings, and display values were hardcoded. Changing
anything required editing source code and recompiling.

**After:** Weapons have a dedicated `weapons.yml` where every stat - damage, cooldowns,
ammo, reload time, visual model IDs, display name - can be edited without touching code.
Match settings, messages, and sounds are all in their respective config files.

---

### Messages and Sounds

**Before:** All messages were hardcoded English strings scattered throughout the code.

**After:** Every player-facing message, title, subtitle, and sound lives in
`messages.yml`. Messages use MiniMessage formatting. Sounds have configurable volume
and pitch. Kill and down notifications include the weapon used and the attacker's name.

---

### Commands System

**Before:** Commands were registered manually with handwritten tab-completion lists that
frequently went out of sync with what commands actually existed.

**After:** Commands use the Cloud v2 framework, which handles tab-completion, argument
parsing, and error messages automatically.

---

### PlaceholderAPI Support

**Before:** A basic set of placeholders existed but team and zone information was largely
missing. The `%oxygenheist_player_oxygen%` placeholder returned team-average oxygen
rather than the individual player's oxygen level.

**After:** The existing placeholders are correct. A full set of team and weapon
placeholders are available covering oxygen, ammo, reload state, team points, and more.

---

### Weapon System

**Before:** Each weapon was a standalone Bukkit listener class. There was no shared
infrastructure - cooldowns, ammo, reload timers, and frame switching were all
re-implemented from scratch per weapon with no consistency between them. Visual states
were driven by hardcoded Custom Model Data integers scattered throughout the code.

**After:** All nine weapons are built on a shared handler architecture with a common
lifecycle (`onLeftClick`, `onRightClick`, `onMeleeHit`, `onSneakToggle`,
`onProjectileHit`, `tick`, `onSlotChange`). Reload animation, ammo tracking, and frame
switching are handled by base classes so individual weapons only implement what makes
them unique. Visual states are driven by named frame IDs in `weapons.yml` that map to
your custom item plugin of choice.

---

### Custom Item Plugin Support

**Before:** Weapons were tied directly to a specific custom item plugin with no way to
swap providers.

**After:** Weapon item creation and frame switching are abstracted behind a
`WeaponItemProvider` interface. Both ItemsAdder and Nexo are supported out of the box.
The active provider is set in `config.yml` with a single line. Switching between
providers requires no code changes - only config and item definitions. A convention-based
frame ID fallback means most weapons need no explicit frame mappings at all.

---

### Weapon Controls

**Before:** Controls were inconsistent across weapons and some weapons could only be
fired while sneaking.

**After:** All weapons follow a unified control scheme: right-click to shoot, left-click
to reload, sneak to aim. Every weapon supports hip-fire - aiming tightens spread but is
never required to fire. The sneak crouch animation is suppressed server-side so enemies
cannot tell when a player is aiming.

---

### Weapon Damage Pipeline

**Before:** Weapon damage was applied directly to players with no integration into the
downed system. Lethal hits caused vanilla death rather than entering the downed state.

**After:** All weapon damage flows through `CombatListener` into the downed system.
Non-lethal hits record the attacker. Lethal hits redirect to `downPlayer` rather than
killing the player outright. The bypass set pattern prevents melee and projectile hits
from double-firing through the same event.

---

### Downed State

**Before:** Downed players could still be damaged and killed outright by enemies,
bypassing the bleedout and revive system entirely.

**After:** Downed player invulnerability is configurable via
`downed.invulnerable-while-downed` in `config.yml`. When disabled, lethal hits on a
downed player trigger immediate elimination rather than silently doing nothing.

---

### Kill and Down Credit

**Before:** The last attacker stored on a player was never cleared or time-limited.
A hit from the start of the match could still award kill credit for a bleedout that
happened minutes later.

**After:** Kill credit and down credit each have independently configurable time windows
(`downed.kill-credit-window-seconds`, `downed.down-credit-window-seconds`). Credit for
bleedout eliminations is only awarded if the attacker landed a hit within that window.
Direct eliminations always award credit regardless of timing. Down credit handles
environmental downs too - if a player takes fall damage shortly after being shot, the
shooter still gets credited.

Credit is tracked as an `AttackCredit` record containing the attacker's UUID, the weapon
used, and a timestamp. The weapon name is shown in kill and down notification messages.

---
