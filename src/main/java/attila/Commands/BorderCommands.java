package attila.Commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import attila.OxygenMain;
import attila.Border.BorderManager;
import attila.Border.BorderTimer;
import attila.Managers.BlockManager;

import java.util.Arrays;

public class BorderCommands implements CommandExecutor {
    
    private final OxygenMain plugin;
    private final BorderManager borderManager;
    private final BorderTimer borderTimer;
    private final BlockManager blockManager;
    
    public BorderCommands(OxygenMain plugin, BorderManager borderManager, BorderTimer borderTimer) {
        this.plugin = plugin;
        this.borderManager = borderManager;
        this.borderTimer = borderTimer;
        this.blockManager = plugin.getBlockManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lThis command can only be executed by players! §7Nice try though.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "wand":
                return handleWand(player);
            case "set":
                return handleSet(player);
            case "setup":
                return handleSetup(player);
            case "start":
                return handleStart(player, args);
            case "stop":
                return handleStop(player);
            case "pause":
                return handlePause(player);
            case "resume":
                return handleResume(player);
            case "reset":
                return handleReset(player);
            case "info":
                return handleInfo(player);
            case "clear":
                return handleClear(player);
            case "blocks":
                return handleBlocks(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }
    
    private boolean handleWand(Player player) {
        if (!player.hasPermission("oxygenheist.border.wand")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        String materialName = plugin.getBorderConfig().getWandMaterial();
        Material wandMaterial;
        try {
            wandMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            wandMaterial = Material.STICK;
        }
        
        ItemStack wand = new ItemStack(wandMaterial);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(plugin.getBorderConfig().getWandName().replace("&", "§"));
        meta.setLore(Arrays.asList(
            "§7Left Click: §ePoint 1",
            "§7Right Click: §ePoint 2"
        ));
        wand.setItemMeta(meta);
        
        player.getInventory().addItem(wand);
        player.sendMessage("§a§lSelection wand obtained! §7Go wild.");
        return true;
    }
    
    private boolean handleSet(Player player) {
        if (!player.hasPermission("oxygenheist.border.set")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        borderManager.confirmArena(player);
        return true;
    }
    
    private boolean handleSetup(Player player) {
        if (!player.hasPermission("oxygenheist.border.setup")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        if (!borderManager.isArenaSet()) {
            player.sendMessage("§c§lError: §fYou must set an area first! §7Duh.");
            return true;
        }
        
        borderManager.setupBorder();
        player.sendMessage(plugin.getBorderConfig().getMessage("border-setup"));
        return true;
    }
    
    private boolean handleStart(Player player, String[] args) {
        if (!player.hasPermission("oxygenheist.border.start")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        if (!borderManager.isArenaSet()) {
            player.sendMessage("§c§lError: §fYou must set an area first! §7Come on.");
            return true;
        }
        
        if (args.length < 4) {
            player.sendMessage("§c§lUsage: §f/border start <delay> <final_size> <duration>");
            return true;
        }
        
        try {
            int delay = Integer.parseInt(args[1]);
            double finalSize = Double.parseDouble(args[2]);
            long duration = Long.parseLong(args[3]);
            
            borderTimer.startTimer(delay, finalSize, duration);
            player.sendMessage("§a§lTimer started! §7Let the chaos begin!");
            player.sendMessage("§fDelay: §e" + delay + "s");
            player.sendMessage("§fFinal size: §e" + finalSize);
            player.sendMessage("§fDuration: §e" + duration + "s");
        } catch (NumberFormatException e) {
            player.sendMessage("§c§lError: §fInvalid numeric values! §7Math is hard, I know.");
        }
        
        return true;
    }
    
    private boolean handleStop(Player player) {
        if (!player.hasPermission("oxygenheist.border.stop")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        borderTimer.cancelTimer();
        plugin.getBorderShrink().stopShrinking();
        player.sendMessage("§a§lTimer and shrinking stopped! §7Party's over.");
        return true;
    }
    
    private boolean handlePause(Player player) {
        if (!player.hasPermission("oxygenheist.border.pause")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        borderTimer.pauseTimer();
        plugin.getBorderShrink().pauseShrinking();
        player.sendMessage("§e§lTimer paused! §7Taking a break?");
        return true;
    }
    
    private boolean handleResume(Player player) {
        if (!player.hasPermission("oxygenheist.border.resume")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        borderTimer.resumeTimer();
        player.sendMessage("§a§lTimer resumed! §7Back to business.");
        return true;
    }
    
    private boolean handleReset(Player player) {
        if (!player.hasPermission("oxygenheist.border.reset")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        borderManager.resetBorder();
        borderTimer.cancelTimer();
        plugin.getBorderShrink().stopShrinking();
        player.sendMessage("§a§lBorder reset! §7Like nothing ever happened.");
        return true;
    }
    
    private boolean handleInfo(Player player) {
        if (!player.hasPermission("oxygenheist.border.info")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        if (!borderManager.isArenaSet()) {
            player.sendMessage("§e§lNo arena established! §7Set one first, genius.");
            return true;
        }
        
        player.sendMessage("§6§l===== BORDER INFO =====");
        player.sendMessage("§fCenter: §e" + borderManager.getArenaCenter().getBlockX() + ", " +
            borderManager.getArenaCenter().getBlockZ());
        player.sendMessage("§fCurrent size: §e" + (int)plugin.getBorderShrink().getCurrentBorderSize());
        player.sendMessage("§fShrinking: §e" + (plugin.getBorderShrink().isShrinking() ? "Yes" : "No"));
        player.sendMessage("§fTimer active: §e" + (borderTimer.isRunning() ? "Yes (" + borderTimer.getRemainingSeconds() + "s)" : "No"));
        player.sendMessage("§fBlock breaking: §e" + (blockManager.isBlockBreakingDisabled() ? "Disabled" : "Enabled"));
        player.sendMessage("§fBlock placing: §e" + (blockManager.isBlockPlacingDisabled() ? "Disabled" : "Enabled"));
        player.sendMessage("§fExplosions: §e" + (blockManager.areExplosionsDisabled() ? "Disabled" : "Enabled"));
        return true;
    }
    
    private boolean handleClear(Player player) {
        if (!player.hasPermission("oxygenheist.border.clear")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        borderManager.clearSelection(player);
        player.sendMessage("§a§lSelection cleared! §7Starting fresh.");
        return true;
    }
    
    private boolean handleBlocks(Player player, String[] args) {
        if (!player.hasPermission("oxygenheist.block.toggle")) {
            player.sendMessage(plugin.getBorderConfig().getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§6§l===== BLOCK COMMANDS =====");
            player.sendMessage("§e/border blocks breaking <on|off>");
            player.sendMessage("§e/border blocks placing <on|off>");
            player.sendMessage("§e/border blocks explosions <on|off>");
            player.sendMessage("§e/border blocks whitelist <add|remove|list> [material]");
            return true;
        }
        
        switch (args[1].toLowerCase()) {
            case "breaking":
                if (args.length < 3) {
                    player.sendMessage("§c§lUsage: §f/border blocks breaking <on|off>");
                    return true;
                }
                boolean breakingOff = args[2].equalsIgnoreCase("off");
                blockManager.setBlockBreakingDisabled(breakingOff);
                return true;
                
            case "placing":
                if (args.length < 3) {
                    player.sendMessage("§c§lUsage: §f/border blocks placing <on|off>");
                    return true;
                }
                boolean placingOff = args[2].equalsIgnoreCase("off");
                blockManager.setBlockPlacingDisabled(placingOff);
                return true;
                
            case "explosions":
                if (args.length < 3) {
                    player.sendMessage("§c§lUsage: §f/border blocks explosions <on|off>");
                    return true;
                }
                boolean explosionsOff = args[2].equalsIgnoreCase("off");
                blockManager.setExplosionsDisabled(explosionsOff);
                return true;
                
            case "whitelist":
                return handleWhitelist(player, args);
                
            default:
                player.sendMessage("§c§lUnknown subcommand! §7Try 'breaking', 'placing', 'explosions', or 'whitelist'.");
                return true;
        }
    }
    
    private boolean handleWhitelist(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c§lUsage: §f/border blocks whitelist <add|remove|list> [material]");
            return true;
        }
        
        switch (args[2].toLowerCase()) {
            case "add":
                if (args.length < 4) {
                    player.sendMessage("§c§lUsage: §f/border blocks whitelist add <material>");
                    return true;
                }
                try {
                    Material material = Material.valueOf(args[3].toUpperCase());
                    blockManager.addWhitelistedBlock(material);
                    player.sendMessage(plugin.getBorderConfig().getMessage("whitelist-added"));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(plugin.getBorderConfig().getMessage("invalid-material"));
                }
                return true;
                
            case "remove":
                if (args.length < 4) {
                    player.sendMessage("§c§lUsage: §f/border blocks whitelist remove <material>");
                    return true;
                }
                try {
                    Material material = Material.valueOf(args[3].toUpperCase());
                    blockManager.removeWhitelistedBlock(material);
                    player.sendMessage(plugin.getBorderConfig().getMessage("whitelist-removed"));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(plugin.getBorderConfig().getMessage("invalid-material"));
                }
                return true;
                
            case "list":
                player.sendMessage("§6§l===== WHITELISTED BLOCKS =====");
                for (Material mat : blockManager.getWhitelistedBlocks()) {
                    player.sendMessage("§e- §f" + mat.name());
                }
                return true;
                
            default:
                player.sendMessage("§c§lUnknown whitelist action! §7Use 'add', 'remove', or 'list'.");
                return true;
        }
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6§l===== BORDER COMMANDS =====");
        player.sendMessage("§e/border wand §7- Get selection wand");
        player.sendMessage("§e/border set §7- Establish arena");
        player.sendMessage("§e/border setup §7- Configure border");
        player.sendMessage("§e/border start <delay> <size> <dur> §7- Start shrinking");
        player.sendMessage("§e/border stop §7- Stop everything");
        player.sendMessage("§e/border pause §7- Pause timer");
        player.sendMessage("§e/border resume §7- Resume timer");
        player.sendMessage("§e/border reset §7- Reset border");
        player.sendMessage("§e/border info §7- Border info");
        player.sendMessage("§e/border clear §7- Clear selection");
        player.sendMessage("§e/border blocks §7- Manage block rules");
    }
}