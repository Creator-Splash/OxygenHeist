package attila.Border;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import attila.OxygenMain;

public class BorderManager implements Listener {
    
    private final OxygenMain plugin;
    private final Map<String, Location> firstPoints;
    private final Map<String, Location> secondPoints;
    private Location arenaCenter;
    private double arenaSize;
    private World arenaWorld;
    private boolean arenaSet;
    
    public BorderManager(OxygenMain plugin) {
        this.plugin = plugin;
        this.firstPoints = new HashMap<>();
        this.secondPoints = new HashMap<>();
        this.arenaSet = false;

        loadArenaFromConfig();
    }
    
    /**
     * Loads arena configuration from config file.
     */
    private void loadArenaFromConfig() {

        if (arenaSet) {
            return;
        }
        
        if (!plugin.getBorderConfig().hasArenaData()) {
            plugin.getLogger().info("No saved arena configuration found.");
            return;
        }
        
        String worldName = plugin.getBorderConfig().getArenaWorld();
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        
        if (world == null) {
            plugin.getLogger().warning("Could not load arena - world '" + worldName + "' not found!");
            return;
        }
        
        double x = plugin.getBorderConfig().getArenaCenterX();
        double y = plugin.getBorderConfig().getArenaCenterY();
        double z = plugin.getBorderConfig().getArenaCenterZ();
        double size = plugin.getBorderConfig().getArenaSize();
        
        arenaCenter = new Location(world, x, y, z);
        arenaWorld = world;
        arenaSize = size;
        arenaSet = true;

        initialCenter = arenaCenter.clone();
        initialSize = arenaSize;
        initialWorld = arenaWorld;

        WorldBorder border = arenaWorld.getWorldBorder();
        border.setCenter(arenaCenter);
        border.setSize(arenaSize);
        border.setWarningDistance(plugin.getBorderConfig().getWarningDistance());
        border.setDamageAmount(plugin.getBorderConfig().getDamageAmount());
        border.setDamageBuffer(plugin.getBorderConfig().getDamageBuffer());
        
        plugin.getLogger().info("Loaded arena configuration: center=(" + (int)x + ", " + (int)y + ", " + (int)z + "), size=" + (int)size);
        plugin.getLogger().info("WorldBorder applied successfully!");
    }
    
