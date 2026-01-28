package attila.Commands;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import attila.Config.GameConfigHandler;
import attila.Game.CaptureZone;
import attila.Game.GameManager;
import attila.Game.GameState;
import attila.OxygenMain;
import attila.Teams.Team;
import attila.Teams.TeamManager;

public class GameCommands implements CommandExecutor {
    
    private final OxygenMain plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final GameConfigHandler config;
    
    public GameCommands(OxygenMain plugin, GameManager gameManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.config = plugin.getGameConfig();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                return handleStart(sender);
            case "stop":
            case "end":
                return handleStop(sender);
            case "setbase":
                return handleSetBase(sender, args);
            case "setzone":
            case "addzone":
                return handleSetZone(sender, args);
            case "delzone":
            case "removezone":
                return handleDelZone(sender, args);
            case "zones":
            case "listzones":
                return handleListZones(sender);
            case "setworld":
            case "setplayground":
                return handleSetWorld(sender, args);
            case "setjoinspawn":
                return handleSetJoinSpawn(sender, args);
            case "teleportjoins":
            case "toggleteleport":
                return handleToggleTeleport(sender, args);
            case "status":
                return handleStatus(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission(config.getPermission("game-start"))) {
            sender.sendMessage("§c§lNice try! §fYou don't have permission to start games.");
            return true;
        }
        
        if (!gameManager.canStartGame()) {
            String error = gameManager.getStartError();
            sender.sendMessage(error);
            return true;
        }
        
        gameManager.startCountdown();
        sender.sendMessage("§a§lGame countdown started! §7Get ready for chaos.");
        
        return true;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission(config.getPermission("game-stop"))) {
            sender.sendMessage("§c§lNice try! §fYou don't have permission to stop games.");
            return true;
        }
        
        if (gameManager.getGameState() == GameState.WAITING) {
            sender.sendMessage(config.getMessage("no-game-running"));
            return true;
        }
        
        gameManager.endGame();
        sender.sendMessage("§c§lGame forcefully ended! §7Someone couldn't handle the pressure.");
        
