package attila.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import attila.OxygenMain;
import attila.Teams.Team;

public class TeamCommandCompleter implements TabCompleter {
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "create", "delete", "add", "remove", "kick", 
        "color", "captain", "list", "info", "friendlyfire", "ff"
    );
    
    private static final List<String> COLOR_PRESETS = Arrays.asList(
        "red", "blue", "green", "yellow", "purple", 
        "orange", "pink", "cyan", "white", "black"
    );
    
    private static final List<String> TOGGLE_OPTIONS = Arrays.asList("on", "off", "true", "false");
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        OxygenMain plugin = OxygenMain.getInstance();
        
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "create":

                    completions.add("<team_name>");
                    break;
                    
                case "add":

                    if (plugin != null && plugin.getTeamManager() != null) {
                        return Bukkit.getOnlinePlayers().stream()
                            .filter(p -> plugin.getTeamManager().getPlayerTeam(p) == null)
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                    return getOnlinePlayers(args[1]);
                    
                case "remove":
                case "kick":

                    if (plugin != null && plugin.getTeamManager() != null) {
                        return Bukkit.getOnlinePlayers().stream()
                            .filter(p -> plugin.getTeamManager().getPlayerTeam(p) != null)
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                    return getOnlinePlayers(args[1]);
                    
                case "captain":
                case "delete":
                case "info":
                case "color":
                case "friendlyfire":
                case "ff":

                    if (plugin != null && plugin.getTeamManager() != null) {
                        return plugin.getTeamManager().getAllTeams().values().stream()
                            .map(Team::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                    break;
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "create":

                    return COLOR_PRESETS.stream()
                        .filter(color -> color.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                    
                case "captain":

                    if (plugin != null && plugin.getTeamManager() != null) {
                        Team team = plugin.getTeamManager().getTeam(args[1]);
                        if (team != null) {

                            List<String> suggestions = new ArrayList<>();
                            for (java.util.UUID uuid : team.getMembers()) {
                                Player member = Bukkit.getPlayer(uuid);
                                if (member != null && member.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                                    suggestions.add(member.getName());
                                }
                            }

                            Bukkit.getOnlinePlayers().stream()
                                .filter(p -> !team.isMember(p.getUniqueId()))
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                                .forEach(suggestions::add);
                            return suggestions;
                        }
                    }
                    return getOnlinePlayers(args[2]);
                    
                case "color":

                    List<String> colorSuggestions = new ArrayList<>(COLOR_PRESETS);
                    colorSuggestions.add("0"); // Start of RGB
                    return colorSuggestions.stream()
                        .filter(c -> c.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                    
                case "friendlyfire":
                case "ff":

                    return TOGGLE_OPTIONS.stream()
                        .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "create":

                    return getOnlinePlayers(args[3]);
                    
                case "color":

                    if (isNumber(args[2])) {
                        return Arrays.asList("0", "128", "255");
                    }
                    break;
            }
        }
        
        if (args.length == 5 && args[0].equalsIgnoreCase("color")) {

            if (isNumber(args[2]) && isNumber(args[3])) {
                return Arrays.asList("0", "128", "255");
            }
        }
        
        return completions;
    }
    
    private List<String> getOnlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    private boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
