package attila.Managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import attila.OxygenMain;
import attila.Border.BorderManager;
import attila.Config.BorderConfigHandler;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class BlockManager implements Listener {
    
    private final OxygenMain plugin;
    private final BorderManager borderManager;
    private final BorderConfigHandler config;
    private List<Material> whitelistedBlocks;
    
    public BlockManager(OxygenMain plugin, BorderManager borderManager) {
        this.plugin = plugin;
        this.borderManager = borderManager;
        this.config = plugin.getBorderConfig();
        this.whitelistedBlocks = new ArrayList<>();
        loadWhitelist();
    }
    
    private void loadWhitelist() {
        whitelistedBlocks.clear();
        for (String materialName : config.getWhitelistedBlocks()) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                whitelistedBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in whitelist: " + materialName);
            }
        }
    }
    
    public void reloadWhitelist() {
        loadWhitelist();
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (!borderManager.isArenaSet()) {
            return;
        }
        
        if (!isInsideArena(block.getLocation())) {
            return;
        }
        
        if (player.hasPermission("oxygenheist.block.bypass")) {
            return;
        }
        
        if (config.isWhitelistEnabled() && whitelistedBlocks.contains(block.getType())) {
            return;
        }
        
        if (config.isBlockBreakingDisabled()) {
            event.setCancelled(true);
            player.sendMessage(config.getMessage("block-break-denied"));
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (!borderManager.isArenaSet()) {
            return;
        }
        
        if (!isInsideArena(block.getLocation())) {
            return;
        }
        
        if (player.hasPermission("oxygenheist.block.bypass")) {
            return;
        }
        
        if (config.isBlockPlacingDisabled()) {
            event.setCancelled(true);
            player.sendMessage(config.getMessage("block-place-denied"));
        }
    }
    
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!borderManager.isArenaSet()) {
            return;
        }
        
        if (!isInsideArena(event.getBlock().getLocation())) {
            return;
        }
        
        if (config.areExplosionsDisabled()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!borderManager.isArenaSet()) {
            return;
        }
        
        if (!isInsideArena(event.getLocation())) {
            return;
        }
        
        if (config.areExplosionsDisabled()) {
            event.blockList().clear();
        }
    }
    
    public boolean isInsideArena(Location location) {
        if (!borderManager.isArenaSet()) {
            return false;
        }
        
        if (!location.getWorld().equals(borderManager.getArenaWorld())) {
            return false;
        }
        
        Location center = borderManager.getArenaCenter();
        double halfSize = borderManager.getArenaSize() / 2;
        
        double distX = Math.abs(location.getX() - center.getX());
        double distZ = Math.abs(location.getZ() - center.getZ());
        
        return distX <= halfSize && distZ <= halfSize;
    }
    
    public void setBlockBreakingDisabled(boolean disabled) {
        config.setBlockBreakingDisabled(disabled);
        String message = disabled ? config.getMessage("block-breaking-disabled") : config.getMessage("block-breaking-enabled");
        Bukkit.getServer().broadcast(Component.text((message)));
    }
    
    public void setBlockPlacingDisabled(boolean disabled) {
        config.setBlockPlacingDisabled(disabled);
        String message = disabled ? config.getMessage("block-placing-disabled") : config.getMessage("block-placing-enabled");
        Bukkit.getServer().broadcast(Component.text((message)));
    }
    
    public void setExplosionsDisabled(boolean disabled) {
        config.setExplosionsDisabled(disabled);
        String message = disabled ? config.getMessage("explosions-disabled") : config.getMessage("explosions-enabled");
        Bukkit.getServer().broadcast(Component.text((message)));
    }
    
    public void addWhitelistedBlock(Material material) {
        if (!whitelistedBlocks.contains(material)) {
            whitelistedBlocks.add(material);
            config.addWhitelistedBlock(material.name());
        }
    }
    
    public void removeWhitelistedBlock(Material material) {
        whitelistedBlocks.remove(material);
        config.removeWhitelistedBlock(material.name());
    }
    
    public List<Material> getWhitelistedBlocks() {
        return new ArrayList<>(whitelistedBlocks);
    }
    
    public boolean isBlockBreakingDisabled() {
        return config.isBlockBreakingDisabled();
    }
    
    public boolean isBlockPlacingDisabled() {
        return config.isBlockPlacingDisabled();
    }
    
    public boolean areExplosionsDisabled() {
        return config.areExplosionsDisabled();
    }
}