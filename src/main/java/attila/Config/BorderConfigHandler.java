package attila.Config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import attila.OxygenMain;

public class BorderConfigHandler {
    
    private final OxygenMain plugin;
    private File configFile;
    private FileConfiguration config;
    
    public BorderConfigHandler(OxygenMain plugin) {
        this.plugin = plugin;
        setupConfig();
    }
    
    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "border-config.yml");
        
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Couldn't create border-config.yml.");
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        setDefaults();
    }
    
    private void setDefaults() {
        config.addDefault("wand.material", "STICK");
        config.addDefault("wand.name", "&6&lSelection Wand");
        
        config.addDefault("border.default-delay", 60);
        config.addDefault("border.default-final-size", 10.0);
        config.addDefault("border.default-duration", 300);
        config.addDefault("border.minimum-size", 5.0);
        config.addDefault("border.can-traverse", false);
        config.addDefault("border.warning-distance", 10);
        config.addDefault("border.damage-amount", 0.2);
        config.addDefault("border.damage-buffer", 5);
        
        config.addDefault("blocks.breaking-disabled", true);
        config.addDefault("blocks.placing-disabled", false);
        config.addDefault("blocks.explosions-disabled", true);
        config.addDefault("blocks.whitelist-enabled", true);
        config.addDefault("blocks.whitelisted-blocks", Arrays.asList(
            "TALL_GRASS",
            "GRASS",
            "DANDELION",
            "POPPY",
            "DEAD_BUSH",
            "FERN",
            "AZURE_BLUET",
            "RED_MUSHROOM",
            "BROWN_MUSHROOM"
        ));
        
        config.addDefault("permissions.wand", "oxygenheist.border.wand");
        config.addDefault("permissions.set", "oxygenheist.border.set");
        config.addDefault("permissions.setup", "oxygenheist.border.setup");
        config.addDefault("permissions.start", "oxygenheist.border.start");
        config.addDefault("permissions.stop", "oxygenheist.border.stop");
        config.addDefault("permissions.pause", "oxygenheist.border.pause");
        config.addDefault("permissions.resume", "oxygenheist.border.resume");
        config.addDefault("permissions.reset", "oxygenheist.border.reset");
        config.addDefault("permissions.info", "oxygenheist.border.info");
        config.addDefault("permissions.clear", "oxygenheist.border.clear");
        config.addDefault("permissions.block-bypass", "oxygenheist.block.bypass");
        config.addDefault("permissions.block-toggle", "oxygenheist.block.toggle");
        
        config.addDefault("messages.no-permission", "&c&lNo permission? &fHow unfortunate for you!");
        config.addDefault("messages.arena-set", "&a&lArena established! &7Finally.");
        config.addDefault("messages.border-setup", "&a&lBorder configured! &7About time.");
        config.addDefault("messages.timer-started", "&a&lTimer started! &7Let the games begin!");
        config.addDefault("messages.shrink-start", "&c&lTHE BORDER IS SHRINKING! &7Run, Forrest, run!");
        config.addDefault("messages.block-break-denied", "&c&lOh wow, trying to break blocks? &fHow adorable! &7This is a no-break zone, genius.");
        config.addDefault("messages.block-place-denied", "&c&lNice try, architect! &fBut we're not redecorating today. &7No block placing here.");
        config.addDefault("messages.block-breaking-enabled", "&a&lBlock breaking is now ENABLED! &7Because reasons.");
        config.addDefault("messages.block-breaking-disabled", "&c&lBlock breaking is now DISABLED! &7Surprise!");
        config.addDefault("messages.block-placing-enabled", "&a&lBlock placing is now ENABLED! &7Deal with it.");
        config.addDefault("messages.block-placing-disabled", "&c&lBlock placing is now DISABLED! &7No more building for you.");
        config.addDefault("messages.explosions-enabled", "&a&lExplosions are now ENABLED! &7Boom time!");
        config.addDefault("messages.explosions-disabled", "&c&lExplosions are now DISABLED! &7Boom... or not.");
        config.addDefault("messages.whitelist-added", "&a&lBlock added to whitelist! &7How generous.");
        config.addDefault("messages.whitelist-removed", "&c&lBlock removed from whitelist! &7Harsh.");
        config.addDefault("messages.invalid-material", "&c&lInvalid material! &7Did you even try?");
        
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
            plugin.getLogger().severe("Couldn't save border-config.yml: " + e.getMessage());
        }
    }
    
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public String getWandMaterial() {
        return config.getString("wand.material", "STICK");
    }
    
    public String getWandName() {
        return config.getString("wand.name", "&6&lSelection Wand");
    }
    
    public int getDefaultDelay() {
        return config.getInt("border.default-delay", 60);
    }
    
    public double getDefaultFinalSize() {
        return config.getDouble("border.default-final-size", 10.0);
    }
    
    public int getDefaultDuration() {
        return config.getInt("border.default-duration", 300);
    }
    
    public int getWarningDistance() {
        return config.getInt("border.warning-distance", 10);
    }
    
    public double getDamageAmount() {
        return config.getDouble("border.damage-amount", 0.2);
    }
    
    public int getDamageBuffer() {
        return config.getInt("border.damage-buffer", 5);
    }
    
    public double getMinimumSize() {
        return config.getDouble("border.minimum-size", 5.0);
    }
    
    public void setMinimumSize(double size) {
        config.set("border.minimum-size", size);
        saveConfig();
    }
    
    public boolean canTraverseBorder() {
        return config.getBoolean("border.can-traverse", false);
    }
    
    public void setCanTraverseBorder(boolean canTraverse) {
        config.set("border.can-traverse", canTraverse);
        saveConfig();
    }
    
    public boolean isBlockBreakingDisabled() {
        return config.getBoolean("blocks.breaking-disabled", true);
    }
    
    public void setBlockBreakingDisabled(boolean disabled) {
        config.set("blocks.breaking-disabled", disabled);
        saveConfig();
    }
    
    public boolean isBlockPlacingDisabled() {
        return config.getBoolean("blocks.placing-disabled", false);
    }
    
    public void setBlockPlacingDisabled(boolean disabled) {
        config.set("blocks.placing-disabled", disabled);
        saveConfig();
    }
    
    public boolean areExplosionsDisabled() {
        return config.getBoolean("blocks.explosions-disabled", true);
    }
    
    public void setExplosionsDisabled(boolean disabled) {
        config.set("blocks.explosions-disabled", disabled);
        saveConfig();
    }
    
    public boolean isWhitelistEnabled() {
        return config.getBoolean("blocks.whitelist-enabled", true);
    }
    
    public void setWhitelistEnabled(boolean enabled) {
        config.set("blocks.whitelist-enabled", enabled);
        saveConfig();
    }
    
    public List<String> getWhitelistedBlocks() {
        return config.getStringList("blocks.whitelisted-blocks");
    }
    
    public void addWhitelistedBlock(String material) {
        List<String> blocks = getWhitelistedBlocks();
        if (!blocks.contains(material.toUpperCase())) {
            blocks.add(material.toUpperCase());
            config.set("blocks.whitelisted-blocks", blocks);
            saveConfig();
        }
    }
    
    public void removeWhitelistedBlock(String material) {
        List<String> blocks = getWhitelistedBlocks();
        blocks.remove(material.toUpperCase());
        config.set("blocks.whitelisted-blocks", blocks);
        saveConfig();
    }
    
    public String getPermission(String key) {
        return config.getString("permissions." + key, "oxygenheist.border." + key);
    }
    
    public String getMessage(String key) {
        return config.getString("messages." + key, "").replace("&", "§");
    }

    
    public void saveArenaData(String worldName, double centerX, double centerY, double centerZ, double size) {
        config.set("arena.world", worldName);
        config.set("arena.center.x", centerX);
        config.set("arena.center.y", centerY);
        config.set("arena.center.z", centerZ);
        config.set("arena.size", size);
        config.set("arena.configured", true);
        saveConfig();
    }
    
    public boolean hasArenaData() {
        return config.getBoolean("arena.configured", false);
    }
    
    public String getArenaWorld() {
        return config.getString("arena.world", "");
    }
    
    public double getArenaCenterX() {
        return config.getDouble("arena.center.x", 0.0);
    }
    
    public double getArenaCenterY() {
        return config.getDouble("arena.center.y", 0.0);
    }
    
    public double getArenaCenterZ() {
        return config.getDouble("arena.center.z", 0.0);
    }
    
    public double getArenaSize() {
        return config.getDouble("arena.size", 100.0);
    }
    
    public void clearArenaData() {
        config.set("arena", null);
        saveConfig();
    }
}