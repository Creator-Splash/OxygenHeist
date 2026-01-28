package attila.Game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import attila.Config.GameConfigHandler;
import attila.OxygenMain;
import attila.Teams.Team;
import attila.Teams.TeamManager;

public class GameManager implements Listener {
    
    private final OxygenMain plugin;
    private final TeamManager teamManager;
    private final GameConfigHandler config;
    
    private GameState gameState;
    private final Map<String, CaptureZone> captureZones;
    private final Set<UUID> deadPlayers;
    private final Set<UUID> downedPlayers;
    private final Map<UUID, Integer> bleedoutTimers;
    private final Map<UUID, Double> reviveProgress;
    private final Map<UUID, BossBar> downedBossBars;
    private final Map<UUID, UUID> revivingPlayer;
    private final Map<UUID, UUID> lastAttacker;
    private BossBar scoreBossBar;
    private BukkitTask gameTask;
    private BukkitTask captureTask;
    private BukkitTask zoneOxygenTask;
    private BukkitTask glowTask;
    private BukkitTask knockdownTask;
    private BukkitTask idleParticleTask;
    private BukkitTask countdownTask;
    private long roundStartTime;
    private boolean instantDeathMode;
    private OxygenManager oxygenManager;
    private WeaponSpawnerManager weaponSpawnerManager;
    private boolean zonesLoaded = false;
    private Set<String> pendingZoneWorlds = new HashSet<>();

    private final Map<CaptureZone, Set<Team>> refillingTeams = new HashMap<>();
    
    public GameManager(OxygenMain plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.config = plugin.getGameConfig();
        this.gameState = GameState.WAITING;
        this.captureZones = new HashMap<>();
        this.deadPlayers = new HashSet<>();
        this.downedPlayers = new HashSet<>();
        this.bleedoutTimers = new HashMap<>();
        this.reviveProgress = new HashMap<>();
        this.downedBossBars = new HashMap<>();
        this.revivingPlayer = new HashMap<>();
        this.lastAttacker = new HashMap<>();
        
        this.scoreBossBar = Bukkit.createBossBar(
            "§6§lNo game in progress",
            BarColor.YELLOW,
            BarStyle.SOLID
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Server ready, loading capture zones...");
                loadZones();
                if (captureZones.isEmpty()) {
                    plugin.getLogger().warning("No zones loaded! Check if the world is loaded.");
                }
            }
        }.runTaskLater(plugin, 40L); // Wait 2 seconds after plugin enable

