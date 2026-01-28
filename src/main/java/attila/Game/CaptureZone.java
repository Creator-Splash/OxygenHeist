package attila.Game;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import attila.Teams.Team;

import java.util.HashMap;
import java.util.Map;

public class CaptureZone {
    
    private final String name;
    private final Location center;
    private final double radius;
    private Team ownerTeam;
    private int captureProgress;
    private Team capturingTeam;
    private ArmorStand hologramTitle;
    private ArmorStand hologramProgress;

    private final Map<Team, Double> teamOxygenPercent = new HashMap<>();

    private ArmorStand hologramOxygen;

    private static final double DEFAULT_DRAIN_TIME_SECONDS = 120.0; // 2 minutes

    private static final double DRAIN_PER_TICK = 100.0 / (DEFAULT_DRAIN_TIME_SECONDS * 20.0);

    private static final double REFILL_PER_TICK = DRAIN_PER_TICK * 0.5; // Refill at half the drain rate

    private double particleAngleOffset = 0;
    
    public CaptureZone(String name, Location center, double radius) {
        this.name = name;
        this.center = center;
        this.radius = radius;
        this.ownerTeam = null;
        this.captureProgress = 0;
        this.capturingTeam = null;
        this.hologramTitle = null;
        this.hologramProgress = null;
        this.hologramOxygen = null;
        createHolograms();
    }
    
    public String getName() {
        return name;
    }
    
    public Location getCenter() {
        return center;
    }
    
    public double getRadius() {
        return radius;
    }
    
    public Team getOwnerTeam() {
        return ownerTeam;
    }
    
    public void setOwnerTeam(Team team) {
        this.ownerTeam = team;
        this.captureProgress = 100;
        this.capturingTeam = null;
        updateHologram();
    }
    
    public boolean hasOwner() {
        return ownerTeam != null;
    }
    
    public int getCaptureProgress() {
        return captureProgress;
    }
    
    public void setCaptureProgress(int progress) {
        this.captureProgress = Math.max(0, Math.min(100, progress));
        updateHologram();
    }
    
    public void addCaptureProgress(int amount) {

        int newProgress = captureProgress + amount;
        setCaptureProgress(newProgress);
    }
    
    public Team getCapturingTeam() {
        return capturingTeam;
    }
    
    public void setCapturingTeam(Team team) {
        this.capturingTeam = team;
        updateHologram();
    }
    
    public boolean isPlayerInZone(Location playerLoc) {
        if (!playerLoc.getWorld().equals(center.getWorld())) {
            return false;
        }
        double distance = playerLoc.distance(center);
        return distance <= radius;
    }
    
    public void resetCapture() {
        this.captureProgress = 0;
        this.capturingTeam = null;
        this.ownerTeam = null;
        this.teamOxygenPercent.clear();
        updateHologram();
    }
    
    /**
     * Initialize oxygen percentage for a team at this capture zone
     */
    public void initTeamOxygen(Team team) {
        if (!teamOxygenPercent.containsKey(team)) {
            teamOxygenPercent.put(team, 100.0);
        }
    }
    
    /**
     * Get the oxygen percentage for a team at this capture zone
     */
    public double getTeamOxygenPercent(Team team) {
        return teamOxygenPercent.getOrDefault(team, 100.0);
    }
    
    /**
     * Set the oxygen percentage for a team at this capture zone
     */
    public void setTeamOxygenPercent(Team team, double percent) {
        teamOxygenPercent.put(team, Math.max(0.0, Math.min(100.0, percent)));
        updateHologram();
    }
    
    /**
     * Drain oxygen for a team based on player count on the point
     * @param team The team to drain oxygen for
     * @param playerCount Number of players from this team on the point (1-5)
     * @return true if oxygen reached 0% and should start refilling
     */
    public boolean drainTeamOxygen(Team team, int playerCount) {
        double currentOxygen = getTeamOxygenPercent(team);
        if (currentOxygen <= 0) {
            return true; // Already at 0%, needs refilling
        }

        double drainMultiplier = Math.min(5, Math.max(1, playerCount));
        double newOxygen = currentOxygen - (DRAIN_PER_TICK * drainMultiplier);
        
        if (newOxygen <= 0) {
            newOxygen = 0;
            teamOxygenPercent.put(team, newOxygen);
            updateHologram();
            return true; // Reached 0%, start refilling
        }
        
        teamOxygenPercent.put(team, newOxygen);
        updateHologram();
        return false;
    }
    
