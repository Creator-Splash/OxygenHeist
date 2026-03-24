package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

@Command("oxygenheight|oh")
@Permission("com.creatorsplash.oxygenheist.game")
@RequiredArgsConstructor
public final class GameCommands implements CommandHandler {

    private final MatchService matchService;

    @Command("start")
    @CommandDescription("Start a match")
    public void start(CommandSender sender) {
        matchService.createMatch();

        Bukkit.getOnlinePlayers().forEach(player ->
            matchService.addPlayer(player.getUniqueId()));

        matchService.startMatch();

        Bukkit.getServer().sendRichMessage("<aqua>Oxygen Heist Match Started!");
    }

    @Command("end")
    @CommandDescription("End the current match")
    public void end(CommandSender sender) {
        matchService.endMatch("manual");

        Bukkit.getServer().sendRichMessage("<red>Match ended");
    }

}
