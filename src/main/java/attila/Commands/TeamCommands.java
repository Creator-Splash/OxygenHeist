package attila.Commands;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import attila.Config.TeamConfigHandler;
import attila.OxygenMain;
import attila.Teams.Team;
import attila.Teams.TeamManager;

public class TeamCommands implements CommandExecutor {
    
    private final OxygenMain plugin;
    private final TeamManager teamManager;
    private final TeamConfigHandler config;
    
    public TeamCommands(OxygenMain plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.config = plugin.getTeamConfig();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "add":
                return handleAdd(sender, args);
            case "remove":
            case "kick":
                return handleRemove(sender, args);
            case "color":
                return handleColor(sender, args);
            case "captain":
                return handleCaptain(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            case "friendlyfire":
            case "ff":
                return handleFriendlyFire(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("create"))) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /team create <name> <color> [captain]");
            sender.sendMessage("§fColors: red, blue, green, yellow, purple, orange, pink, cyan, white, black");
            return true;
        }
        
        String teamName = args[1];
        
        if (teamName.length() < config.getMinNameLength() || teamName.length() > config.getMaxNameLength()) {
            sender.sendMessage("§c§lInvalid name! §fTeam names must be between " + 
                config.getMinNameLength() + " and " + config.getMaxNameLength() + " characters.");
            return true;
        }

        String colorName = args[2];
        String presetColor = config.getPresetColor(colorName);
        if (presetColor == null) {
            sender.sendMessage("§c§lInvalid color! §fChoose from: red, blue, green, yellow, purple, orange, pink, cyan, white, black");
            return true;
        }
        
        String[] rgb = presetColor.split(",");
        Color color = Color.fromRGB(
            Integer.parseInt(rgb[0]),
            Integer.parseInt(rgb[1]),
            Integer.parseInt(rgb[2])
        );
        
        Player captain = null;
        if (args.length >= 4) {
            captain = Bukkit.getPlayer(args[3]);
            if (captain == null) {
                sender.sendMessage("§c§lCaptain not found! §fAre they even online? How disappointing.");
                return true;
            }
            
            if (teamManager.getPlayerTeam(captain) != null) {
                sender.sendMessage("§c§lThat player is already in a team! §fCan't have split loyalties here.");
                return true;
            }
        }
        
        if (teamManager.createTeam(teamName, captain)) {
            Team team = teamManager.getTeam(teamName);
            team.setArmorColor(color);
            
            sender.sendMessage(config.getMessage("team-created"));
            sender.sendMessage("§fTeam: §e" + teamName);
            sender.sendMessage("§fColor: §e" + colorName);
            if (captain != null) {
                sender.sendMessage("§fCaptain: §e" + captain.getName());
                captain.sendMessage("§a§lYou've been made captain of team §e" + teamName + "§a§l! Don't screw it up.");
            } else {
                sender.sendMessage("§fCaptain: §7None (set one with /team captain)");
            }
        } else {
            sender.sendMessage(config.getMessage("team-exists"));
        }
        
