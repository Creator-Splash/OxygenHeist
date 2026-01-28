package attila.Commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BorderCommandCompleter implements TabCompleter {

    private static final List<String> MAIN_COMMANDS = Arrays.asList(
        "wand", "set", "setup", "start", "stop", "pause", "resume", "reset", "info", "clear", "blocks"
    );

    private static final List<String> BLOCK_SUBCOMMANDS = Arrays.asList(
        "breaking", "placing", "explosions", "whitelist"
    );

    private static final List<String> TOGGLE_OPTIONS = Arrays.asList("on", "off", "true", "false", "enable", "disable");

    private static final List<String> WHITELIST_ACTIONS = Arrays.asList("add", "remove", "list", "clear");
    
    private static final List<String> COMMON_SHRINK_TIMES = Arrays.asList("30", "60", "120", "180", "300", "600");
    
    private static final List<String> COMMON_MATERIALS = Arrays.asList(
        "CHEST", "ENDER_CHEST", "CRAFTING_TABLE", "FURNACE", "ANVIL",
        "BREWING_STAND", "ENCHANTING_TABLE", "BARREL", "SHULKER_BOX",
        "TNT", "OBSIDIAN", "RESPAWN_ANCHOR", "BED"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {

            return MAIN_COMMANDS.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        String mainCommand = args[0].toLowerCase();
        
        if (args.length == 2) {
            switch (mainCommand) {
                case "start":

                    return COMMON_SHRINK_TIMES.stream()
                        .filter(t -> t.startsWith(args[1]))
                        .collect(Collectors.toList());
                    
                case "blocks":
                    return BLOCK_SUBCOMMANDS.stream()
                        .filter(sub -> sub.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length >= 2 && "blocks".equalsIgnoreCase(args[0])) {
            if (args.length == 3) {
                String subCmd = args[1].toLowerCase();
                if ("breaking".equals(subCmd) || "placing".equals(subCmd) || "explosions".equals(subCmd)) {
                    return TOGGLE_OPTIONS.stream()
                        .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                } else if ("whitelist".equals(subCmd)) {
                    return WHITELIST_ACTIONS.stream()
                        .filter(act -> act.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
            } else if (args.length == 4 && "whitelist".equalsIgnoreCase(args[1])) {
                String action = args[2].toLowerCase();
                if ("add".equals(action)) {

                    List<String> suggestions = new ArrayList<>();

                    COMMON_MATERIALS.stream()
                        .filter(mat -> mat.toLowerCase().startsWith(args[3].toLowerCase()))
                        .forEach(suggestions::add);

                    Arrays.stream(Material.values())
                        .filter(Material::isBlock)
                        .map(Material::name)
                        .filter(mat -> mat.toLowerCase().startsWith(args[3].toLowerCase()))
                        .filter(mat -> !suggestions.contains(mat))
                        .limit(20) // Limit to prevent overwhelming suggestions
                        .forEach(suggestions::add);
                    
                    return suggestions;
                } else if ("remove".equals(action)) {


                    return Arrays.stream(Material.values())
                        .filter(Material::isBlock)
                        .map(Material::name)
                        .filter(mat -> mat.toLowerCase().startsWith(args[3].toLowerCase()))
                        .limit(20)
                        .collect(Collectors.toList());
                }
            }
        }

        return completions;
    }
}