    private Location initialCenter;
    private double initialSize;
    private World initialWorld;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() != getWandMaterial()) {
            return;
        }
        
        if (!player.hasPermission("oxygenheist.border.wand")) {
            return;
        }
        
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            org.bukkit.block.Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;
            Location loc = clickedBlock.getLocation();
            firstPoints.put(player.getName(), loc);
            player.sendMessage("§a§lFirst point established: §f" + 
                loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            org.bukkit.block.Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;
            Location loc = clickedBlock.getLocation();
            secondPoints.put(player.getName(), loc);
            player.sendMessage("§a§lSegundo punto establecido: §f" + 
                loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        if (!arenaSet || plugin.getBorderConfig().canTraverseBorder()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to == null || from == null || !to.getWorld().equals(arenaWorld)) {
            return;
        }

        if (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ()) {
            return;
        }

        if (!isInsideBorder(to)) {
            WorldBorder border = arenaWorld.getWorldBorder();
            double borderRadius = border.getSize() / 2.0;
            double distance = to.distance(border.getCenter());

            if (distance > borderRadius + 2) {
                event.setCancelled(true);
                player.teleport(from);
                player.sendMessage("§c§l¡YOU SHALL NOT PASS!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.8f);
            }
        }
    }
    
    private boolean isInsideBorder(Location loc) {
        if (!arenaSet || loc.getWorld() != arenaWorld) {
            return true;
        }
        
        WorldBorder border = arenaWorld.getWorldBorder();
        double borderSize = border.getSize() / 2.0;
        Location center = border.getCenter();
        
        double dx = Math.abs(loc.getX() - center.getX());
        double dz = Math.abs(loc.getZ() - center.getZ());
        
        return dx <= borderSize && dz <= borderSize;
    }
    
    public boolean hasSelection(Player player) {
        return firstPoints.containsKey(player.getName()) && 
               secondPoints.containsKey(player.getName());
    }
    
    public void confirmArena(Player player) {
        if (!hasSelection(player)) {
            player.sendMessage("§c§lError: §fYou must select two points first");
            return;
        }
        
        Location loc1 = firstPoints.get(player.getName());
        Location loc2 = secondPoints.get(player.getName());
        
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            player.sendMessage("§c§lError: §fAmbos puntos deben estar en el mismo mundo");
            return;
        }
        
        double centerX = (loc1.getX() + loc2.getX()) / 2;
        double centerZ = (loc1.getZ() + loc2.getZ()) / 2;
        double centerY = (loc1.getY() + loc2.getY()) / 2;
        
        arenaCenter = new Location(loc1.getWorld(), centerX, centerY, centerZ);
        arenaWorld = loc1.getWorld();
        
        double distX = Math.abs(loc1.getX() - loc2.getX());
        double distZ = Math.abs(loc1.getZ() - loc2.getZ());
        arenaSize = Math.max(distX, distZ);

        initialCenter = arenaCenter.clone();
        initialSize = arenaSize;
        initialWorld = arenaWorld;
        
        arenaSet = true;

        plugin.getBorderConfig().saveArenaData(
            arenaWorld.getName(),
            arenaCenter.getX(),
            arenaCenter.getY(),
            arenaCenter.getZ(),
            arenaSize
        );
        
        player.sendMessage("§a§lArena set!");
        player.sendMessage("§fCenter: §e" + (int)centerX + ", " + (int)centerY + ", " + (int)centerZ);
        player.sendMessage("§fSize: §e" + (int)arenaSize + " blocks");
        player.sendMessage("§a§lArena configuration saved!");
    }
    
    public void setupBorder() {
        if (!arenaSet) {

            autoConfirmFromSelection();
        }
        
        if (!arenaSet || arenaCenter == null) {
            return;
        }
        
        WorldBorder border = arenaWorld.getWorldBorder();
        border.setCenter(arenaCenter);
        border.setSize(arenaSize);

        if (plugin.getBorderConfig().canTraverseBorder()) {

            border.setWarningDistance(plugin.getBorderConfig().getWarningDistance());
            border.setDamageAmount(plugin.getBorderConfig().getDamageAmount());
            border.setDamageBuffer(plugin.getBorderConfig().getDamageBuffer());
        } else {

            border.setWarningDistance(15); // Large warning zone
            border.setDamageAmount(5.0); // Massive damage (2.5 hearts per tick)
            border.setDamageBuffer(0); // Instant damage, no buffer
        }
    }
    
    private void autoConfirmFromSelection() {

        for (String playerName : firstPoints.keySet()) {
            if (secondPoints.containsKey(playerName)) {
                Location loc1 = firstPoints.get(playerName);
                Location loc2 = secondPoints.get(playerName);
                
                if (!loc1.getWorld().equals(loc2.getWorld())) {
                    continue;
                }
                
                double centerX = (loc1.getX() + loc2.getX()) / 2;
                double centerZ = (loc1.getZ() + loc2.getZ()) / 2;
                double centerY = (loc1.getY() + loc2.getY()) / 2;
                
                arenaCenter = new Location(loc1.getWorld(), centerX, centerY, centerZ);
                arenaWorld = loc1.getWorld();
                
                double distX = Math.abs(loc1.getX() - loc2.getX());
                double distZ = Math.abs(loc1.getZ() - loc2.getZ());
                arenaSize = Math.max(distX, distZ);

                initialCenter = arenaCenter.clone();
                initialSize = arenaSize;
                initialWorld = arenaWorld;
                
                arenaSet = true;
                return;
            }
        }
    }
    
    public void resetBorder() {
        if (arenaWorld != null) {
            WorldBorder border = arenaWorld.getWorldBorder();
            border.reset();
        }
        arenaSet = false;
    }
    
    public void resetToInitialSize() {
        if (initialWorld != null && initialCenter != null) {
            WorldBorder border = initialWorld.getWorldBorder();
            border.setCenter(initialCenter);
            border.setSize(initialSize);
            border.setWarningDistance(10);
            border.setDamageAmount(0.2);
            border.setDamageBuffer(5);
        }
    }
    
    public Location getArenaCenter() {
        return arenaCenter;
    }
    
    public double getArenaSize() {
        return arenaSize;
    }
    
    public World getArenaWorld() {
        return arenaWorld;
    }
    
    public boolean isArenaSet() {

        if (arenaSet || (initialCenter != null && initialWorld != null)) {
            return true;
        }

        for (String playerName : firstPoints.keySet()) {
            if (secondPoints.containsKey(playerName)) {
                return true;
            }
        }


        if (plugin.getBorderConfig().hasArenaData()) {
            loadArenaFromConfig();
            return arenaSet;
        }
        
        return false;
    }
    
    public void clearSelection(Player player) {
        firstPoints.remove(player.getName());
        secondPoints.remove(player.getName());
    }
    
    private Material getWandMaterial() {
        String materialName = plugin.getBorderConfig().getWandMaterial();
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STICK;
        }
    }
}