    /**
     * Refill oxygen for a team (called when oxygen reaches 0%)
     * @param team The team to refill oxygen for
     */
    public void refillTeamOxygen(Team team) {
        double currentOxygen = getTeamOxygenPercent(team);
        if (currentOxygen >= 100.0) {
            return; // Already full
        }
        
        double newOxygen = currentOxygen + REFILL_PER_TICK;
        teamOxygenPercent.put(team, Math.min(100.0, newOxygen));
        updateHologram();
    }
    
    /**
     * Check if a team's oxygen is currently refilling (at 0%)
     */
    public boolean isTeamOxygenRefilling(Team team) {
        return getTeamOxygenPercent(team) <= 0;
    }
    
    private void createHolograms() {
        if (center.getWorld() == null) return;

        Location oxygenLoc = center.clone().add(0, 3.2, 0);
        hologramOxygen = (ArmorStand) center.getWorld().spawnEntity(oxygenLoc, EntityType.ARMOR_STAND);
        hologramOxygen.setVisible(false);
        hologramOxygen.setGravity(false);
        hologramOxygen.setMarker(true);
        hologramOxygen.setInvulnerable(true);
        hologramOxygen.setCustomName(buildOxygenDisplay());
        hologramOxygen.setCustomNameVisible(true);

        Location titleLoc = center.clone().add(0, 2.8, 0);
        hologramTitle = (ArmorStand) center.getWorld().spawnEntity(titleLoc, EntityType.ARMOR_STAND);
        hologramTitle.setVisible(false);
        hologramTitle.setGravity(false);
        hologramTitle.setMarker(true);
        hologramTitle.setInvulnerable(true);
        hologramTitle.setCustomName("§8§l[NEUTRAL] §f" + name);
        hologramTitle.setCustomNameVisible(true);

        Location progressLoc = center.clone().add(0, 2.4, 0);
        hologramProgress = (ArmorStand) center.getWorld().spawnEntity(progressLoc, EntityType.ARMOR_STAND);
        hologramProgress.setVisible(false);
        hologramProgress.setGravity(false);
        hologramProgress.setMarker(true);
        hologramProgress.setInvulnerable(true);
        hologramProgress.setCustomName(buildProgressBar());
        hologramProgress.setCustomNameVisible(true);
    }
    
    public void recreateHolograms() {
        removeHologram();
        createHolograms();
    }
    
    public void updateHologram() {

        if (hologramOxygen == null || !hologramOxygen.isValid()) {
            if (center.getWorld() != null) {
                Location oxygenLoc = center.clone().add(0, 3.2, 0);
                hologramOxygen = (ArmorStand) center.getWorld().spawnEntity(oxygenLoc, EntityType.ARMOR_STAND);
                hologramOxygen.setVisible(false);
                hologramOxygen.setGravity(false);
                hologramOxygen.setMarker(true);
                hologramOxygen.setInvulnerable(true);
                hologramOxygen.setCustomName(buildOxygenDisplay());
                hologramOxygen.setCustomNameVisible(true);
            }
        } else {
            hologramOxygen.setCustomName(buildOxygenDisplay());
        }

        if (hologramTitle == null || !hologramTitle.isValid()) {
            if (center.getWorld() != null) {
                Location titleLoc = center.clone().add(0, 2.8, 0);
                hologramTitle = (ArmorStand) center.getWorld().spawnEntity(titleLoc, EntityType.ARMOR_STAND);
                hologramTitle.setVisible(false);
                hologramTitle.setGravity(false);
                hologramTitle.setMarker(true);
                hologramTitle.setInvulnerable(true);

                if (ownerTeam != null) {
                    String colorCode = getTeamColorCode(ownerTeam);
                    hologramTitle.setCustomName(colorCode + "§l" + ownerTeam.getName() + " §7| §f" + name);
                } else {
                    hologramTitle.setCustomName("§8§l[NEUTRAL] §f" + name);
                }
                hologramTitle.setCustomNameVisible(true);
            }
        }
        
        if (hologramProgress == null || !hologramProgress.isValid()) {
            if (center.getWorld() != null) {
                Location progressLoc = center.clone().add(0, 2.4, 0);
                hologramProgress = (ArmorStand) center.getWorld().spawnEntity(progressLoc, EntityType.ARMOR_STAND);
                hologramProgress.setVisible(false);
                hologramProgress.setGravity(false);
                hologramProgress.setMarker(true);
                hologramProgress.setInvulnerable(true);

                hologramProgress.setCustomName(buildProgressBar());
                hologramProgress.setCustomNameVisible(true);
            }
        } else {

            if (ownerTeam != null) {
                String colorCode = getTeamColorCode(ownerTeam);
                hologramTitle.setCustomName(colorCode + "§l" + ownerTeam.getName() + " §7| §f" + name);
            } else {
                hologramTitle.setCustomName("§8§l[NEUTRAL] §f" + name);
            }

            if (hologramProgress != null) {
                hologramProgress.setCustomName(buildProgressBar());
            }
        }
    }
    