        return true;
    }
    
    private boolean handleSetBase(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("game-setbase"))) {
            sender.sendMessage("§c§lNice try! §fYou don't have permission to set bases.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can set base locations.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /game setbase <team>");
            return true;
        }
        
        Player player = (Player) sender;
        String teamName = args[1];
        Team team = teamManager.getTeam(teamName);
        
        if (team == null) {
            sender.sendMessage("§c§lTeam not found! §fDid you spell it wrong? Wouldn't surprise me.");
            return true;
        }
        
        Location loc = player.getLocation();
        team.setBaseLocation(loc);

        team.saveToConfig(plugin);
        
        sender.sendMessage(config.getMessage("base-set"));
        sender.sendMessage("§fTeam: §e" + team.getName());
        sender.sendMessage("§fLocation: §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        
        return true;
    }
    
    private boolean handleSetZone(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("game-setzone"))) {
            sender.sendMessage("§c§lNice try! §fYou don't have permission to create zones.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can create capture zones.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /game setzone <name> [radius]");
            return true;
        }
        
        Player player = (Player) sender;
        String zoneName = args[1];
        
        double radius = config.getCaptureRadius();
        if (args.length >= 3) {
            try {
                radius = Double.parseDouble(args[2]);
                if (radius < 1 || radius > 50) {
                    sender.sendMessage("§c§lInvalid radius! §fMust be between 1 and 50. Not rocket science.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c§lInvalid number! §fDid you skip math class?");
                return true;
            }
        }
        
        if (gameManager.getCaptureZone(zoneName) != null) {
            sender.sendMessage("§c§lZone already exists! §fBe more creative with names.");
            return true;
        }
        
        Location loc = player.getLocation();
        gameManager.addCaptureZone(zoneName, loc, radius);
        
        String msg = config.getMessage("zone-created").replace("%radius%", String.valueOf((int) radius));
        sender.sendMessage(msg);
        sender.sendMessage("§fZone: §e" + zoneName);
        sender.sendMessage("§fCenter: §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        
        return true;
    }
    
    private boolean handleDelZone(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("game-delzone"))) {
            sender.sendMessage("§c§lNice try! §fYou don't have permission to delete zones.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /game delzone <name>");
            return true;
        }
        
        String zoneName = args[1];
        
        if (gameManager.removeCaptureZone(zoneName)) {
            sender.sendMessage(config.getMessage("zone-deleted"));
        } else {
            sender.sendMessage("§c§lZone not found! §fMaybe it never existed?");
        }
        
        return true;
    }
    
    private boolean handleListZones(CommandSender sender) {
        Map<String, CaptureZone> zones = gameManager.getAllCaptureZones();
        
        if (zones.isEmpty()) {
            sender.sendMessage("§e§lNo capture zones exist! §fCreate some with /game setzone.");
            return true;
        }
        
        sender.sendMessage("§6§l===== CAPTURE ZONES =====");
        for (CaptureZone zone : zones.values()) {
            String owner = zone.hasOwner() ? zone.getOwnerTeam().getName() : "None";
            sender.sendMessage("§e" + zone.getName() + " §7- §fRadius: §a" + (int) zone.getRadius() + 
                " §7| §fOwner: §a" + owner);
        }
        sender.sendMessage("§6§l=========================");
        
        return true;
    }
    
    private boolean handleSetWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("game-setworld"))) {
            sender.sendMessage("§c§lNice try! §fYou don't have permission to set playground world.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can set the playground world.");
            return true;
        }
        
        Player player = (Player) sender;
        String worldName = player.getWorld().getName();
        
        config.setPlaygroundWorld(worldName);
        
        String msg = config.getMessage("playground-set").replace("%world%", worldName);
        sender.sendMessage(msg);
        
        return true;
    }
    
    private boolean handleSetJoinSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("game-setjoinspawn"))) {
            sender.sendMessage("§c§lNice try! §fYou don't have permission to set join spawn.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getLocation();
        
        config.setJoinSpawnLocation(location);
        config.saveConfig();
        
        sender.sendMessage("§a§lJoin spawn location set! §7Players will spawn at:");
        sender.sendMessage("§fWorld: §e" + location.getWorld().getName());
        sender.sendMessage("§fX: §e" + location.getBlockX());
        sender.sendMessage("§fY: §e" + location.getBlockY());
        sender.sendMessage("§fZ: §e" + location.getBlockZ());
        
        return true;
    }
    
    private boolean handleToggleTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission(config.getPermission("game-toggleteleport"))) {
            sender.sendMessage("§c§lNice try! §fYou don't have permission to toggle teleport.");
            return true;
        }
        
        boolean currentState = config.isTeleportOnJoinEnabled();
        boolean newState = !currentState;
        
        config.set("game.teleport-on-join", newState);
        config.saveConfig();
        
        String status = newState ? "§aENABLED" : "§cDISABLED";
        sender.sendMessage("§a§lTeleport on join: " + status);
        
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage("§6§l===== GAME STATUS =====");
        sender.sendMessage("§fState: §e" + gameManager.getGameState().name());
        
        String playgroundWorld = config.getPlaygroundWorld();
        String worldStatus = (playgroundWorld == null || playgroundWorld.isEmpty()) ? "§cNot set" : "§a" + playgroundWorld;
        sender.sendMessage("§fPlayground World: " + worldStatus);
        
        Map<String, Team> teams = teamManager.getAllTeams();
        sender.sendMessage("§fTeams: §e" + teams.size());
        
        for (Team team : teams.values()) {
            String hasBase = team.hasBase() ? "§aYes" : "§cNo";
            sender.sendMessage("  §7- §e" + team.getName() + " §7| Base: " + hasBase + 
                " §7| Points: §e" + team.getPoints());
        }
        
        Map<String, CaptureZone> zones = gameManager.getAllCaptureZones();
        sender.sendMessage("§fCapture Zones: §e" + zones.size());
        
        boolean borderSet = plugin.getBorderManager().isArenaSet();
        sender.sendMessage("§fBorder Set: " + (borderSet ? "§aYes" : "§cNo"));
        
        boolean canStart = gameManager.canStartGame();
        sender.sendMessage("§fReady to Start: " + (canStart ? "§aYes" : "§cNo"));
        
        sender.sendMessage("§6§l=======================");
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== GAME COMMANDS =====");
        sender.sendMessage("§e/game start §7- Start the game");
        sender.sendMessage("§e/game stop §7- End the game");
        sender.sendMessage("§e/game setworld §7- Set the playground world");
        sender.sendMessage("§e/game setbase <team> §7- Set team spawn location");
        sender.sendMessage("§e/game setzone <name> [radius] §7- Create capture zone");
        sender.sendMessage("§e/game delzone <name> §7- Delete capture zone");
        sender.sendMessage("§e/game zones §7- List all capture zones");
        sender.sendMessage("§e/game setjoinspawn §7- Set join spawn location");
        sender.sendMessage("§e/game teleportjoins §7- Toggle teleport on join");
        sender.sendMessage("§e/game status §7- Show game status");
        sender.sendMessage("§6§l=========================");
    }
}
