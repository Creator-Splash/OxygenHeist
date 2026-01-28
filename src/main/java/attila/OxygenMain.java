package attila;

import org.bukkit.plugin.java.JavaPlugin;

import attila.Border.BorderManager;
import attila.Border.BorderShrink;
import attila.Border.BorderTimer;
import attila.Commands.BorderCommandCompleter;
import attila.Commands.BorderCommands;
import attila.Commands.GameCommandCompleter;
import attila.Commands.GameCommands;
import attila.Commands.OxygenHeistCommandCompleter;
import attila.Commands.TeamCommandCompleter;
import attila.Commands.TeamCommands;
import attila.Config.BorderConfigHandler;
import attila.Config.GameConfigHandler;
import attila.Config.TeamConfigHandler;
import attila.Game.GameManager;
import attila.Game.OxygenManager;
import attila.Game.WeaponSpawnerManager;
import attila.Managers.BlockManager;
import attila.Placeholders.OxygenPlaceholderExpansion;
import attila.Teams.TeamManager;
import attila.oxygenweapons.*;

public class OxygenMain extends JavaPlugin {

    private static OxygenMain instance;
    private BorderManager borderManager;
    private BorderShrink borderShrink;
    private BorderTimer borderTimer;
    private BorderConfigHandler borderConfig;
    private BlockManager blockManager;
    private TeamManager teamManager;
    private TeamConfigHandler teamConfig;
    private GameManager gameManager;
    private GameConfigHandler gameConfig;
    private OxygenManager oxygenManager;
    private WeaponSpawnerManager weaponSpawnerManager;

    @Override
    public void onEnable() {
        instance = this;


        getServer().getPluginManager().registerEvents(new ReefHarpoonGun(this), this);
        getServer().getPluginManager().registerEvents(new SpikeShooter(this), this);
        getServer().getPluginManager().registerEvents(new VenomSpitter(this), this);
        getServer().getPluginManager().registerEvents(new NeedleRifle(this), this); // Esto me ha costado un ojo de la cara hacerlo :)
        getServer().getPluginManager().registerEvents(new MantaCannon(this), this);
        getServer().getPluginManager().registerEvents(new DartSlingshot(this), this);
        getServer().getPluginManager().registerEvents(new StealCrossbow(this), this);
        getServer().getPluginManager().registerEvents(new ClawCannon(this), this);
        getServer().getPluginManager().registerEvents(new SiltBlaster(this), this);

        
        /*
        * LA GUADALUPANAAA, LA GUADALUPANAA, LA GUADALUPANAAA
        */
        
        borderConfig = new BorderConfigHandler(this);
        borderConfig.loadConfig();
        
        teamConfig = new TeamConfigHandler(this);
        teamConfig.loadConfig();
        
        gameConfig = new GameConfigHandler(this);
        gameConfig.loadConfig();
        
        borderManager = new BorderManager(this);
        borderShrink = new BorderShrink(this, borderManager);
        borderTimer = new BorderTimer(this, borderShrink);
        blockManager = new BlockManager(this, borderManager);
        teamManager = new TeamManager(this);
        gameManager = new GameManager(this, teamManager);
        oxygenManager = new OxygenManager(this, gameManager, teamManager);
        weaponSpawnerManager = new WeaponSpawnerManager(this, gameManager);
        gameManager.setOxygenManager(oxygenManager);
        gameManager.setWeaponSpawnerManager(weaponSpawnerManager);
        
        getServer().getPluginManager().registerEvents(borderManager, this);
        getServer().getPluginManager().registerEvents(blockManager, this);
        getServer().getPluginManager().registerEvents(teamManager, this);
        getServer().getPluginManager().registerEvents(gameManager, this);
        getServer().getPluginManager().registerEvents(oxygenManager, this);
        getServer().getPluginManager().registerEvents(weaponSpawnerManager, this);
        
        BorderCommands borderCommands = new BorderCommands(this, borderManager, borderTimer);
        getCommand("border").setExecutor(borderCommands);
        getCommand("border").setTabCompleter(new BorderCommandCompleter());
        
        TeamCommands teamCommands = new TeamCommands(this, teamManager);
        getCommand("team").setExecutor(teamCommands);
        getCommand("team").setTabCompleter(new TeamCommandCompleter());
        
        GameCommands gameCommands = new GameCommands(this, gameManager, teamManager);
        getCommand("game").setExecutor(gameCommands);
        getCommand("game").setTabCompleter(new GameCommandCompleter(this));

        getCommand("oxygenheist").setExecutor(this);
        getCommand("oxygenheist").setTabCompleter(new OxygenHeistCommandCompleter());

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OxygenPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully!");
        } else {
            getLogger().info("PlaceholderAPI not found - placeholders will not be available.");
        }