        return true;
    }
    
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("delete"))) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /team delete <name>");
            return true;
        }
        
        String teamName = args[1];
        
        if (teamManager.deleteTeam(teamName)) {
            sender.sendMessage(config.getMessage("team-deleted"));
        } else {
            sender.sendMessage(config.getMessage("team-not-found"));
        }
        
        return true;
    }
    
    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        
        Player captain = (Player) sender;
        Team team = teamManager.getPlayerTeam(captain);
        
        if (team == null) {
            sender.sendMessage(config.getMessage("not-in-team"));
            return true;
        }
        
        if (!team.isCaptain(captain.getUniqueId())) {
            if (!captain.hasPermission(config.getPermission("add"))) {
                sender.sendMessage(config.getMessage("not-captain"));
                return true;
            }
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /team add <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c§lPlayer not found! §fAre they even online?");
            return true;
        }
        
        if (team.getSize() >= config.getMaxTeamSize()) {
            sender.sendMessage(config.getMessage("team-full"));
            return true;
        }
        
        if (teamManager.addPlayerToTeam(target, team)) {
            sender.sendMessage(config.getMessage("player-added"));
            sender.sendMessage("§fPlayer: §e" + target.getName());
            target.sendMessage("§a§lYou've been added to team §e" + team.getName() + "§a§l!");
        } else {
            sender.sendMessage(config.getMessage("already-in-team"));
        }
        
        return true;
    }
    
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        
        Player captain = (Player) sender;
        Team team = teamManager.getPlayerTeam(captain);
        
        if (team == null) {
            sender.sendMessage(config.getMessage("not-in-team"));
            return true;
        }
        
        if (!team.isCaptain(captain.getUniqueId())) {
            if (!captain.hasPermission(config.getPermission("remove"))) {
                sender.sendMessage(config.getMessage("not-captain"));
                return true;
            }
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /team remove <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c§lPlayer not found! §fDid they rage quit?");
            return true;
        }
        
        if (teamManager.removePlayerFromTeam(target, team)) {
            sender.sendMessage(config.getMessage("player-removed"));
            sender.sendMessage("§fPlayer: §e" + target.getName());
            target.sendMessage("§c§lYou've been kicked from team §e" + team.getName() + "§c§l!");
        } else {
            sender.sendMessage(config.getMessage("cannot-remove-captain"));
        }
        
        return true;
    }
    
    private boolean handleColor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        
        Player captain = (Player) sender;
        Team team = teamManager.getPlayerTeam(captain);
        
        if (team == null) {
            sender.sendMessage(config.getMessage("not-in-team"));
            return true;
        }
        
        if (!team.isCaptain(captain.getUniqueId())) {
            if (!captain.hasPermission(config.getPermission("color"))) {
                sender.sendMessage(config.getMessage("not-captain"));
                return true;
            }
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /team color <preset|rgb>");
            sender.sendMessage("§fPresets: red, blue, green, yellow, purple, orange, pink, cyan, white, black");
            sender.sendMessage("§fRGB: /team color <r> <g> <b>");
            return true;
        }
        
        Color color = null;
        
        if (args.length == 2) {
            String presetColor = config.getPresetColor(args[1]);
            if (presetColor != null) {
                String[] rgb = presetColor.split(",");
                int r = Integer.parseInt(rgb[0]);
                int g = Integer.parseInt(rgb[1]);
                int b = Integer.parseInt(rgb[2]);
                color = Color.fromRGB(r, g, b);
            }
        } else if (args.length == 4) {
            try {
                int r = Integer.parseInt(args[1]);
                int g = Integer.parseInt(args[2]);
                int b = Integer.parseInt(args[3]);
                
                if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                    sender.sendMessage("§c§lInvalid RGB! §fValues must be between 0 and 255.");
                    return true;
                }
                
                color = Color.fromRGB(r, g, b);
            } catch (NumberFormatException e) {
                sender.sendMessage(config.getMessage("invalid-color"));
                return true;
            }
        }
        
        if (color == null) {
            sender.sendMessage(config.getMessage("invalid-color"));
            return true;
        }
        
        teamManager.setTeamColor(team, color);
        sender.sendMessage(config.getMessage("color-changed"));
        
        return true;
    }
    
    private boolean handleCaptain(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("captain"))) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /team captain <team> <player>");
            return true;
        }
        
        String teamName = args[1];
        Team team = teamManager.getTeam(teamName);
        
        if (team == null) {
            sender.sendMessage(config.getMessage("team-not-found"));
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!team.hasCaptain() || team.isCaptain(player.getUniqueId()) || player.hasPermission(config.getPermission("captain"))) {

            } else {
                sender.sendMessage(config.getMessage("not-captain"));
                return true;
            }
        }
        
        Player newCaptain = Bukkit.getPlayer(args[2]);
        if (newCaptain == null) {
            sender.sendMessage("§c§lPlayer not found! §fMaybe they abandoned ship?");
            return true;
        }
        
        if (teamManager.setCaptain(team, newCaptain)) {
            sender.sendMessage(config.getMessage("captain-changed"));
            sender.sendMessage("§fNew captain: §e" + newCaptain.getName());
            newCaptain.sendMessage("§a§lYou're now the captain of §e" + team.getName() + "§a§l! Don't screw it up.");
        } else {
            sender.sendMessage("§c§lFailed to set captain!");
        }
        
        return true;
    }
    
    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission(config.getPermission("list"))) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }
        
        Map<String, Team> teams = teamManager.getAllTeams();
        
        if (teams.isEmpty()) {
            sender.sendMessage("§e§lNo teams exist! §fHow sad. Create one?");
            return true;
        }
        
        sender.sendMessage("§6§l===== TEAMS =====");
        for (Team team : teams.values()) {
            UUID captainUUID = team.getCaptainUUID();
            String captainName = "None";
            if (captainUUID != null) {
                Player captain = Bukkit.getPlayer(captainUUID);
                captainName = captain != null ? captain.getName() : "Offline";
            }
            sender.sendMessage("§e" + team.getName() + " §7- §fCaptain: §a" + captainName + 
                " §7| §fMembers: §a" + team.getSize());
        }
        sender.sendMessage("§6§l==================");
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("info"))) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }
        
        String teamName;
        Team team;
        
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUsage: /team info <name>");
                return true;
            }
            team = teamManager.getPlayerTeam((Player) sender);
            if (team == null) {
                sender.sendMessage(config.getMessage("not-in-team"));
                return true;
            }
        } else {
            teamName = args[1];
            team = teamManager.getTeam(teamName);
            if (team == null) {
                sender.sendMessage(config.getMessage("team-not-found"));
                return true;
            }
        }
        
        UUID captainUUID = team.getCaptainUUID();
        String captainName = "None";
        if (captainUUID != null) {
            Player captain = Bukkit.getPlayer(captainUUID);
            captainName = captain != null ? captain.getName() : "Offline";
        }
        
        sender.sendMessage("§6§l===== " + team.getName().toUpperCase() + " =====");
        sender.sendMessage("§fCaptain: §e" + captainName);
        sender.sendMessage("§fMembers: §e" + team.getSize() + "/" + config.getMaxTeamSize());
        sender.sendMessage("§fFriendly Fire: " + (team.isFriendlyFireEnabled() ? "§aEnabled" : "§cDisabled"));
        sender.sendMessage("§fColor: §7RGB(" + team.getArmorColor().getRed() + ", " + 
            team.getArmorColor().getGreen() + ", " + team.getArmorColor().getBlue() + ")");
        
        StringBuilder members = new StringBuilder("§fMembers: §7");
        java.util.List<UUID> memberUUIDs = team.getMembers();
        for (int i = 0; i < memberUUIDs.size(); i++) {
            UUID memberUUID = memberUUIDs.get(i);
            if (memberUUID != null) {
                Player member = Bukkit.getPlayer(memberUUID);
                String memberName = member != null ? member.getName() : "Offline";
                members.append(memberName);
                if (i < memberUUIDs.size() - 1) {
                    members.append(", ");
                }
            }
        }
        sender.sendMessage(members.toString());
        sender.sendMessage("§6§l" + "=".repeat(team.getName().length() + 12));
        
        return true;
    }
    
    private boolean handleFriendlyFire(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        
        Player captain = (Player) sender;
        Team team = teamManager.getPlayerTeam(captain);
        
        if (team == null) {
            sender.sendMessage(config.getMessage("not-in-team"));
            return true;
        }
        
        if (!team.isCaptain(captain.getUniqueId())) {
            if (!captain.hasPermission(config.getPermission("friendlyfire"))) {
                sender.sendMessage(config.getMessage("not-captain"));
                return true;
            }
        }
        
        boolean newValue = !team.isFriendlyFireEnabled();
        team.setFriendlyFire(newValue);

        team.saveToConfig(plugin);
        
        if (newValue) {
            sender.sendMessage(config.getMessage("friendlyfire-enabled"));
        } else {
            sender.sendMessage(config.getMessage("friendlyfire-disabled"));
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== TEAM COMMANDS =====");
        sender.sendMessage("§e/team create <name> <captain> §7- Create a team (OP)");
        sender.sendMessage("§e/team delete <name> §7- Delete a team (OP)");
        sender.sendMessage("§e/team add <player> §7- Add a player (Captain)");
        sender.sendMessage("§e/team remove <player> §7- Remove a player (Captain)");
        sender.sendMessage("§e/team color <preset|r g b> §7- Change armor color (Captain)");
        sender.sendMessage("§e/team captain <player> §7- Transfer leadership (Captain)");
        sender.sendMessage("§e/team list §7- List all teams");
        sender.sendMessage("§e/team info [name] §7- Team information");
        sender.sendMessage("§e/team ff §7- Toggle friendly fire (Captain)");
        sender.sendMessage("§6§l=========================");
    }
}
