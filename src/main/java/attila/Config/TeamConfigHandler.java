package attila.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import attila.OxygenMain;

public class TeamConfigHandler {
    
    private final OxygenMain plugin;
    private File configFile;
    private FileConfiguration config;
    
    public TeamConfigHandler(OxygenMain plugin) {
        this.plugin = plugin;
        setupConfig();
    }
    
    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "team-config.yml");
        
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Couldn't create team-config.yml.");
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        setDefaults();
    }
    
    private void setDefaults() {
        config.addDefault("teams.max-size", 10);
        config.addDefault("teams.min-name-length", 3);
        config.addDefault("teams.max-name-length", 16);
        config.addDefault("teams.allow-duplicate-colors", false);
        config.addDefault("teams.friendly-fire-default", false);
        config.addDefault("teams.auto-assign-armor", true);
        
        config.addDefault("colors.preset.red", "255,0,0");
        config.addDefault("colors.preset.blue", "0,0,255");
        config.addDefault("colors.preset.green", "0,255,0");
        config.addDefault("colors.preset.yellow", "255,255,0");
        config.addDefault("colors.preset.purple", "128,0,128");
        config.addDefault("colors.preset.orange", "255,165,0");
        config.addDefault("colors.preset.pink", "255,192,203");
        config.addDefault("colors.preset.cyan", "0,255,255");
        config.addDefault("colors.preset.white", "255,255,255");
        config.addDefault("colors.preset.black", "0,0,0");
        
        config.addDefault("permissions.create", "oxygenheist.team.create");
        config.addDefault("permissions.delete", "oxygenheist.team.delete");
        config.addDefault("permissions.add", "oxygenheist.team.add");
        config.addDefault("permissions.remove", "oxygenheist.team.remove");
        config.addDefault("permissions.color", "oxygenheist.team.color");
        config.addDefault("permissions.captain", "oxygenheist.team.captain");
        config.addDefault("permissions.list", "oxygenheist.team.list");
        config.addDefault("permissions.info", "oxygenheist.team.info");
        config.addDefault("permissions.friendlyfire", "oxygenheist.team.friendlyfire");
        
        config.addDefault("messages.team-created", "&a&lTeam created! &7Congratulations on your brand new squad.");
        config.addDefault("messages.team-deleted", "&c&lTeam deleted! &7Hope you didn't like them too much.");
        config.addDefault("messages.player-added", "&a&lPlayer added! &7One more soul to carry.");
        config.addDefault("messages.player-removed", "&c&lPlayer removed! &7They won't be missed.");
        config.addDefault("messages.captain-changed", "&e&lNew captain assigned! &7Try not to mess it up.");
        config.addDefault("messages.color-changed", "&a&lColor updated! &7Fabulous choice, darling.");
        config.addDefault("messages.no-permission", "&c&lNice try! &fBut you don't have permission for that.");
        config.addDefault("messages.team-exists", "&c&lOh, really? &fThat team name is already taken. How original.");
        config.addDefault("messages.team-not-found", "&c&lAkward... &fThat team doesn't exist. Try again?");
        config.addDefault("messages.not-captain", "&c&lHold up! &fOnly the captain can do that. Know your place.");
        config.addDefault("messages.already-in-team", "&c&lUh-oh! &fThat player is already in a team. Traitor?");
        config.addDefault("messages.not-in-team", "&c&lWeird... &fThat player isn't in a team. What are you doing?");
        config.addDefault("messages.team-full", "&c&lTeam's packed! &fNo more room for wannabes.");
        config.addDefault("messages.invalid-color", "&c&lInvalid color! &fDid you fail art class?");
        config.addDefault("messages.cannot-remove-captain", "&c&lNope! &fCan't remove the captain. Transfer leadership first.");
        config.addDefault("messages.friendlyfire-enabled", "&a&lFriendly fire enabled! &7Now you can betray your friends.");
        config.addDefault("messages.friendlyfire-disabled", "&c&lFriendly fire disabled! &7Your teammates are safe... for now.");
        
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
            plugin.getLogger().severe("Couldn't save team-config.yml: " + e.getMessage());
        }
    }
    
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public int getMaxTeamSize() {
        return config.getInt("teams.max-size", 10);
    }
    
    public int getMinNameLength() {
        return config.getInt("teams.min-name-length", 3);
    }
    
    public int getMaxNameLength() {
        return config.getInt("teams.max-name-length", 16);
    }
    
    public boolean allowDuplicateColors() {
        return config.getBoolean("teams.allow-duplicate-colors", false);
    }
    
    public boolean getFriendlyFireDefault() {
        return config.getBoolean("teams.friendly-fire-default", false);
    }
    
    public boolean autoAssignArmor() {
        return config.getBoolean("teams.auto-assign-armor", true);
    }
    
    public String getPresetColor(String colorName) {
        return config.getString("colors.preset." + colorName.toLowerCase(), null);
    }
    
    public String getPermission(String key) {
        return config.getString("permissions." + key, "oxygenheist.team." + key);
    }
    
    public String getMessage(String key) {
        return config.getString("messages." + key, "").replace("&", "§");
    }
    
    public void saveTeamName(String teamName) {
        List<String> teams = config.getStringList("saved-teams");
        if (!teams.contains(teamName)) {
            teams.add(teamName);
            config.set("saved-teams", teams);
            saveConfig();
        }
    }
    
    public void removeTeamName(String teamName) {
        List<String> teams = config.getStringList("saved-teams");
        teams.remove(teamName);
        config.set("saved-teams", teams);
        saveConfig();
    }
    
    public List<String> getSavedTeamNames() {
        return new ArrayList<>(config.getStringList("saved-teams"));
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public void saveTeamData(String teamName, String key, Object value) {
        config.set("data." + teamName.toLowerCase() + "." + key, value);
        saveConfig();
    }
    
    public Object getTeamData(String teamName, String key, Object defaultValue) {
        return config.get("data." + teamName.toLowerCase() + "." + key, defaultValue);
    }
    
    public org.bukkit.configuration.ConfigurationSection getTeamDataSection(String teamName) {
        return config.getConfigurationSection("data." + teamName.toLowerCase());
    }
}
