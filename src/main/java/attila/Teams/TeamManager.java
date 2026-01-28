package attila.Teams;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import attila.OxygenMain;

public class TeamManager implements Listener {
    
    private final OxygenMain plugin;
    private final Map<String, Team> teams;
    private final Map<UUID, String> playerTeams;
    private final Map<UUID, BossBar> waitingBossBars;
    
    public TeamManager(OxygenMain plugin) {
        this.plugin = plugin;
        this.teams = new HashMap<>();
        this.playerTeams = new HashMap<>();
        this.waitingBossBars = new HashMap<>();

        loadTeams();
    }
    
    /**
     * Loads all saved teams from the configuration file.
     */
    public void loadTeams() {
        java.util.List<String> savedTeams = plugin.getTeamConfig().getSavedTeamNames();
        
        if (savedTeams == null || savedTeams.isEmpty()) {
            plugin.getLogger().info("No saved teams found to load.");
            return;
        }
        
        int loaded = 0;
        for (String teamName : savedTeams) {
            org.bukkit.configuration.ConfigurationSection teamSection = 
                plugin.getTeamConfig().getTeamDataSection(teamName);
            
            if (teamSection == null) {
                plugin.getLogger().warning("Team '" + teamName + "' is in saved-teams list but has no data section!");
                continue;
            }

            String name = teamSection.getString("name", teamName);

            int red = teamSection.getInt("color.red", 255);
            int green = teamSection.getInt("color.green", 255);
            int blue = teamSection.getInt("color.blue", 255);
            Color color = Color.fromRGB(red, green, blue);

            int points = teamSection.getInt("points", 0);

            boolean friendlyFire = teamSection.getBoolean("friendly-fire", false);

            UUID captainUUID = null;
            String captainStr = teamSection.getString("captain");
            if (captainStr != null && !captainStr.isEmpty()) {
                try {
                    captainUUID = UUID.fromString(captainStr);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid captain UUID in team " + teamName + ": " + captainStr);
                }
            }

            java.util.List<String> memberStrings = teamSection.getStringList("members");
            java.util.List<UUID> members = new java.util.ArrayList<>();
            
            for (String uuidStr : memberStrings) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    members.add(uuid);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in team " + teamName + ": " + uuidStr);
                }
            }

            if (captainUUID == null && !members.isEmpty()) {
                captainUUID = members.get(0);
                plugin.getLogger().warning("Team '" + teamName + "' had no captain! Auto-assigned first member as captain.");
            }

            Team team = new Team(name, captainUUID);
            team.setArmorColor(color);
            team.setPoints(points);
            team.setFriendlyFire(friendlyFire);

            for (UUID memberUUID : members) {
                if (!memberUUID.equals(captainUUID)) {
                    team.addMember(memberUUID);
                }
                playerTeams.put(memberUUID, teamName.toLowerCase());
            }

            org.bukkit.configuration.ConfigurationSection baseSection = teamSection.getConfigurationSection("base");
            if (baseSection != null) {
                String worldName = baseSection.getString("world");
                double x = baseSection.getDouble("x");
                double y = baseSection.getDouble("y");
                double z = baseSection.getDouble("z");
                float yaw = (float) baseSection.getDouble("yaw", 0.0);
                float pitch = (float) baseSection.getDouble("pitch", 0.0);
                
                org.bukkit.World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    try {
                        org.bukkit.WorldCreator creator = new org.bukkit.WorldCreator(worldName);
                        world = creator.createWorld();
                        if (world != null) {
                            plugin.getLogger().info("Loaded world '" + worldName + "' for team '" + teamName + "' base.");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not create/load world '" + worldName + "' for team '" + teamName + "': " + e.getMessage());
                    }
                }
                
                if (world != null) {
                    Location base = new Location(world, x, y, z, yaw, pitch);
                    team.setBaseLocation(base);
                    plugin.getLogger().info("Loaded base for team '" + teamName + "' at " + worldName + " (" + (int)x + ", " + (int)y + ", " + (int)z + ")");
                } else {
                    plugin.getLogger().severe("FAILED to load base for team '" + teamName + "' - world '" + worldName + "' not found! Team will not have a base.");
                }
            }
            
