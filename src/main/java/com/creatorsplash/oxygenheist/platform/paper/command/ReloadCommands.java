package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.platform.paper.config.match.MatchConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfigService;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

@Command("oxygenheist|oh")
@Permission("com.creatorsplash.oxygenheist.admin")
@RequiredArgsConstructor
public final class ReloadCommands implements CommandHandler {

    private final JavaPlugin plugin;
    private final MatchConfigService matchConfigService;
    private final MessageConfigService messageConfigService;
    private final LogCenter log;

    @Command("reload")
    @CommandDescription("Reload all plugin configuration files")
    public void reload(CommandSender sender) {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        matchConfigService.load(config);
        messageConfigService.load();

        sender.sendRichMessage("<green>OxygenHeist config reloaded.");
        log.info("Config reloaded by " + sender.getName());
    }

}