        startIdleParticleTask();
    }
    
    /**
     * Called when a world is loaded - retry loading zones that were waiting for this world
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        if (pendingZoneWorlds.contains(worldName)) {
            plugin.getLogger().info("World '" + worldName + "' loaded, retrying zone loading...");
            pendingZoneWorlds.remove(worldName);

            reloadZones();
        }
    }
    
    /**
     * Reloads all capture zones from config
     */
    public void reloadZones() {

        for (CaptureZone zone : captureZones.values()) {
            zone.removeHologram();
        }
        captureZones.clear();

        loadZones();
    }
    
    private void startIdleParticleTask() {
        idleParticleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (CaptureZone zone : captureZones.values()) {
                    zone.spawnIdleParticles();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * Gives a random weapon to all online players at game start.
     */
    private void giveRandomWeaponsToPlayers() {
        java.util.List<String> weaponIds = config.getWeaponIds();
        if (weaponIds.isEmpty()) {
            plugin.getLogger().warning("No weapons configured to give to players!");
            return;
        }
        
        java.util.Random random = new java.util.Random();
        
        for (Player player : Bukkit.getOnlinePlayers()) {

            String weaponId = weaponIds.get(random.nextInt(weaponIds.size()));
            String command = config.getWeaponCommand(weaponId);
            String weaponName = config.getWeaponDisplayName(weaponId);
            
            if (!command.isEmpty()) {

                if (command.contains("custom_model_data=") && !command.contains("custom_model_data={")) {
                    command = command.replaceAll("custom_model_data=(\\d+)", "custom_model_data={floats:[$1]}");
                }

                String finalCommand = command.replace("%player%", player.getName());
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    player.sendMessage("§a§lStarting weapon: §b" + weaponName);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to give weapon '" + weaponId + "' to " + player.getName() + ": " + e.getMessage());
                    plugin.getLogger().warning("Command was: " + finalCommand);
                }
            }
        }
        
        plugin.getLogger().info("Gave random weapons to all players at game start.");
    }
    
    public void setOxygenManager(OxygenManager oxygenManager) {
        this.oxygenManager = oxygenManager;
    }
    
    public void setWeaponSpawnerManager(WeaponSpawnerManager weaponSpawnerManager) {
        this.weaponSpawnerManager = weaponSpawnerManager;
    }
    
    public WeaponSpawnerManager getWeaponSpawnerManager() {
        return weaponSpawnerManager;
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public boolean canStartGame() {
        if (gameState != GameState.WAITING) {
            return false;
        }
        
        String playgroundWorld = config.getPlaygroundWorld();
        if (playgroundWorld == null || playgroundWorld.isEmpty()) {
            return false;
        }
        
        Map<String, Team> teams = teamManager.getAllTeams();
        if (teams.size() < config.getMinTeams()) {
            return false;
        }
        
        for (Team team : teams.values()) {
            if (!team.hasBase()) {
                return false;
            }
        }
        
        return plugin.getBorderManager().isArenaSet();
    }
    
    public String getStartError() {
        if (gameState != GameState.WAITING) {
            return config.getMessage("game-already-running");
        }
        
        String playgroundWorld = config.getPlaygroundWorld();
        if (playgroundWorld == null || playgroundWorld.isEmpty()) {
            return config.getMessage("playground-not-set");
        }
        
        Map<String, Team> teams = teamManager.getAllTeams();
        if (teams.size() < config.getMinTeams()) {
            return config.getMessage("not-enough-teams")
                .replace("%min%", String.valueOf(config.getMinTeams()));
        }
        
        for (Team team : teams.values()) {
            if (!team.hasBase()) {
                return config.getMessage("no-bases-set");
            }
        }
        
        if (!plugin.getBorderManager().isArenaSet()) {
            return config.getMessage("border-not-set");
        }
        
        return null;
    }
    
    public void startCountdown() {
        gameState = GameState.COUNTDOWN;
        int countdown = config.getCountdownTime();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreBossBar.addPlayer(player);
        }
        
        countdownTask = new BukkitRunnable() {
            int time = countdown;
            
            @Override
            public void run() {

                if (gameState != GameState.COUNTDOWN) {
                    cancel();
                    return;
                }

                if (time > 0) {
                    String message = config.getMessage("game-starting")
                        .replace("%time%", String.valueOf(time));
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle("§6§l" + time, message, 0, 25, 5);
                    }
                    
                    scoreBossBar.setTitle("§e§lGame starting in §6" + time + "§e§l seconds");
                    scoreBossBar.setProgress((double) time / countdown);
                    
                    time--;
                } else {

                    cancel();
                    startGame();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    public void startGame() {
        gameState = GameState.PLAYING;
        roundStartTime = System.currentTimeMillis();
        instantDeathMode = false;

        if (plugin.getBorderManager().isArenaSet()) {
            plugin.getBorderManager().setupBorder();
        } else {
            plugin.getLogger().warning("Border not properly set! Game may not work correctly.");
        }
        
        for (Team team : teamManager.getAllTeams().values()) {
            team.setPoints(0);
        }
        
        Bukkit.broadcastMessage(config.getMessage("game-started"));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§a§lGAME START!", "§7Good luck!", 0, 40, 20);
        }
        
        spawnAllTeams();

        int maxSpawnDelay = Math.max(config.getCaptainSpawnDelay(), config.getTeamSpawnDelay());
        new BukkitRunnable() {
            @Override
            public void run() {
                giveRandomWeaponsToPlayers();
            }
        }.runTaskLater(plugin, (maxSpawnDelay * 20L) + 40L); // +40 ticks = 2 seconds buffer
        
        startGameTasks();
        
        if (config.isOxygenEnabled() && oxygenManager != null) {
            oxygenManager.startOxygenSystem();
        }

        if (config.isWeaponSpawnerEnabled() && weaponSpawnerManager != null) {
            weaponSpawnerManager.start();
        }

        if (plugin.getBorderManager().isArenaSet()) {
            int shrinkDelay = config.getBorderShrinkDelay();
            int shrinkDuration = config.getBorderShrinkDuration();
            double shrinkPercent = config.getBorderShrinkSizePercent();
            
            double currentSize = plugin.getBorderShrink().getCurrentBorderSize();
            double targetSize = currentSize * (shrinkPercent / 100.0);
            
            plugin.getBorderTimer().startTimer(shrinkDelay, targetSize, shrinkDuration);
            plugin.getLogger().info("Border shrinking timer started: " + shrinkDelay + "s delay, target " + (int)targetSize + " blocks (" + shrinkPercent + "%) in " + shrinkDuration + "s");
        }
        
        updateBossBar();
    }
    
    private void spawnAllTeams() {
        for (Team team : teamManager.getAllTeams().values()) {
            spawnTeamWithCaptain(team);
        }
    }
    
    public void spawnTeamWithCaptain(Team team) {
        if (!team.hasBase()) {
            plugin.getLogger().warning("Team '" + team.getName() + "' has no base set! Cannot spawn team.");
            return;
        }
        
        Location base = team.getBaseLocation();
        if (base == null) {
            plugin.getLogger().warning("Team '" + team.getName() + "' base location is null! Cannot spawn team.");
            return;
        }
        
        plugin.getLogger().info("Spawning team '" + team.getName() + "' at base: " + base.getWorld().getName() + " (" + (int)base.getX() + ", " + (int)base.getY() + ", " + (int)base.getZ() + ")");
        
        int captainDelay = config.getCaptainSpawnDelay();
        int teamDelay = config.getTeamSpawnDelay();

        if (team.hasCaptain()) {
            Player captain = Bukkit.getPlayer(team.getCaptainUUID());
            if (captain != null && captain.isOnline()) {
                new BukkitRunnable() {
                    int time = captainDelay;
                    @Override
                    public void run() {

                        if (gameState != GameState.PLAYING || !captain.isOnline()) {
                            cancel();
                            return;
                        }
                        
                        if (time <= 0) {
                            teleportAndPrepare(captain, base, team);
                            captain.sendMessage("§a§lYou've spawned as captain! Lead your team to victory!");
                            cancel();
                            return;
                        }
                        captain.sendTitle("§e§lSpawning", "§7in " + time + " seconds", 0, 25, 5);
                        time--;
                    }
                }.runTaskTimer(plugin, 0L, 20L);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID memberUUID : team.getMembers()) {
                    if (team.hasCaptain() && memberUUID.equals(team.getCaptainUUID())) continue;
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        teleportAndPrepare(member, base, team);
                        if (team.hasCaptain()) {
                            member.sendMessage("§a§lSpawned with your team! Follow your captain!");
                        } else {
                            member.sendMessage("§a§lSpawned with your team!");
                        }
                    }
                }
            }
        }.runTaskLater(plugin, teamDelay * 20L);
    }
    
    private void teleportAndPrepare(Player player, Location loc, Team team) {

        if (loc.getWorld() == null) {
            plugin.getLogger().severe("Cannot teleport player " + player.getName() + " - world is null for team " + team.getName());
            player.sendMessage("§c§lError: Team base world not loaded! Contact an administrator.");
            return;
        }
        
        player.teleport(loc);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, null);
        }
        

        teamManager.applyTeamArmor(player, team);

        deadPlayers.remove(player.getUniqueId());
        
        if (config.isCaptainGlowEnabled() && team.isCaptain(player.getUniqueId())) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false
            ));
        }
    }
    
    private void startGameTasks() {
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.PLAYING) {
                    cancel();
                    return;
                }
                
                Team lastTeam = checkLastTeamStanding();
                if (lastTeam != null) {
                    endGameWithWinner(lastTeam);
                    cancel();
                    return;
                }
                
                long elapsed = (System.currentTimeMillis() - roundStartTime) / 1000;
                int roundDuration = config.getRoundDuration();
                int timeLeft = (int)(roundDuration - elapsed);
                
                if (timeLeft <= 0) {
                    endGame();
                    cancel();
                    return;
                }

                if (timeLeft <= config.getInstantDeathTime() && !instantDeathMode) {
                    instantDeathMode = true;
                    Bukkit.broadcastMessage(config.getMessage("instant-death-warning"));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§c§l⚠ INSTANT DEATH MODE ⚠", "§7No more revives!", 10, 60, 20);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                    }
                }
                
                if (timeLeft == 60 || timeLeft == 30 || timeLeft == 10) {
                    String msg = config.getMessage("round-ending-soon").replace("%time%", String.valueOf(timeLeft));
                    Bukkit.broadcastMessage(msg);
                }
                
                updateBossBar();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        captureTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.PLAYING) {
                    cancel();
                    return;
                }
                processCaptureZones();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        zoneOxygenTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.PLAYING) {
                    cancel();
                    return;
                }
                processZoneOxygen();
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        knockdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.PLAYING) {
                    cancel();
                    return;
                }
                processKnockdowns();
            }
        }.runTaskTimer(plugin, 0L, 5L);
        
        if (config.isCaptainGlowEnabled()) {
            glowTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (gameState != GameState.PLAYING) {
                        cancel();
                        return;
                    }
                    updateCaptainGlow();
                }
            }.runTaskTimer(plugin, 0L, config.getGlowUpdateInterval());
        }
    }
    
    private void processCaptureZones() {
        for (CaptureZone zone : captureZones.values()) {
            Map<Team, Integer> teamCounts = new HashMap<>();
            
            for (Player player : Bukkit.getOnlinePlayers()) {

                if (deadPlayers.contains(player.getUniqueId()) || downedPlayers.contains(player.getUniqueId())) {
                    continue;
                }
                
                Team playerTeam = teamManager.getPlayerTeam(player);
                if (playerTeam == null) continue;
                
                if (zone.isPlayerInZone(player.getLocation())) {
                    teamCounts.merge(playerTeam, 1, Integer::sum);
                }
            }

            if (teamCounts.isEmpty()) {
                if (zone.getCaptureProgress() > 0 && !zone.hasOwner()) {

                    zone.setCaptureProgress(zone.getCaptureProgress() - 2);
                }
                continue;
            }

            if (teamCounts.size() > 1) {
                zone.setCapturingTeam(null);

                zone.spawnParticles(org.bukkit.Color.fromRGB(255, 100, 0)); // Orange
                continue;
            }
            
            if (teamCounts.size() == 1) {
                Team capturingTeam = teamCounts.keySet().iterator().next();
                
                if (zone.hasOwner() && zone.getOwnerTeam().equals(capturingTeam)) {

                    capturingTeam.addPoints(config.getCapturePerSecond());
                } else if (zone.hasOwner() && !zone.getOwnerTeam().equals(capturingTeam)) {

                    zone.setCapturingTeam(capturingTeam);
                    if (zone.getCaptureProgress() > 0) {
                        zone.addCaptureProgress(-(100 / config.getCaptureTime()));
                    } else {

                        zone.setOwnerTeam(null);
                        zone.setCaptureProgress(0);
                    }
                } else {

                    zone.setCapturingTeam(capturingTeam);
                    zone.addCaptureProgress(100 / config.getCaptureTime());
                    
                    if (zone.getCaptureProgress() >= 100) {
                        zone.setOwnerTeam(capturingTeam);
                        String msg = config.getMessage("zone-captured")
                            .replace("%team%", capturingTeam.getName())
                            .replace("%zone%", zone.getName());
                        Bukkit.broadcastMessage(msg);
                        
                        if (config.isOxygenEnabled() && oxygenManager != null) {
                            oxygenManager.restoreOxygen(capturingTeam, config.getCaptureOxygenRestore());
                            String oxygenMsg = config.getMessage("oxygen-restored")
                                .replace("%amount%", String.valueOf(config.getCaptureOxygenRestore()))
                                .replace("%zone%", zone.getName());
                            for (UUID uuid : capturingTeam.getMembers()) {
                                Player p = Bukkit.getPlayer(uuid);
                                if (p != null) p.sendMessage(oxygenMsg);
                            }
                        }
                    }
                }
            }
            
            if (zone.hasOwner()) {
                Color color = zone.getOwnerTeam().getArmorColor();
                zone.spawnParticles(color);
            }
        }
    }
    
    /**
     * Process oxygen drain/refill at capture zones every tick.
     * Players standing on a point drain their team's oxygen.
     * Drain rate scales with player count (1-5x multiplier).
     * When oxygen reaches 0%, it starts refilling automatically.
     */
    private void processZoneOxygen() {
        for (CaptureZone zone : captureZones.values()) {

            refillingTeams.putIfAbsent(zone, new HashSet<>());
            Set<Team> refilling = refillingTeams.get(zone);

            Map<Team, Integer> teamCounts = new HashMap<>();
            
            for (Player player : Bukkit.getOnlinePlayers()) {

                if (deadPlayers.contains(player.getUniqueId()) || downedPlayers.contains(player.getUniqueId())) {
                    continue;
                }
                
                Team playerTeam = teamManager.getPlayerTeam(player);
                if (playerTeam == null) continue;
                
                if (zone.isPlayerInZone(player.getLocation())) {
                    teamCounts.merge(playerTeam, 1, Integer::sum);

                    zone.initTeamOxygen(playerTeam);
                }
            }

            for (Map.Entry<Team, Integer> entry : teamCounts.entrySet()) {
                Team team = entry.getKey();
                int playerCount = entry.getValue();

                if (refilling.contains(team)) {
                    zone.refillTeamOxygen(team);

                    if (zone.getTeamOxygenPercent(team) >= 100.0) {
                        refilling.remove(team);
                    }
                } else {

                    boolean reachedZero = zone.drainTeamOxygen(team, playerCount);
                    if (reachedZero) {

                        refilling.add(team);
                    }
                }
            }

            for (Team team : new HashSet<>(refilling)) {
                if (!teamCounts.containsKey(team)) {
                    zone.refillTeamOxygen(team);
                    if (zone.getTeamOxygenPercent(team) >= 100.0) {
                        refilling.remove(team);
                    }
                }
            }
        }
    }
    
    private void updateCaptainGlow() {
        for (Team team : teamManager.getAllTeams().values()) {
            UUID captainUUID = team.getCaptainUUID();
            if (captainUUID == null) continue;
            
            Player captain = Bukkit.getPlayer(captainUUID);
            if (captain != null && captain.isOnline() && !deadPlayers.contains(captain.getUniqueId())) {
                if (!captain.hasPotionEffect(PotionEffectType.GLOWING)) {
                    captain.addPotionEffect(new PotionEffect(
                        PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false
                    ));
                }
            }
        }
    }
    
    private void updateBossBar() {
        Team leadingTeam = null;
        int maxPoints = 0;
        
        for (Team team : teamManager.getAllTeams().values()) {
            if (team.getPoints() > maxPoints) {
                maxPoints = team.getPoints();
                leadingTeam = team;
            }
        }
        
        if (leadingTeam != null) {
            Color c = leadingTeam.getArmorColor();
            BarColor barColor = getBarColorFromRGB(c);
            scoreBossBar.setColor(barColor);
            scoreBossBar.setTitle("§6§l" + leadingTeam.getName() + " §7leads with §e" + maxPoints + " §7points!");
        } else {
            scoreBossBar.setTitle("§e§lNo team has scored yet!");
        }
        scoreBossBar.setProgress(1.0);
    }
    
    private BarColor getBarColorFromRGB(Color c) {
        if (c.getRed() > 200 && c.getGreen() < 100 && c.getBlue() < 100) return BarColor.RED;
        if (c.getBlue() > 200 && c.getRed() < 100 && c.getGreen() < 100) return BarColor.BLUE;
        if (c.getGreen() > 200 && c.getRed() < 100 && c.getBlue() < 100) return BarColor.GREEN;
        if (c.getRed() > 200 && c.getGreen() > 200 && c.getBlue() < 100) return BarColor.YELLOW;
        if (c.getRed() > 200 && c.getGreen() < 100 && c.getBlue() > 200) return BarColor.PURPLE;
        if (c.getRed() > 200 && c.getGreen() > 100 && c.getBlue() < 100) return BarColor.YELLOW;
        return BarColor.WHITE;
    }
    
    private Team checkLastTeamStanding() {
        Team aliveTeam = null;
        int aliveTeamCount = 0;
        
        for (Team team : teamManager.getAllTeams().values()) {
            boolean hasAliveMember = false;
            
            for (UUID uuid : team.getMembers()) {
                if (!deadPlayers.contains(uuid) && !downedPlayers.contains(uuid)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        hasAliveMember = true;
                        break;
                    }
                }
            }
            
            if (hasAliveMember) {
                aliveTeam = team;
                aliveTeamCount++;
            }
        }
        
        if (aliveTeamCount == 1) {
            return aliveTeam;
        }
        return null;
    }
    
    private void endGameWithWinner(Team winningTeam) {
        gameState = GameState.ENDED;

        if (gameTask != null) gameTask.cancel();
        if (captureTask != null) captureTask.cancel();
        if (zoneOxygenTask != null) zoneOxygenTask.cancel();
        if (glowTask != null) glowTask.cancel();
        if (knockdownTask != null) knockdownTask.cancel();
        if (countdownTask != null) countdownTask.cancel();

        refillingTeams.clear();
        
        if (config.isOxygenEnabled() && oxygenManager != null) {
            oxygenManager.stopOxygenSystem();
        }

        if (weaponSpawnerManager != null) {
            weaponSpawnerManager.stop();
        }
        
        String msg = config.getMessage("last-team-standing")
            .replace("%team%", winningTeam.getName());
        Bukkit.broadcastMessage(msg);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§6§l" + winningTeam.getName() + " WINS!", 
                "§aLast team standing!", 0, 100, 40);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.removePotionEffect(PotionEffectType.GLOWING);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.setSneaking(false);

            player.getInventory().clear();

            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);
        }
        
        for (BossBar bar : downedBossBars.values()) {
            bar.removeAll();
        }
        
        deadPlayers.clear();
        downedPlayers.clear();
        bleedoutTimers.clear();
        reviveProgress.clear();
        downedBossBars.clear();
        revivingPlayer.clear();
        lastAttacker.clear();
        
        for (CaptureZone zone : captureZones.values()) {
            zone.resetCapture();
        }

        scoreBossBar.removeAll();

        plugin.getBorderManager().resetToInitialSize();
        plugin.getBorderTimer().cancelTimer();
        plugin.getBorderShrink().stopShrinking();
        
        gameState = GameState.WAITING;
        
        new BukkitRunnable() {
            @Override
            public void run() {

                scoreBossBar.setTitle("§6§lNo game in progress");
                scoreBossBar.setColor(BarColor.YELLOW);
                scoreBossBar.setProgress(1.0);
            }
        }.runTaskLater(plugin, 200L);
    }
    
    public void endGame() {
        gameState = GameState.ENDED;

        if (gameTask != null) gameTask.cancel();
        if (captureTask != null) captureTask.cancel();
        if (zoneOxygenTask != null) zoneOxygenTask.cancel();
        if (glowTask != null) glowTask.cancel();
        if (knockdownTask != null) knockdownTask.cancel();
        if (countdownTask != null) countdownTask.cancel();

        refillingTeams.clear();
        
        if (config.isOxygenEnabled() && oxygenManager != null) {
            oxygenManager.stopOxygenSystem();
        }

        if (weaponSpawnerManager != null) {
            weaponSpawnerManager.stop();
        }
        
        Team winningTeam = null;
        int maxPoints = 0;
        
        for (Team team : teamManager.getAllTeams().values()) {
            if (team.getPoints() > maxPoints) {
                maxPoints = team.getPoints();
                winningTeam = team;
            }
        }
        
        Bukkit.broadcastMessage(config.getMessage("game-ended"));
        
        if (winningTeam != null) {
            String msg = config.getMessage("team-wins")
                .replace("%team%", winningTeam.getName());
            Bukkit.broadcastMessage(msg);
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle("§6§l" + winningTeam.getName() + " WINS!", 
                    "§7with §e" + maxPoints + " §7points!", 0, 100, 40);
            }
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.removePotionEffect(PotionEffectType.GLOWING);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.setSneaking(false);

            player.getInventory().clear();

            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);
        }
        
        for (BossBar bar : downedBossBars.values()) {
            bar.removeAll();
        }
        
        deadPlayers.clear();
        downedPlayers.clear();
        bleedoutTimers.clear();
        reviveProgress.clear();
        downedBossBars.clear();
        revivingPlayer.clear();
        lastAttacker.clear();
        
        for (CaptureZone zone : captureZones.values()) {
            zone.resetCapture();
        }

        plugin.getBorderManager().resetToInitialSize();
        plugin.getBorderTimer().cancelTimer();
        plugin.getBorderShrink().stopShrinking();

        gameState = GameState.WAITING;
        
        new BukkitRunnable() {
            @Override
            public void run() {

                scoreBossBar.setTitle("§6§lNo game in progress");
                scoreBossBar.setColor(BarColor.YELLOW);

                if (idleParticleTask != null) {
                    idleParticleTask.cancel();
                }
                startIdleParticleTask();
            }
        }.runTaskLater(plugin, 200L);
    }
    
    private void processKnockdowns() {
        for (UUID uuid : new HashSet<>(downedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                eliminatePlayer(uuid);
                continue;
            }
            
            BossBar bar = downedBossBars.get(uuid);
            int timeLeft = bleedoutTimers.getOrDefault(uuid, 0);
            
            UUID reviverUUID = revivingPlayer.get(uuid);
            if (reviverUUID != null) {
                Player reviver = Bukkit.getPlayer(reviverUUID);

                if (reviver == null || !reviver.isOnline()) {

                    revivingPlayer.remove(uuid);
                    reviveProgress.put(uuid, 0.0);
                } else if (reviver.getLocation().distance(player.getLocation()) <= 3.0) {
                    double progress = reviveProgress.getOrDefault(uuid, 0.0);
                    progress += (100.0 / (config.getReviveTime() * 4));
                    reviveProgress.put(uuid, progress);
                    
                    if (bar != null) {
                        bar.setTitle("§a§lBEING REVIVED §7- §e" + (int)progress + "%");
                        bar.setColor(BarColor.GREEN);
                    }
                    
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 1);
                    
                    if (progress >= 100) {
                        revivePlayer(player);
                        reviver.sendMessage(config.getMessage("revive-success").replace("%player%", player.getName()));
                    }
                    continue;
                } else {

                    revivingPlayer.remove(uuid);
                    reviveProgress.put(uuid, 0.0);
                }
            }
            
            if (bar != null) {
                double progress = (double) timeLeft / (config.getBleedoutTime() * 4);
                bar.setProgress(Math.max(0, Math.min(1, progress)));
                bar.setTitle("§c§lDOWNED §7- §e" + (timeLeft / 4) + "s");
                bar.setColor(BarColor.RED);
            }
            
            bleedoutTimers.put(uuid, timeLeft - 1);
            
            if (timeLeft <= 0) {
                eliminatePlayer(uuid);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (gameState != GameState.PLAYING) return;
        
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        
        String playgroundWorld = config.getPlaygroundWorld();
        if (!playgroundWorld.isEmpty() && !player.getWorld().getName().equals(playgroundWorld)) {
            return;
        }
        
        if (downedPlayers.contains(uuid)) {
            event.setCancelled(true);
            return;
        }
        
        if (deadPlayers.contains(uuid)) {
            event.setCancelled(true);
            return;
        }
        
        Team playerTeam = teamManager.getPlayerTeam(player);
        if (playerTeam == null) return;
        
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            
            if (instantDeathMode) {
                eliminatePlayerInstantly(player, event);
            } else {
                knockdownPlayer(player, event);
            }
        }
    }
    
    private void eliminatePlayerInstantly(Player player, EntityDamageEvent event) {
        UUID uuid = player.getUniqueId();
        Team playerTeam = teamManager.getPlayerTeam(player);
        
        deadPlayers.add(uuid);
        
        Player attacker = null;
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            if (damageEvent.getDamager() instanceof Player) {
                attacker = (Player) damageEvent.getDamager();
            }
        }
        
        if (attacker != null) {
            Team attackerTeam = teamManager.getPlayerTeam(attacker);
            if (attackerTeam != null && !attackerTeam.equals(playerTeam)) {
                int points = config.getKillReward();
                
                if (playerTeam.isCaptain(uuid)) {
                    points += config.getCaptainKillBonus();
                }
                
                attackerTeam.addPoints(points);
                
                String msg = config.getMessage("kill-reward")
                    .replace("%points%", String.valueOf(points))
                    .replace("%victim%", player.getName());
                attacker.sendMessage(msg);
            }
        }
        
        player.setGameMode(GameMode.SPECTATOR);
        player.sendTitle("§4§lELIMINATED", "§7You've been eliminated!", 0, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1f);
        
        Bukkit.broadcastMessage(config.getMessage("player-eliminated")
            .replace("%player%", player.getName()));
            
        if (playerTeam.isCaptain(uuid)) {
            for (UUID memberUUID : playerTeam.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(config.getMessage("captain-eliminated"));
                }
            }
        }
    }
    
    private void knockdownPlayer(Player player, EntityDamageEvent event) {
        UUID uuid = player.getUniqueId();
        Team playerTeam = teamManager.getPlayerTeam(player);
        
        downedPlayers.add(uuid);
        bleedoutTimers.put(uuid, config.getBleedoutTime() * 4);
        reviveProgress.put(uuid, 0.0);
        
        Player attacker = null;
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            if (damageEvent.getDamager() instanceof Player) {
                attacker = (Player) damageEvent.getDamager();
                lastAttacker.put(uuid, attacker.getUniqueId());
            }
        }
        
        player.setHealth(player.getMaxHealth());
        player.setSneaking(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false));
        
        BossBar downedBar = Bukkit.createBossBar(
            "§c§lDOWNED §7- §e" + config.getBleedoutTime() + "s",
            BarColor.RED,
            BarStyle.SOLID
        );
        downedBar.addPlayer(player);
        downedBossBars.put(uuid, downedBar);
        
        player.sendTitle("§c§lYOU'RE DOWN!", "§7Hold on! A teammate can revive you!", 0, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);
        
        if (attacker != null) {
            Team attackerTeam = teamManager.getPlayerTeam(attacker);
            if (attackerTeam != null && !attackerTeam.equals(playerTeam)) {
                attacker.sendMessage(config.getMessage("player-knocked").replace("%player%", player.getName()));
            }
        }
        
        for (UUID memberUUID : playerTeam.getMembers()) {
            if (memberUUID.equals(uuid)) continue;
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(config.getMessage("teammate-down").replace("%player%", player.getName()));
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (gameState != GameState.PLAYING) return;
        if (!(event.getRightClicked() instanceof Player)) return;
        
        Player reviver = event.getPlayer();
        Player downed = (Player) event.getRightClicked();
        
        if (!downedPlayers.contains(downed.getUniqueId())) return;
        
        Team reviverTeam = teamManager.getPlayerTeam(reviver);
        Team downedTeam = teamManager.getPlayerTeam(downed);
        
        if (reviverTeam == null || downedTeam == null) return;
        if (!reviverTeam.equals(downedTeam)) return;
        
        if (downedPlayers.contains(reviver.getUniqueId())) {
            reviver.sendMessage("§c§lYou can't revive while downed!");
            return;
        }
        
        revivingPlayer.put(downed.getUniqueId(), reviver.getUniqueId());
        reviver.sendMessage(config.getMessage("reviving-player").replace("%player%", downed.getName()));
        downed.sendMessage(config.getMessage("being-revived").replace("%player%", reviver.getName()));
    }
    
    private void revivePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        
        downedPlayers.remove(uuid);
        bleedoutTimers.remove(uuid);
        reviveProgress.remove(uuid);
        revivingPlayer.remove(uuid);
        lastAttacker.remove(uuid);
        
        BossBar bar = downedBossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        
        player.setSneaking(false);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setHealth(player.getMaxHealth() * 0.5);
        
        player.sendTitle("§a§lREVIVED!", "§7You're back in the fight!", 0, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }
    
    private void eliminatePlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        Team playerTeam = null;

        if (player != null) {
            playerTeam = teamManager.getPlayerTeam(player);
        } else {

            for (Team team : teamManager.getAllTeams().values()) {
                if (team.isMember(uuid)) {
                    playerTeam = team;
                    break;
                }
            }
        }
        
        UUID attackerUUID = lastAttacker.remove(uuid);
        if (attackerUUID != null && playerTeam != null) {
            Player attacker = Bukkit.getPlayer(attackerUUID);
            if (attacker != null) {
                Team attackerTeam = teamManager.getPlayerTeam(attacker);
                if (attackerTeam != null && !attackerTeam.equals(playerTeam)) {
                    int points = config.getKillReward();
                    
                    if (playerTeam.isCaptain(uuid)) {
                        points += config.getCaptainKillBonus();
                    }
                    
                    attackerTeam.addPoints(points);
                    
                    String msg = config.getMessage("kill-reward")
                        .replace("%points%", String.valueOf(points))
                        .replace("%victim%", player != null ? player.getName() : "Unknown");
                    attacker.sendMessage(msg);
                }
            }
        }
        
        downedPlayers.remove(uuid);
        bleedoutTimers.remove(uuid);
        reviveProgress.remove(uuid);
        revivingPlayer.remove(uuid);
        deadPlayers.add(uuid);
        
        BossBar bar = downedBossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        
        if (player != null && player.isOnline()) {
            player.setSneaking(false);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendTitle("§4§lELIMINATED", "§7You've bled out!", 0, 60, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1f);
        }
        
        if (playerTeam != null) {
            Bukkit.broadcastMessage(config.getMessage("player-eliminated")
                .replace("%player%", player != null ? player.getName() : "Unknown"));
                
            if (playerTeam.isCaptain(uuid)) {
                for (UUID memberUUID : playerTeam.getMembers()) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.sendMessage(config.getMessage("captain-eliminated"));
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameState != GameState.PLAYING) return;
        
        Player player = event.getPlayer();
        if (!downedPlayers.contains(player.getUniqueId())) return;
        
        if (!player.isSneaking()) {
            player.setSneaking(true);
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        
        double distance = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));
        if (distance > 0.1) {
            event.setTo(from.setDirection(to.getDirection()));
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (downedPlayers.contains(uuid)) {
            eliminatePlayer(uuid);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (gameState == GameState.PLAYING || gameState == GameState.COUNTDOWN) {
            scoreBossBar.addPlayer(event.getPlayer());
        }
    }
    
    public void addCaptureZone(String name, Location center, double radius) {
        CaptureZone zone = new CaptureZone(name, center, radius);
        captureZones.put(name.toLowerCase(), zone);
        saveZone(zone);
    }
    
    private void saveZone(CaptureZone zone) {
        String path = "zones." + zone.getName().toLowerCase();
        config.set(path + ".world", zone.getCenter().getWorld().getName());
        config.set(path + ".x", zone.getCenter().getX());
        config.set(path + ".y", zone.getCenter().getY());
        config.set(path + ".z", zone.getCenter().getZ());
        config.set(path + ".radius", zone.getRadius());
    }
    
    private void loadZones() {
        plugin.getLogger().info("Loading capture zones from config...");

        org.bukkit.configuration.ConfigurationSection dataSection = config.getConfigurationSection("zones.data");
        if (dataSection != null) {
            plugin.getLogger().info("Found zones.data section with keys: " + dataSection.getKeys(false));
            loadZonesFromSection(dataSection, "zones.data");
            return;
        } else {
            plugin.getLogger().info("No zones.data section found, trying zones...");
        }

        org.bukkit.configuration.ConfigurationSection zonesSection = config.getConfigurationSection("zones");
        if (zonesSection != null) {
            plugin.getLogger().info("Found zones section with keys: " + zonesSection.getKeys(false));
            loadZonesFromSection(zonesSection, "zones");
        } else {
            plugin.getLogger().info("No zones section found in config!");
        }
    }
    
    private void loadZonesFromSection(org.bukkit.configuration.ConfigurationSection section, String basePath) {
        for (String zoneName : section.getKeys(false)) {
            String path = basePath + "." + zoneName;

            String worldName = config.getString(path + ".center.world", "");
            double x, y, z, radius;
            
            if (!worldName.isEmpty()) {

                x = config.getDouble(path + ".center.x", 0.0);
                y = config.getDouble(path + ".center.y", 0.0);
                z = config.getDouble(path + ".center.z", 0.0);
                radius = config.getDouble(path + ".radius", 5.0);
            } else {

                worldName = config.getString(path + ".world", "");
                x = config.getDouble(path + ".x", 0.0);
                y = config.getDouble(path + ".y", 0.0);
                z = config.getDouble(path + ".z", 0.0);
                radius = config.getDouble(path + ".radius", 5.0);
            }
            
            if (worldName.isEmpty()) {
                plugin.getLogger().warning("Zone '" + zoneName + "' has no world configured!");
                continue;
            }
            
            plugin.getLogger().info("Attempting to load zone '" + zoneName + "' in world '" + worldName + "'...");
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().info("World '" + worldName + "' not loaded, attempting to create/load...");
                try {
                    world = org.bukkit.WorldCreator.name(worldName).createWorld();
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to create/load world: " + e.getMessage());
                }
            }
            
            if (world != null) {
                String displayName = config.getString(path + ".name", zoneName);
                Location center = new Location(world, x, y, z);
                CaptureZone zone = new CaptureZone(displayName, center, radius);
                captureZones.put(zoneName.toLowerCase(), zone);
                plugin.getLogger().info("✓ Loaded capture zone: " + displayName + " at (" + (int)x + ", " + (int)y + ", " + (int)z + ") in " + worldName);
            } else {
                plugin.getLogger().warning("✗ Could not load zone '" + zoneName + "' - world '" + worldName + "' not found! Adding to pending list.");
                pendingZoneWorlds.add(worldName);
            }
        }
        
        zonesLoaded = captureZones.size() > 0;
        plugin.getLogger().info("Zone loading complete: " + captureZones.size() + " zones loaded.");
    }
    
    public boolean removeCaptureZone(String name) {
        CaptureZone zone = captureZones.remove(name.toLowerCase());
        if (zone != null) {
            zone.removeHologram();

            config.set("zones." + name.toLowerCase(), null);
            return true;
        }
        return false;
    }
    
    public CaptureZone getCaptureZone(String name) {
        return captureZones.get(name.toLowerCase());
    }
    
    public Map<String, CaptureZone> getAllCaptureZones() {
        return new HashMap<>(captureZones);
    }
    
    public boolean isPlayerDead(UUID uuid) {
        return deadPlayers.contains(uuid);
    }
    
    public boolean isPlayerDowned(UUID uuid) {
        return downedPlayers.contains(uuid);
    }
    
    /**
     * Cleans up any orphaned hologram ArmorStands from previous server runs
     * and recreates fresh holograms for all zones
     */
    public void cleanupHolograms() {
        String playgroundWorld = config.getPlaygroundWorld();
        if (playgroundWorld == null || playgroundWorld.isEmpty()) return;
        
        World world = Bukkit.getWorld(playgroundWorld);
        if (world == null) return;
        
        plugin.getLogger().info("Cleaning up orphaned hologram entities...");

        int removed = 0;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.ArmorStand) {
                org.bukkit.entity.ArmorStand stand = (org.bukkit.entity.ArmorStand) entity;
                if (!stand.isVisible() && stand.isMarker()) {

                    for (CaptureZone zone : captureZones.values()) {
                        if (zone.getCenter().getWorld().equals(world)) {
                            double distance = stand.getLocation().distance(zone.getCenter());
                            if (distance < 5) {
                                stand.remove();
                                removed++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        plugin.getLogger().info("Removed " + removed + " orphaned hologram entities.");

        for (CaptureZone zone : captureZones.values()) {
            zone.recreateHolograms();
        }
        
        plugin.getLogger().info("Recreated holograms for " + captureZones.size() + " capture zones.");
    }
}