            teams.put(teamName.toLowerCase(), team);
            loaded++;
        }
        
        plugin.getLogger().info("Loaded " + loaded + "/" + savedTeams.size() + " teams from configuration.");
    }
    
    public boolean createTeam(String teamName, Player captain) {
        if (teams.containsKey(teamName.toLowerCase())) {
            return false;
        }
        
        UUID captainUUID = captain != null ? captain.getUniqueId() : null;
        Team team = new Team(teamName, captainUUID);
        teams.put(teamName.toLowerCase(), team);
        
        if (captain != null) {
            playerTeams.put(captain.getUniqueId(), teamName.toLowerCase());

            applyTeamArmor(captain, team);

            removeWaitingBossBar(captain);
            if (captain.getGameMode() == GameMode.SPECTATOR || captain.getGameMode() == GameMode.CREATIVE) {
                captain.setGameMode(GameMode.ADVENTURE);
            }
        }

        plugin.getTeamConfig().saveTeamName(teamName);

        team.saveToConfig(plugin);
        
        return true;
    }
    
    public boolean deleteTeam(String teamName) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null) {
            return false;
        }
        
        for (UUID member : team.getMembers()) {
            playerTeams.remove(member);
        }
        
        teams.remove(teamName.toLowerCase());
        plugin.getTeamConfig().removeTeamName(teamName);

        plugin.getTeamConfig().getConfig().set("data." + teamName.toLowerCase(), null);
        plugin.getTeamConfig().saveConfig();
        
        return true;
    }
    
    public Team getTeam(String teamName) {
        return teams.get(teamName.toLowerCase());
    }
    
    public Team getPlayerTeam(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());
        if (teamName == null) {
            return null;
        }
        return teams.get(teamName);
    }
    
    public boolean addPlayerToTeam(Player player, Team team) {
        if (playerTeams.containsKey(player.getUniqueId())) {
            return false;
        }
        
        if (team.addMember(player.getUniqueId())) {
            playerTeams.put(player.getUniqueId(), team.getName().toLowerCase());
            applyTeamArmor(player, team);

            removeWaitingBossBar(player);
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
                player.setGameMode(GameMode.ADVENTURE);
            }

            if (team.hasBase()) {
                Location base = team.getBaseLocation();
                if (base != null) {
                    player.teleport(base);
                }
            }

            team.saveToConfig(plugin);
            
            return true;
        }
        return false;
    }
    
    public boolean removePlayerFromTeam(Player player, Team team) {
        if (!team.isCaptain(player.getUniqueId())) {
            if (team.removeMember(player.getUniqueId())) {
                playerTeams.remove(player.getUniqueId());
                removeTeamArmor(player);

                String playgroundWorld = plugin.getGameConfig().getPlaygroundWorld();
                if (!playgroundWorld.isEmpty() && player.getWorld().getName().equals(playgroundWorld)) {
                    if (!player.isOp() && !player.hasPermission("oxygenheist.bypass.teamcheck")) {
                        player.setGameMode(GameMode.SPECTATOR);
                        showWaitingBossBar(player);
                    }
                }

                team.saveToConfig(plugin);
                
                return true;
            }
        }
        return false;
    }
    
    public boolean setCaptain(Team team, Player newCaptain) {
        UUID oldCaptain = team.getCaptainUUID();
        team.setCaptain(newCaptain.getUniqueId());

        if (!team.isMember(newCaptain.getUniqueId())) {
            playerTeams.put(newCaptain.getUniqueId(), team.getName().toLowerCase());
            applyTeamArmor(newCaptain, team);
        }

        removeWaitingBossBar(newCaptain);
        if (newCaptain.getGameMode() == GameMode.SPECTATOR || newCaptain.getGameMode() == GameMode.CREATIVE) {
            newCaptain.setGameMode(GameMode.ADVENTURE);
        }

        team.saveToConfig(plugin);
        
        return true;
    }
    
    public void setTeamColor(Team team, Color color) {
        team.setArmorColor(color);
        
        for (UUID memberUUID : team.getMembers()) {
            Player player = Bukkit.getPlayer(memberUUID);
            if (player != null && player.isOnline()) {
                applyTeamArmor(player, team);
            }
        }

        team.saveToConfig(plugin);
    }
    
    public void applyTeamArmor(Player player, Team team) {
        Color color = team.getArmorColor();
        
        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, color);
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, color);
        ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, color);
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, color);
        
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }
    
    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public void removeTeamArmor(Player player) {
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
    }
    
    public Map<String, Team> getAllTeams() {
        return new HashMap<>(teams);
    }
    
    public boolean areTeammates(Player player1, Player player2) {
        Team team1 = getPlayerTeam(player1);
        Team team2 = getPlayerTeam(player2);
        
        return team1 != null && team2 != null && team1.equals(team2);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Team team = getPlayerTeam(player);
        
        String playgroundWorld = plugin.getGameConfig().getPlaygroundWorld();

        if (plugin.getGameConfig().isTeleportOnJoinEnabled()) {
            Location spawnLocation = plugin.getGameConfig().getJoinSpawnLocation();
            if (spawnLocation != null) {
                player.teleport(spawnLocation);
            }
        }
        
        if (team != null) {
            applyTeamArmor(player, team);
            removeWaitingBossBar(player);
        } else {


            setSpectatorModeMultipleTimes(player, 10);
            showWaitingBossBar(player);
        }
    }
    
    private void setSpectatorModeMultipleTimes(Player player, int attempts) {
        if (attempts <= 0) {
            return;
        }
        
        if (player.getGameMode() != GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SPECTATOR);
        }
        
        if (attempts > 1) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (player.isOnline() && getPlayerTeam(player) == null) {
                    setSpectatorModeMultipleTimes(player, attempts - 1);
                }
            }, 5L);
        }
    }
    
    private void showWaitingBossBar(Player player) {
        BossBar bar = waitingBossBars.get(player.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar(
                "§e§lWAITING TO BE ASSIGNED, Que la virgen de guadalupe te bendiga",
                BarColor.YELLOW,
                BarStyle.SOLID
            );
            waitingBossBars.put(player.getUniqueId(), bar);
        }
        bar.addPlayer(player);
        bar.setVisible(true);
    }
    
    private void removeWaitingBossBar(Player player) {
        BossBar bar = waitingBossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
            bar.setVisible(false);
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        Team victimTeam = getPlayerTeam(victim);
        Team attackerTeam = getPlayerTeam(attacker);
        
        if (victimTeam != null && attackerTeam != null && victimTeam.equals(attackerTeam)) {
            if (!victimTeam.isFriendlyFireEnabled()) {
                event.setCancelled(true);
                attacker.sendMessage("§c§lWhoops! §fThat's your teammate, genius. Friendly fire is disabled.");
            }
        }
    }
}
