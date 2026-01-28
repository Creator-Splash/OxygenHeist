package attila.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import attila.Game.CaptureZone;
import attila.Game.GameState;
import attila.OxygenMain;
import attila.Teams.Team;

public class GameCommandCompleter implements TabCompleter {
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "start", "stop", "end", "setbase", "setzone", "addzone", 
        "delzone", "removezone", "zones", "listzones", "status", 
        "setworld", "setjoinspawn", "teleportjoins"
    );
    
    private static final List<String> COMMON_RADII = Arrays.asList("3", "5", "7", "10", "15", "20");
    
    private final OxygenMain plugin;
    
    public GameCommandCompleter(OxygenMain plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {

            GameState currentState = plugin.getGameManager().getGameState();
            
            return SUBCOMMANDS.stream()
                .filter(sub -> {

                    if (sub.equals("start") && currentState != GameState.WAITING) return false;
                    if ((sub.equals("stop") || sub.equals("end")) && currentState == GameState.WAITING) return false;
                    return sub.toLowerCase().startsWith(args[0].toLowerCase());
                })
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "setbase":

                    List<String> teamSuggestions = new ArrayList<>();

                    plugin.getTeamManager().getAllTeams().values().stream()
                        .filter(t -> !t.hasBase())
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .forEach(teamSuggestions::add);

                    plugin.getTeamManager().getAllTeams().values().stream()
                        .filter(Team::hasBase)
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .filter(name -> !teamSuggestions.contains(name))
                        .forEach(teamSuggestions::add);
                    
                    return teamSuggestions;
                    
                case "delzone":
                case "removezone":
                    return plugin.getGameManager().getAllCaptureZones().values().stream()
                        .map(CaptureZone::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                    
                case "setzone":
                case "addzone":

                    completions.add("<zone_name>");

                    completions.add("spawn");
                    completions.add("center");
                    completions.add("north");
                    completions.add("south");
                    completions.add("east");
                    completions.add("west");
                    completions.add("tower");
                    completions.add("base");
                    return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                    
                case "setworld":

                    return Bukkit.getWorlds().stream()
                        .map(World::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("setzone") || subCommand.equals("addzone")) {

                return COMMON_RADII.stream()
                    .filter(r -> r.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}