    private String buildProgressBar() {
        int barLength = 20;
        int filledBars = (int) ((captureProgress / 100.0) * barLength);
        
        StringBuilder bar = new StringBuilder();
        
        if (capturingTeam != null && captureProgress > 0 && captureProgress < 100) {

            String capturingColor = getTeamColorCode(capturingTeam);
            bar.append(capturingColor).append("⚡ ");
            
            for (int i = 0; i < barLength; i++) {
                if (i < filledBars) {
                    bar.append(capturingColor).append("█");
                } else {
                    bar.append("§7░");
                }
            }
            
            bar.append(" §f").append(captureProgress).append("%");
        } else if (ownerTeam != null && captureProgress == 100) {

            String ownerColor = getTeamColorCode(ownerTeam);
            bar.append(ownerColor).append("✔ ");
            
            for (int i = 0; i < barLength; i++) {
                bar.append(ownerColor).append("█");
            }
            
            bar.append(" §a100%");
        } else {

            bar.append("§7○ ");
            
            for (int i = 0; i < barLength; i++) {
                bar.append("§8░");
            }
            
            bar.append(" §70%");
        }
        
        return bar.toString();
    }
    
    /**
     * Build the oxygen display showing each team's oxygen percentage
     */
    private String buildOxygenDisplay() {
        if (teamOxygenPercent.isEmpty()) {
            return "§7⬡ Oxygen: §8No teams";
        }
        
        StringBuilder display = new StringBuilder();
        display.append("§b⬡ ");
        
        boolean first = true;
        for (Map.Entry<Team, Double> entry : teamOxygenPercent.entrySet()) {
            Team team = entry.getKey();
            double percent = entry.getValue();
            String colorCode = getTeamColorCode(team);
            
            if (!first) {
                display.append(" §7| ");
            }
            first = false;

            String teamName = team.getName();
            String abbreviation = teamName.length() > 3 ? teamName.substring(0, 3) : teamName;

            String percentColor;
            if (percent >= 70) {
                percentColor = "§a"; // Green for high
            } else if (percent >= 30) {
                percentColor = "§e"; // Yellow for medium
            } else if (percent > 0) {
                percentColor = "§c"; // Red for low
            } else {
                percentColor = "§4"; // Dark red for depleted (refilling)
            }
            
            display.append(colorCode).append(abbreviation).append(" ");
            display.append(percentColor).append(String.format("%.0f%%", percent));

            if (percent <= 0) {
                display.append(" §d↻");
            }
        }
        
        return display.toString();
    }
    
    private String getTeamColorCode(Team team) {
        if (team == null) return "§7";
        
        Color color = team.getArmorColor();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        
        if (r > 200 && g < 80 && b < 80) return "§c";
        if (r < 80 && g < 80 && b > 200) return "§9";
        if (r < 80 && g > 200 && b < 80) return "§a";
        if (r > 200 && g > 200 && b < 80) return "§e";
        if (r > 150 && g < 80 && b > 150) return "§5";
        if (r > 200 && g > 100 && b < 80) return "§6";
        if (r > 200 && g < 150 && b > 200) return "§d";
        if (r < 100 && g > 200 && b > 200) return "§b";
        if (r > 230 && g > 230 && b > 230) return "§f";
        if (r < 50 && g < 50 && b < 50) return "§0";
        
        return "§f";
    }
    
