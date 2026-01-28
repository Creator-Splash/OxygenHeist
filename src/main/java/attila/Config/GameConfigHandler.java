package attila.Config;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import attila.OxygenMain;

public class GameConfigHandler {
    
    private final OxygenMain plugin;
    private File configFile;
    private FileConfiguration config;
    
    public GameConfigHandler(OxygenMain plugin) {
        this.plugin = plugin;
        setupConfig();
    }
    
    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "game-config.yml");
        boolean isNewFile = !configFile.exists();
        
        if (isNewFile) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Couldn't create game-config.yml.");
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        setDefaults(isNewFile);
    }
    
    private void setDefaults(boolean isNewFile) {
        config.addDefault("game.bleedout-time", 30);
        config.addDefault("game.revive-time", 5);
        config.addDefault("game.captain-spawn-delay", 3);
        config.addDefault("game.team-spawn-delay", 5);
        config.addDefault("game.countdown-time", 10);
        config.addDefault("game.min-teams", 2);
        config.addDefault("game.round-duration", 600);
        config.addDefault("game.instant-death-time", 120);
        config.addDefault("game.playground-world", "");
        config.addDefault("game.teleport-on-join", true);
        config.addDefault("game.join-spawn-world", "");
        config.addDefault("game.join-spawn-x", 0.0);
        config.addDefault("game.join-spawn-y", 64.0);
        config.addDefault("game.join-spawn-z", 0.0);
        
        config.addDefault("border.shrink-delay", 60);
        config.addDefault("border.shrink-duration", 300);
        config.addDefault("border.shrink-size-percent", 20.0);
        
        config.addDefault("oxygen.enabled", true);
        config.addDefault("oxygen.max-oxygen", 300);
        config.addDefault("oxygen.drain-interval", 20);
        config.addDefault("oxygen.drain-amount", 5);
        config.addDefault("oxygen.capture-restore", 50);
        config.addDefault("oxygen.suffocation-damage", 2.0);
        
        config.addDefault("points.kill-reward", 10);
        config.addDefault("points.capture-per-second", 1);
        config.addDefault("points.captain-kill-bonus", 5);
        
        config.addDefault("capture.radius", 5);
        config.addDefault("capture.time-to-capture", 10);
        config.addDefault("capture.particle-interval", 10);
        
        config.addDefault("captain.glow-enabled", true);
        config.addDefault("captain.glow-update-interval", 20);

        config.addDefault("weapon-spawner.enabled", true);
        config.addDefault("weapon-spawner.spawn-interval", 60); // seconds between spawns
        config.addDefault("weapon-spawner.initial-spawn-count", 5); // how many spawn at game start
        config.addDefault("weapon-spawner.max-holograms", 15); // max simultaneous holograms
        config.addDefault("weapon-spawner.pickup-radius", 1.5); // blocks
        config.addDefault("weapon-spawner.pickup-cooldown", 3); // seconds cooldown per player



        if (isNewFile) {
            config.set("weapon-spawner.weapons.reef_harpoon.display-name", "Reef Harpoon Gun");
            config.set("weapon-spawner.weapons.reef_harpoon.command", "give %player% golden_horse_armor[custom_model_data={floats:[1001]}] 1");
            config.set("weapon-spawner.weapons.spike_shooter.display-name", "Spike Shooter");
            config.set("weapon-spawner.weapons.spike_shooter.command", "give %player% golden_horse_armor[custom_model_data={floats:[2001]}] 1");
            config.set("weapon-spawner.weapons.venom_spitter.display-name", "Venom Spitter");
            config.set("weapon-spawner.weapons.venom_spitter.command", "give %player% golden_horse_armor[custom_model_data={floats:[3001]}] 1");
            config.set("weapon-spawner.weapons.needle_rifle.display-name", "Needle Rifle");
            config.set("weapon-spawner.weapons.needle_rifle.command", "give %player% golden_horse_armor[custom_model_data={floats:[4001]}] 1");
            config.set("weapon-spawner.weapons.ammo_pack.display-name", "Munición");
            config.set("weapon-spawner.weapons.ammo_pack.command", "give %player% firework_star[custom_model_data={floats:[1000]}] 16");
            config.set("weapon-spawner.weapons.health_pack.display-name", "Kit de Salud");
            config.set("weapon-spawner.weapons.health_pack.command", "effect give %player% instant_health 1 1");
        }
        
        config.addDefault("messages.game-starting", "&a&lGame starting in %time% seconds!");
        config.addDefault("messages.game-started", "&a&lLET THE GAMES BEGIN! &7May the odds be ever in your favor.");
        config.addDefault("messages.game-ended", "&c&lGAME OVER! &7Time to go home, folks.");
        config.addDefault("messages.team-wins", "&6&l%team% &a&lWINS! &7Congratulations... I guess.");
        config.addDefault("messages.instant-death-warning", "&c&l⚠ INSTANT DEATH MODE! &7No more revives!");
        config.addDefault("messages.round-ending-soon", "&e&lRound ending in %time% seconds!");
        config.addDefault("messages.playground-set", "&a&lPlayground world set! &7Game will only work in %world%.");
        config.addDefault("messages.playground-not-set", "&c&lNo playground world set! &7Set one with /game setworld.");
        config.addDefault("messages.oxygen-low", "&c&lOxygen low! &7Capture a zone to restore!");
        config.addDefault("messages.oxygen-restored", "&a&l+%amount% Oxygen! &7Your team captured %zone%!");
        config.addDefault("messages.last-team-standing", "&6&l%team% &a&lis the last team standing!");
        config.addDefault("messages.player-knocked", "&c%player% &7has been knocked down!");
        config.addDefault("messages.teammate-down", "&e&l%player% &7is down! Go revive them!");
        config.addDefault("messages.player-eliminated", "&4&l%player% &7has been eliminated!");
        config.addDefault("messages.captain-eliminated", "&c&lYour captain has been eliminated!");
        config.addDefault("messages.reviving-player", "&a&lReviving &e%player%&a&l... Stay close!");
        config.addDefault("messages.being-revived", "&a&l%player% &7is reviving you! Hold on!");
        config.addDefault("messages.revive-success", "&a&lYou revived &e%player%&a&l!");
        config.addDefault("messages.kill-reward", "&a+%points% points &7for knocking &c%victim%");
        config.addDefault("messages.zone-captured", "&a&l%team% &7captured &e%zone%!");
        config.addDefault("messages.zone-contested", "&e&lZone contested! &7Multiple teams detected.");
        config.addDefault("messages.captain-spawning", "&e&lCaptain spawning in %time% seconds...");
        config.addDefault("messages.team-spawning", "&a&lTeam spawning with captain!");
        config.addDefault("messages.base-set", "&a&lBase location set! &7Your team will spawn here.");
        config.addDefault("messages.zone-created", "&a&lCapture zone created! &7Radius: %radius% blocks.");
        config.addDefault("messages.zone-deleted", "&c&lCapture zone deleted! &7It's gone forever.");
        config.addDefault("messages.not-enough-teams", "&c&lNot enough teams! &7Need at least %min% teams.");
        config.addDefault("messages.no-bases-set", "&c&lSome teams have no base! &7Set bases first.");
        config.addDefault("messages.border-not-set", "&c&lBorder not configured! &7Set up the arena first.");
        config.addDefault("messages.game-already-running", "&c&lGame already in progress! &7Patience, young padawan.");
        config.addDefault("messages.no-game-running", "&c&lNo game running! &7Start one first.");
        
        config.addDefault("permissions.game-start", "oxygenheist.game.start");
        config.addDefault("permissions.game-stop", "oxygenheist.game.stop");
        config.addDefault("permissions.game-setworld", "oxygenheist.game.setworld");
        config.addDefault("permissions.game-setbase", "oxygenheist.game.setbase");
        config.addDefault("permissions.game-setzone", "oxygenheist.game.setzone");
        config.addDefault("permissions.game-delzone", "oxygenheist.game.delzone");
        
        config.options().copyDefaults(true);
        saveConfig();
    }
    
    public void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Couldn't save game-config.yml: " + e.getMessage());
        }
    }
    
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public int getBleedoutTime() {
        return config.getInt("game.bleedout-time", 30);
    }
    
    public int getReviveTime() {
        return config.getInt("game.revive-time", 5);
    }
    
    public int getCaptainSpawnDelay() {
        return config.getInt("game.captain-spawn-delay", 3);
    }
    
    public int getTeamSpawnDelay() {
        return config.getInt("game.team-spawn-delay", 5);
    }
    
    public int getCountdownTime() {
        return config.getInt("game.countdown-time", 10);
    }
    
    public int getMinTeams() {
        return config.getInt("game.min-teams", 2);
    }
    
    public int getKillReward() {
        return config.getInt("points.kill-reward", 10);
    }
    
    public int getCapturePerSecond() {
        return config.getInt("points.capture-per-second", 1);
    }
    
    public int getCaptainKillBonus() {
        return config.getInt("points.captain-kill-bonus", 5);
    }
    
    public int getCaptureRadius() {
        return config.getInt("capture.radius", 5);
    }
    
    public int getCaptureTime() {
        return config.getInt("capture.time-to-capture", 10);
    }
    
    public int getParticleInterval() {
        return config.getInt("capture.particle-interval", 10);
    }
    
    public boolean isCaptainGlowEnabled() {
        return config.getBoolean("captain.glow-enabled", true);
    }
    
    public int getGlowUpdateInterval() {
        return config.getInt("captain.glow-update-interval", 20);
    }
    
    public int getRoundDuration() {
        return config.getInt("game.round-duration", 600);
    }
    
    public int getInstantDeathTime() {
        return config.getInt("game.instant-death-time", 120);
    }
    
    public int getBorderShrinkDelay() {
        return config.getInt("border.shrink-delay", 60);
    }
    
    public int getBorderShrinkDuration() {
        return config.getInt("border.shrink-duration", 300);
    }
    
    public double getBorderShrinkSizePercent() {
        return config.getDouble("border.shrink-size-percent", 20.0);
    }
    
    public String getPlaygroundWorld() {
        return config.getString("game.playground-world", "");
    }
    
    public void setPlaygroundWorld(String worldName) {
        config.set("game.playground-world", worldName);
        saveConfig();
    }
    
    public boolean isTeleportOnJoinEnabled() {
        return config.getBoolean("game.teleport-on-join", true);
    }
    
    public org.bukkit.Location getJoinSpawnLocation() {
        String worldName = config.getString("game.join-spawn-world", "");
        if (worldName.isEmpty()) {
            return null;
        }
        
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        
        double x = config.getDouble("game.join-spawn-x", 0.0);
        double y = config.getDouble("game.join-spawn-y", 64.0);
        double z = config.getDouble("game.join-spawn-z", 0.0);
        
        return new org.bukkit.Location(world, x, y, z);
    }
    
    public void setJoinSpawnLocation(org.bukkit.Location location) {
        if (location != null && location.getWorld() != null) {
            config.set("game.join-spawn-world", location.getWorld().getName());
            config.set("game.join-spawn-x", location.getX());
            config.set("game.join-spawn-y", location.getY());
            config.set("game.join-spawn-z", location.getZ());
            saveConfig();
        }
    }
    
    public boolean isOxygenEnabled() {
        return config.getBoolean("oxygen.enabled", true);
    }
    
    public int getMaxOxygen() {
        return config.getInt("oxygen.max-oxygen", 300);
    }
    
    public int getOxygenDrainInterval() {
        return config.getInt("oxygen.drain-interval", 20);
    }
    
    public int getOxygenDrainAmount() {
        return config.getInt("oxygen.drain-amount", 5);
    }
    
    public int getCaptureOxygenRestore() {
        return config.getInt("oxygen.capture-restore", 50);
    }
    
    public double getSuffocationDamage() {
        return config.getDouble("oxygen.suffocation-damage", 2.0);
    }

    
    public boolean isWeaponSpawnerEnabled() {
        return config.getBoolean("weapon-spawner.enabled", true);
    }
    
    public int getWeaponSpawnInterval() {
        return config.getInt("weapon-spawner.spawn-interval", 60);
    }
    
    public int getWeaponInitialSpawnCount() {
        return config.getInt("weapon-spawner.initial-spawn-count", 5);
    }
    
    public int getWeaponMaxHolograms() {
        return config.getInt("weapon-spawner.max-holograms", 15);
    }
    
    public double getWeaponPickupRadius() {
        return config.getDouble("weapon-spawner.pickup-radius", 1.5);
    }
    
    public int getWeaponPickupCooldown() {
        return config.getInt("weapon-spawner.pickup-cooldown", 3);
    }
    
    /**
     * Gets all configured weapon IDs.
     */
    public java.util.List<String> getWeaponIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        org.bukkit.configuration.ConfigurationSection weapons = config.getConfigurationSection("weapon-spawner.weapons");
        if (weapons != null) {
            ids.addAll(weapons.getKeys(false));
        }
        return ids;
    }
    
    /**
     * Gets the display name for a weapon.
     */
    public String getWeaponDisplayName(String weaponId) {
        return config.getString("weapon-spawner.weapons." + weaponId + ".display-name", weaponId);
    }
    
    /**
     * Gets the console command for a weapon.
     * Use %player% as placeholder for player name.
     */
    public String getWeaponCommand(String weaponId) {
        return config.getString("weapon-spawner.weapons." + weaponId + ".command", "");
    }
    
    /**
     * Sets the display name for a weapon.
     */
    public void setWeaponDisplayName(String weaponId, String displayName) {
        config.set("weapon-spawner.weapons." + weaponId + ".display-name", displayName);
        saveConfig();
    }
    
    /**
     * Sets the console command for a weapon.
     */
    public void setWeaponCommand(String weaponId, String command) {
        config.set("weapon-spawner.weapons." + weaponId + ".command", command);
        saveConfig();
    }
    
    /**
     * Adds a new weapon to the configuration.
     */
    public void addWeapon(String weaponId, String displayName, String command) {
        config.set("weapon-spawner.weapons." + weaponId + ".display-name", displayName);
        config.set("weapon-spawner.weapons." + weaponId + ".command", command);
        saveConfig();
    }
    
    /**
     * Removes a weapon from the configuration.
     */
    public void removeWeapon(String weaponId) {
        config.set("weapon-spawner.weapons." + weaponId, null);
        saveConfig();
    }
    
    
    public String getPermission(String key) {
        return config.getString("permissions." + key, "oxygenheist.game." + key);
    }
    
    public String getMessage(String key) {
        String msg = config.getString("messages." + key, "");
        return msg != null ? msg.replace("&", "§") : "";
    }
    
    public void set(String path, Object value) {
        config.set(path, value);
        saveConfig();
    }
    
    public Object get(String path, Object defaultValue) {
        Object result = config.get(path);
        return result != null ? result : defaultValue;
    }
    
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    public org.bukkit.configuration.ConfigurationSection getConfigurationSection(String path) {
        return config.getConfigurationSection(path);
    }
}