        gameManager.cleanupHolograms();
        weaponSpawnerManager.cleanupOrphanedHolograms();
        

    }

    private void La_Guadalupana() {
        getLogger().info("");
        getLogger().info("Desde el cielo una hermosa mañana");
        getLogger().info("Desde el cielo una hermosa mañana");
        getLogger().info("La Guadalupana");
        getLogger().info("La Guadalupana, la Guadalupana");
        getLogger().info("bajó al Tepeyac");
        getLogger().info("La Guadalupana");
        getLogger().info("La Guadalupana, la Guadalupana");
        getLogger().info("bajó al Tepeyac");
        getLogger().info("");
        getLogger().info("Su llegada llenó de alegría");
        getLogger().info("Su llegada llenó de alegría");
        getLogger().info("De luz y armonía");
        getLogger().info("De luz y armonía y de libertad");
        getLogger().info("De luz y armonía todo el Anahuatl");
        getLogger().info("De luz y armonía");
        getLogger().info("De luz y armonía");
        getLogger().info("De luz y armonía todo el Anahuatl");
        getLogger().info("");
        getLogger().info("Por el monte pasaba Juan Diego");
        getLogger().info("Por el monte pasaba Juan Diego");
        getLogger().info("Y acercose luego");
        getLogger().info("Y acercose luego");
        getLogger().info("Y acercose luego al oír cantar");
        getLogger().info("Y acercose luego");
        getLogger().info("Y acercose luego");
        getLogger().info("Y acercose luego al oír cantar");
        getLogger().info("");
        getLogger().info("Juan Dieguito, la Virgen le dijo");
        getLogger().info("Juan Dieguito, la Virgen le dijo");
        getLogger().info("Este cerro elijo");
        getLogger().info("Este cerro elijo");
        getLogger().info("Este cerro elijo para hacer mi altar");
        getLogger().info("Este cerro elijo");
        getLogger().info("Este cerro elijo");
        getLogger().info("Este cerro elijo para hacer mi altar");
        getLogger().info("");
        getLogger().info("Suplicante juntaba sus manos");
        getLogger().info("Suplicante juntaba sus manos");
        getLogger().info("Y eran mexicanos");
        getLogger().info("Y eran mexicanos");
        getLogger().info("Y eran mexicanos su porte y su faz");
        getLogger().info("Y eran mexicanos");
        getLogger().info("Y eran mexicanos");
        getLogger().info("Y eran mexicanos su porte y su faz");
        getLogger().info("");
        getLogger().info("En la Tilma entre rosas pintadas");
        getLogger().info("En la Tilma entre rosas pintadas");
        getLogger().info("Su imagen amada");
        getLogger().info("Su imagen amada");
        getLogger().info("Su imagen amada se dignó a dejar");
        getLogger().info("Su imagen amada");
        getLogger().info("Su imagen amada");
        getLogger().info("Su imagen amada se dignó a dejar");
        getLogger().info("");
        getLogger().info("Desde entonces para el mexicano");
        getLogger().info("Desde entonces para el mexicano");
        getLogger().info("Ser Guadalupano");
        getLogger().info("Ser Guadalupano");
        getLogger().info("Ser Guadalupano es algo esencial");
        getLogger().info("Ser Guadalupano");
        getLogger().info("Ser Guadalupano");
        getLogger().info("Ser Guadalupano es algo esencial");
        getLogger().info("");
    }


    private void VIVA() {
        getLogger().info("");
        getLogger().info("╔═══════════════════════════════════════════╗");
        getLogger().info("║                                           ║");
        getLogger().info("║     ██╗   ██╗██╗██╗   ██╗ █████╗          ║");
        getLogger().info("║     ██║   ██║██║██║   ██║██╔══██╗         ║");
        getLogger().info("║     ██║   ██║██║██║   ██║███████║         ║");
        getLogger().info("║     ╚██╗ ██╔╝██║╚██╗ ██╔╝██╔══██║         ║");
        getLogger().info("║      ╚████╔╝ ██║ ╚████╔╝ ██║  ██║         ║");
        getLogger().info("║       ╚═══╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝         ║");
        getLogger().info("║                                           ║");
        getLogger().info("╚═══════════════════════════════════════════╝");
        getLogger().info("");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("            ██████░░░░░░░░░░░░░░░░░░██████");
        getLogger().info("");
        getLogger().info("");
        getLogger().info("        ⚡ VIVAN HIDALGO Y MORELOS ⚡");
        getLogger().info("        🔥 VIVAAAAAAAAAAAA 🔥");
        getLogger().info("        ⭐ VIVAN VILLA Y ZAPATA ⭐");
        getLogger().info("        💥 VIVAAAAAAAAAAAA 💥");
        getLogger().info("");
        getLogger().info("   ════════════════════════════════════════════════════");
        getLogger().info("   ║  CHINGUEN A SU MADRE LOS PINCHES POLITICOS     ║");
        getLogger().info("   ║         RATEROS HIJOS DE PUTAAAAAA             ║");
        getLogger().info("   ════════════════════════════════════════════════════");
        getLogger().info("");
        getLogger().info("        🎺🎺🎺 SIIIIIIIIIIIIIIIIII 🎺🎺🎺");
        getLogger().info("");
        getLogger().info("              Ahi es cuando la raza dice:");
        getLogger().info("        🗣️  Que la chinguen, que la chinguen  🗣️");
        getLogger().info("        🗣️  Que la chinguen, que la chinguen  🗣️");
        getLogger().info("");
        getLogger().info("               .-. .-. .-. .-. .-. .-.");
        getLogger().info("              |V| |I| |R| |G| |E| |N|");
        getLogger().info("               '-' '-' '-' '-' '-' '-'");
        getLogger().info("         ✨ VIVA LA VIRGEN DE GUADALUPE ✨");
        getLogger().info("");
        getLogger().info("   ███╗   ███╗███████╗██╗  ██╗██╗ ██████╗ ██████╗ ");
        getLogger().info("   ████╗ ████║██╔════╝╚██╗██╔╝██║██╔════╝██╔═══██╗");
        getLogger().info("   ██╔████╔██║█████╗   ╚███╔╝ ██║██║     ██║   ██║");
        getLogger().info("   ██║╚██╔╝██║██╔══╝   ██╔██╗ ██║██║     ██║   ██║");
        getLogger().info("   ██║ ╚═╝ ██║███████╗██╔╝ ██╗██║╚██████╗╚██████╔╝");
        getLogger().info("   ╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝╚═╝ ╚═════╝ ╚═════╝ ");
        getLogger().info("");
        getLogger().info("   🎉 VIVA MEXICOOOOOOOOOOOOOO 🎉");
        getLogger().info("   🇲🇽🇲🇽 VIVA MEXICO CABRONESSSS 🇲🇽🇲🇽");
        getLogger().info("");
        getLogger().info("        🎸 Y QUE VIVA EL ROCK AND ROLLLLL 🎸");
        getLogger().info("             ♫ ♪ ♫ ♪ ♫ ♪ ♫ ♪ ♫ ♪ ♫");
        getLogger().info("");
        getLogger().info("╔════════════════════════════════════════════════════════════════╗");
        getLogger().info("║          🌟 ARRIBA MÉXICO, HIJOS DE SU PUTA MADRE! 🌟         ║");
        getLogger().info("╚════════════════════════════════════════════════════════════════╝");
        getLogger().info("Ah si, plugin apagado correctamente.");
    }

    @Override
    public void onDisable() {
        if (borderTimer != null) {
            borderTimer.cancelTimer();
        }
        if (borderConfig != null) {
            borderConfig.saveConfig();
        }
        if (teamConfig != null) {
            teamConfig.saveConfig();
        }
        if (gameConfig != null) {
            gameConfig.saveConfig();
        }
    }

    public static OxygenMain getInstance() {
        return instance;
    }
    
    public BorderManager getBorderManager() {
        return borderManager;
    }
    
    public BorderShrink getBorderShrink() {
        return borderShrink;
    }
    
    public BorderTimer getBorderTimer() {
        return borderTimer;
    }
    
    public BorderConfigHandler getBorderConfig() {
        return borderConfig;
    }
    
    public BlockManager getBlockManager() {
        return blockManager;
    }
    
    public TeamManager getTeamManager() {
        return teamManager;
    }
    
    public TeamConfigHandler getTeamConfig() {
        return teamConfig;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public GameConfigHandler getGameConfig() {
        return gameConfig;
    }
    
    public OxygenManager getOxygenManager() {
        return oxygenManager;
    }
    
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("oxygenheist")) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /oxygenheist reload");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("oxygenheist.reload")) {
                    sender.sendMessage("§cYou don't have permission to reload the plugin!");
                    return true;
                }
                
                try {
                    reloadConfigs();
                    sender.sendMessage("§aPlugin configuration reloaded successfully!");
                    getLogger().info("Plugin configuration reloaded by " + sender.getName());
                } catch (Exception e) {
                    sender.sendMessage("§cFailed to reload plugin configuration: " + e.getMessage());
                    getLogger().severe("Failed to reload plugin configuration: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            }
            
            sender.sendMessage("§cUnknown subcommand. Use: /oxygenheist reload");
            return true;
        }
        return false;
    }
    
    private void reloadConfigs() {

        if (borderConfig != null) {
            borderConfig.loadConfig();
        }
        if (teamConfig != null) {
            teamConfig.loadConfig();
        }
        if (gameConfig != null) {
            gameConfig.loadConfig();
        }

        if (borderManager != null) {

            getLogger().info("Border configuration reloaded");
        }
        
        getLogger().info("All configurations reloaded successfully");
    }
}