    public void removeHologram() {
        if (hologramOxygen != null && hologramOxygen.isValid()) {
            hologramOxygen.remove();
        }
        if (hologramTitle != null && hologramTitle.isValid()) {
            hologramTitle.remove();
        }
        if (hologramProgress != null && hologramProgress.isValid()) {
            hologramProgress.remove();
        }
    }
    
    public void spawnIdleParticles() {
        if (center.getWorld() == null) return;
        
        Color color = ownerTeam != null ? ownerTeam.getArmorColor() : Color.GRAY;

        particleAngleOffset += 0.05;
        if (particleAngleOffset >= Math.PI * 2) {
            particleAngleOffset = 0;
        }

        Particle.DustOptions outerDust = new Particle.DustOptions(color, 1.4f);
        int outerPoints = Math.max(16, (int) (radius * 4));
        double angleStep = (Math.PI * 2) / outerPoints;
        
        for (int i = 0; i < outerPoints; i++) {
            double angle = (i * angleStep) + particleAngleOffset;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLoc = new Location(center.getWorld(), x, center.getY() + 0.2, z);
            center.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, outerDust);
        }

        Particle.DustOptions innerDust = new Particle.DustOptions(color, 1.0f);
        double innerRadius = radius * 0.5;
        int innerPoints = Math.max(8, (int) (innerRadius * 4));
        double innerAngleStep = (Math.PI * 2) / innerPoints;
        
        for (int i = 0; i < innerPoints; i++) {
            double angle = (i * innerAngleStep) - (particleAngleOffset * 1.5);
            double x = center.getX() + innerRadius * Math.cos(angle);
            double z = center.getZ() + innerRadius * Math.sin(angle);
            Location particleLoc = new Location(center.getWorld(), x, center.getY() + 0.15, z);
            center.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, innerDust);
        }

        double pulse = (Math.sin(particleAngleOffset * 3) + 1) / 2; // 0 to 1
        float centerSize = 1.2f + (float) (pulse * 0.4f);
        Particle.DustOptions centerDust = new Particle.DustOptions(color, centerSize);
        Location centerLoc = center.clone().add(0, 0.25, 0);
        center.getWorld().spawnParticle(Particle.DUST, centerLoc, 2, 0.1, 0, 0.1, 0, centerDust);

        if (captureProgress > 0 && captureProgress < 100 && capturingTeam != null) {
            Color captureColor = capturingTeam.getArmorColor();
            Particle.DustOptions pillarDust = new Particle.DustOptions(captureColor, 1.2f);
            
            double heightProgress = (captureProgress / 100.0) * 3.0;
            
            for (int cardinal = 0; cardinal < 4; cardinal++) {
                double angle = (Math.PI / 2) * cardinal + particleAngleOffset;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                
                for (double y = 0; y < heightProgress; y += 0.4) {
                    Location pillarLoc = new Location(center.getWorld(), x, center.getY() + y, z);
                    center.getWorld().spawnParticle(Particle.DUST, pillarLoc, 1, 0, 0, 0, 0, pillarDust);
                }
            }
        }
    }
    
    /**
     * Spawns active capture particles - more intense when capturing
     */
    public void spawnParticles(Color color) {
        if (center.getWorld() == null) return;
        
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.6f);

        particleAngleOffset += 0.1;
        
        for (double y = 0; y < 3; y += 0.3) {
            double spiralRadius = radius * 0.4 * (1 - y / 4);
            double angle = particleAngleOffset + (y * 2);
            double x = center.getX() + spiralRadius * Math.cos(angle);
            double z = center.getZ() + spiralRadius * Math.sin(angle);
            Location particleLoc = new Location(center.getWorld(), x, center.getY() + y, z);
            center.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
        }

        int points = (int) (radius * 6);
        double angleStep = (Math.PI * 2) / points;
        for (int i = 0; i < points; i++) {
            double angle = (i * angleStep) + particleAngleOffset;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double yOffset = Math.sin(angle * 4 + particleAngleOffset * 2) * 0.3;
            Location particleLoc = new Location(center.getWorld(), x, center.getY() + 0.5 + yOffset, z);
            center.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
        }
    }
}
