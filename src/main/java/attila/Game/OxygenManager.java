package attila.Game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import attila.Config.GameConfigHandler;
import attila.OxygenMain;
import attila.Teams.Team;
import attila.Teams.TeamManager;

public class OxygenManager implements Listener {
    
    private final OxygenMain plugin;
    private final GameConfigHandler config;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    
    private final Map<UUID, Integer> playerOxygen;
    private BukkitTask oxygenTask;
    private boolean active;
    
    public OxygenManager(OxygenMain plugin, GameManager gameManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.config = plugin.getGameConfig();
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.playerOxygen = new HashMap<>();
        this.active = false;
    }
    
    public void startOxygenSystem() {
        active = true;
        
        for (Team team : teamManager.getAllTeams().values()) {
            for (UUID uuid : team.getMembers()) {
                playerOxygen.put(uuid, config.getMaxOxygen());
            }
        }
        
        oxygenTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || gameManager.getGameState() != GameState.PLAYING) {
                    cancel();
                    return;
                }
                processOxygen();
            }
        }.runTaskTimer(plugin, 0L, config.getOxygenDrainInterval());
    }
    
    public void stopOxygenSystem() {
        active = false;
        if (oxygenTask != null) {
            oxygenTask.cancel();
            oxygenTask = null;
        }
        
        for (UUID uuid : playerOxygen.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setRemainingAir(player.getMaximumAir());
            }
        }
        
        playerOxygen.clear();
    }
    
    private void processOxygen() {
        String playgroundWorld = config.getPlaygroundWorld();
        
        for (UUID uuid : new HashMap<>(playerOxygen).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            
            if (!playgroundWorld.isEmpty() && !player.getWorld().getName().equals(playgroundWorld)) {
                continue;
            }

            if (gameManager.isPlayerDead(uuid) || gameManager.isPlayerDowned(uuid)) {
                continue;
            }

            Team playerTeam = teamManager.getPlayerTeam(player);
            if (playerTeam != null) {
                boolean inOwnZone = false;
                for (CaptureZone zone : gameManager.getAllCaptureZones().values()) {
                    if (zone.hasOwner() && zone.getOwnerTeam().equals(playerTeam) && zone.isPlayerInZone(player.getLocation())) {
                        inOwnZone = true;
                        break;
                    }
                }
                if (inOwnZone) {
                    continue; 
                }
            }
            
            int oxygen = playerOxygen.getOrDefault(uuid, config.getMaxOxygen());
            oxygen -= config.getOxygenDrainAmount();
            
            if (oxygen < 0) oxygen = 0;
            playerOxygen.put(uuid, oxygen);
            
            int maxAir = player.getMaximumAir();
            int airBubbles = (int) ((double) oxygen / config.getMaxOxygen() * maxAir);


            player.setRemainingAir(Math.max(0, airBubbles));
            
            if (oxygen <= 0) {
                player.damage(config.getSuffocationDamage());
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 0.5f, 1f);
            } else if (oxygen <= config.getMaxOxygen() * 0.2) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.3f, 1.5f);
            }
        }
    }
    
    public void restoreOxygen(Team team, int amount) {
        for (UUID uuid : team.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            if (gameManager.isPlayerDead(uuid) || gameManager.isPlayerDowned(uuid)) continue;
            
            int oxygen = playerOxygen.getOrDefault(uuid, 0);
            int newOxygen = Math.min(config.getMaxOxygen(), oxygen + amount);
            playerOxygen.put(uuid, newOxygen);

            int maxAir = player.getMaximumAir();
            int airBubbles = (int) ((double) newOxygen / config.getMaxOxygen() * maxAir);
            player.setRemainingAir(Math.max(0, airBubbles));

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        }
    }
    
    public void restoreFullOxygen(Player player) {
        UUID uuid = player.getUniqueId();
        playerOxygen.put(uuid, config.getMaxOxygen());
        player.setRemainingAir(player.getMaximumAir());
    }
    
    public int getPlayerOxygen(UUID uuid) {
        return playerOxygen.getOrDefault(uuid, config.getMaxOxygen());
    }
    
    public int getTeamOxygen(Team team) {
        if (team == null) return config.getMaxOxygen();
        
        int totalOxygen = 0;
        int count = 0;
        
        for (UUID uuid : team.getMembers()) {
            if (playerOxygen.containsKey(uuid)) {
                totalOxygen += playerOxygen.get(uuid);
                count++;
            }
        }
        
        return count > 0 ? totalOxygen / count : config.getMaxOxygen();
    }
    
    public int getMaxOxygen() {
        return config.getMaxOxygen();
    }
    
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.DROWNING) return;
        
        Player player = (Player) event.getEntity();
        if (playerOxygen.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevents vanilla air regeneration by canceling air change events.
     * This stops the flickering caused by Minecraft trying to regenerate air.
     */
    @EventHandler
    public void onAirChange(org.bukkit.event.entity.EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!active) return;
        
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        
        if (!playerOxygen.containsKey(uuid)) return;

        int oxygen = playerOxygen.getOrDefault(uuid, config.getMaxOxygen());
        int maxAir = player.getMaximumAir();
        int targetAir = (int) ((double) oxygen / config.getMaxOxygen() * maxAir);

        if (event.getAmount() != targetAir) {
            event.setAmount(targetAir);
        }
    }
    
    /**
     * Prevents vanilla air regeneration for managed players.
     * Called every tick to override Minecraft's natural air bubble regeneration.
     */
    @EventHandler
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {

        if (playerOxygen.containsKey(event.getPlayer().getUniqueId())) {
            playerOxygen.put(event.getPlayer().getUniqueId(), config.getMaxOxygen());
        }
    }
    

    
    public boolean isActive() {
        return active;
    }
}
