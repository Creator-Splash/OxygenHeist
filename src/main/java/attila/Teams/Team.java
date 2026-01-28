package attila.Teams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;

import attila.OxygenMain;

public class Team {
    
    private final String name;
    private UUID captainUUID;
    private final List<UUID> members;
    private Color armorColor;
    private boolean friendly;
    private Location baseLocation;
    private int points;
    
    public Team(String name, UUID captainUUID) {
        this.name = name;
        this.captainUUID = captainUUID;
        this.members = new ArrayList<>();
        if (captainUUID != null) {
            this.members.add(captainUUID);
        }
        this.armorColor = Color.WHITE;
        this.friendly = true;
        this.baseLocation = null;
        this.points = 0;
    }
    
    public String getName() {
        return name;
    }
    
    public UUID getCaptainUUID() {
        return captainUUID;
    }
    
    public void setCaptain(UUID captainUUID) {
        this.captainUUID = captainUUID;
        if (captainUUID != null && !members.contains(captainUUID)) {
            members.add(captainUUID);
        }
    }
    
    public boolean hasCaptain() {
        return captainUUID != null;
    }
    
    public List<UUID> getMembers() {
        return new ArrayList<>(members);
    }
    
    public boolean addMember(UUID playerUUID) {
        if (!members.contains(playerUUID)) {
            members.add(playerUUID);
            return true;
        }
        return false;
    }
    
    public boolean removeMember(UUID playerUUID) {

        boolean removed = members.remove(playerUUID);

        if (removed && playerUUID.equals(captainUUID)) {
            captainUUID = null;
        }
        
        return removed;
    }
    
    public boolean isMember(UUID playerUUID) {
        return members.contains(playerUUID);
    }
    
    public boolean isCaptain(UUID playerUUID) {
        return captainUUID != null && captainUUID.equals(playerUUID);
    }
    
    public Color getArmorColor() {
        return armorColor;
    }
    
    public void setArmorColor(Color color) {
        this.armorColor = color;
    }
    
    public void setArmorColor(int red, int green, int blue) {
        this.armorColor = Color.fromRGB(red, green, blue);
    }
    
    public int getSize() {
        return members.size();
    }
    
    public boolean isFriendlyFireEnabled() {
        return friendly;
    }
    
    public void setFriendlyFire(boolean enabled) {
        this.friendly = enabled;
    }
    
    public Location getBaseLocation() {
        return baseLocation;
    }
    
    public void setBaseLocation(Location location) {
        this.baseLocation = location;
    }
    
    public boolean hasBase() {
        return baseLocation != null;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    public void addPoints(int amount) {
        this.points += amount;
    }
    
    public void removePoints(int amount) {
        this.points = Math.max(0, this.points - amount);
    }
    
    /**
     * Saves this team's data to the configuration file.
     */
    public void saveToConfig(OxygenMain plugin) {
        String teamKey = name.toLowerCase();

        plugin.getTeamConfig().saveTeamData(teamKey, "name", name);
        plugin.getTeamConfig().saveTeamData(teamKey, "points", points);
        plugin.getTeamConfig().saveTeamData(teamKey, "friendly-fire", friendly);

        plugin.getTeamConfig().saveTeamData(teamKey, "color.red", armorColor.getRed());
        plugin.getTeamConfig().saveTeamData(teamKey, "color.green", armorColor.getGreen());
        plugin.getTeamConfig().saveTeamData(teamKey, "color.blue", armorColor.getBlue());

        if (captainUUID != null) {
            plugin.getTeamConfig().saveTeamData(teamKey, "captain", captainUUID.toString());
        } else {
            plugin.getTeamConfig().saveTeamData(teamKey, "captain", null);
        }

        List<String> memberStrings = new ArrayList<>();
        for (UUID uuid : members) {
            memberStrings.add(uuid.toString());
        }
        plugin.getTeamConfig().saveTeamData(teamKey, "members", memberStrings);

        if (baseLocation != null && baseLocation.getWorld() != null) {
            plugin.getTeamConfig().saveTeamData(teamKey, "base.world", baseLocation.getWorld().getName());
            plugin.getTeamConfig().saveTeamData(teamKey, "base.x", baseLocation.getX());
            plugin.getTeamConfig().saveTeamData(teamKey, "base.y", baseLocation.getY());
            plugin.getTeamConfig().saveTeamData(teamKey, "base.z", baseLocation.getZ());
            plugin.getTeamConfig().saveTeamData(teamKey, "base.yaw", baseLocation.getYaw());
            plugin.getTeamConfig().saveTeamData(teamKey, "base.pitch", baseLocation.getPitch());
        }
    }
}
