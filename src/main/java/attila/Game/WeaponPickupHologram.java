package attila.Game;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.Particle.DustOptions;

/**
 * Represents a weapon pickup hologram that players can walk through to receive items/commands.
 */
public class WeaponPickupHologram {
    
    private final String id;
    private final Location location;
    private final String displayName;
    private final String command;
    private final double pickupRadius;
    private ArmorStand hologramName;
    private ArmorStand hologramIcon;
    private boolean active;
    private double animationOffset = 0;
    
    /**
     * Creates a new weapon pickup hologram.
     * @param id Unique identifier for this hologram
     * @param location The center location of the hologram
     * @param displayName The name to show above the hologram
     * @param command The command to execute when picked up (use %player% for player name)
     * @param pickupRadius The radius within which players can pick up
     */
    public WeaponPickupHologram(String id, Location location, String displayName, String command, double pickupRadius) {
        this.id = id;
        this.location = location.clone();
        this.displayName = displayName;
        this.command = command;
        this.pickupRadius = pickupRadius;
        this.active = true;
        createHolograms();
    }
    
    public String getId() {
        return id;
    }
    
    public Location getLocation() {
        return location.clone();
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCommand() {
        return command;
    }
    
    public double getPickupRadius() {
        return pickupRadius;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Checks if a player is within pickup range.
     */
    public boolean isPlayerInRange(Location playerLoc) {
        if (!active) return false;
        if (playerLoc.getWorld() == null || !playerLoc.getWorld().equals(location.getWorld())) {
            return false;
        }
        return playerLoc.distance(location) <= pickupRadius;
    }
    
    /**
     * Execute the command for the given player.
     */
    public void executeCommand(Player player) {
        if (command == null || command.isEmpty()) return;
        
        String finalCommand = command.replace("%player%", player.getName());



        if (finalCommand.contains("custom_model_data=") && !finalCommand.contains("custom_model_data={")) {
            finalCommand = finalCommand.replaceAll("custom_model_data=(\\d+)", "custom_model_data={floats:[$1]}");
        }
        
        try {
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), finalCommand);
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().warning("[OxygenHeist] Failed to execute weapon command: " + finalCommand);
            org.bukkit.Bukkit.getLogger().warning("[OxygenHeist] Error: " + e.getMessage());
        }
    }
    
    private void createHolograms() {
        if (location.getWorld() == null) return;

        Location nameLoc = location.clone().add(0, 2.5, 0);
        hologramName = (ArmorStand) location.getWorld().spawnEntity(nameLoc, EntityType.ARMOR_STAND);
        hologramName.setVisible(false);
        hologramName.setGravity(false);
        hologramName.setMarker(true);
        hologramName.setInvulnerable(true);
        hologramName.setCustomName("§b§l⚔ §f" + displayName + " §b§l⚔");
        hologramName.setCustomNameVisible(true);

        Location iconLoc = location.clone().add(0, 2.1, 0);
        hologramIcon = (ArmorStand) location.getWorld().spawnEntity(iconLoc, EntityType.ARMOR_STAND);
        hologramIcon.setVisible(false);
        hologramIcon.setGravity(false);
        hologramIcon.setMarker(true);
        hologramIcon.setInvulnerable(true);
        hologramIcon.setCustomName("§7[ §e¡PICKUP! §7]");
        hologramIcon.setCustomNameVisible(true);
    }
    
    /**
     * Spawns particles around the hologram for visibility.
     */
    public void spawnParticles() {
        if (!active || location.getWorld() == null) return;
        
        animationOffset += 0.15;
        if (animationOffset > Math.PI * 2) {
            animationOffset = 0;
        }

        for (int i = 0; i < 8; i++) {
            double angle = animationOffset + (Math.PI * 2 * i / 8);
            double x = Math.cos(angle) * pickupRadius * 0.7;
            double z = Math.sin(angle) * pickupRadius * 0.7;
            
            Location particleLoc = location.clone().add(x, 1.0, z);
            DustOptions dustOptions = new DustOptions(Color.fromRGB(0, 255, 255), 1.0f);
            location.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
        }

        double yOffset = Math.sin(animationOffset * 2) * 0.3;
        Location centerParticle = location.clone().add(0, 1.5 + yOffset, 0);
        location.getWorld().spawnParticle(Particle.END_ROD, centerParticle, 2, 0.1, 0.1, 0.1, 0.01);
    }
    
    /**
     * Updates the hologram animation (bobbing effect).
     */
    public void updateAnimation() {
        if (!active) return;
        
        animationOffset += 0.1;
        if (animationOffset > Math.PI * 2) {
            animationOffset = 0;
        }
        
        double yOffset = Math.sin(animationOffset) * 0.15;
        
        if (hologramName != null && hologramName.isValid()) {
            Location newLoc = location.clone().add(0, 2.5 + yOffset, 0);
            hologramName.teleport(newLoc);
        }
        
        if (hologramIcon != null && hologramIcon.isValid()) {
            Location newLoc = location.clone().add(0, 2.1 + yOffset, 0);
            hologramIcon.teleport(newLoc);
        }
    }
    
    /**
     * Removes the hologram entities from the world.
     */
    public void remove() {
        active = false;
        
        if (hologramName != null && hologramName.isValid()) {
            hologramName.remove();
        }
        if (hologramIcon != null && hologramIcon.isValid()) {
            hologramIcon.remove();
        }
    }
    
    /**
     * Plays pickup effects and removes the hologram.
     */
    public void onPickup(Player player) {
        if (!active) return;

        player.playSound(location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
        player.playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);

        if (location.getWorld() != null) {
            location.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0.1);
        }

        executeCommand(player);

        remove();
    }
}
