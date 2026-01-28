# Team System

Complete team management system for OxygenHeist minigame.

## Features

- Team creation with customizable names
- Captain-based permission system
- Player management (add/remove)
- Custom leather armor coloring per team
- Friendly fire toggle
- Automatic armor application on join

## Commands

- `/team create <name>` - Create a new team (3-16 characters)
- `/team delete <name>` - Delete a team (OP only)
- `/team add <player>` - Add player to your team (captain or OP)
- `/team remove <player>` - Remove player from team (captain or OP)
- `/team color <preset|r g b>` - Change team armor color
- `/team captain <player>` - Transfer team leadership
- `/team list` - View all teams
- `/team info [name]` - Show team details
- `/team ff` - Toggle friendly fire

## Color Presets

red, blue, green, yellow, purple, orange, pink, cyan, white, black

## Permissions

- `oxygenheist.team.create` - Create teams
- `oxygenheist.team.delete` - Delete teams
- `oxygenheist.team.add` - Add players
- `oxygenheist.team.remove` - Remove players
- `oxygenheist.team.color` - Change colors
- `oxygenheist.team.captain` - Transfer leadership
- `oxygenheist.team.list` - List teams
- `oxygenheist.team.info` - View info
- `oxygenheist.team.friendlyfire` - Toggle FF

## Configuration

Located in `team-config.yml`:
- Max team size (default: 10)
- Name length limits (min: 3, max: 16)
- Duplicate color allowance
- Default friendly fire setting
- Auto armor assignment
- Custom messages
- Color presets

## Classes

- `Team.java` - Team data model
- `TeamManager.java` - Team operations and listeners
- `TeamCommands.java` - Command handler
- `TeamCommandCompleter.java` - Tab completion
- `TeamConfigHandler.java` - Configuration manager
