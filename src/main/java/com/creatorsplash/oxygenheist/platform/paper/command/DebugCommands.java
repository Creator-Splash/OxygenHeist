package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

@Command("oxygenheist|oh")
@Permission("com.creatorsplash.oxygenheist.debug")
@RequiredArgsConstructor
public final class DebugCommands implements CommandHandler {

    private final MatchService matchService;

    @Command("down <player> [ticks]")
    @CommandDescription("Force down a player")
    public void down(
        CommandSender sender,
        @Argument("player") Player target,
        @Argument("ticks") Integer rawTicks
    ) {
        int ticks = rawTicks == null ? 200 : rawTicks;

        matchService.getSession().ifPresentOrElse(session -> {
            session.getOrCreatePlayer(target.getUniqueId()).down(ticks);
            sender.sendRichMessage("<yellow>Downed " + target.getName());
            target.sendRichMessage("<yellow>You have been downed for <white>" + ticks + "</white> ticks");
        }, () -> sender.sendRichMessage("<red>No active game session"));
    }

    @Command("revive <player>")
    @CommandDescription("Force revive a player")
    public void revive(
        CommandSender sender,
        @Argument("player") Player target
    ) {
        matchService.getSession().ifPresentOrElse(session -> {
            session.getOrCreatePlayer(target.getUniqueId()).revive();
            target.sendRichMessage("<green>You have been revived");
            sender.sendRichMessage("<aqua>You have revived " + target.getName());
        }, () -> sender.sendRichMessage("<red>No active game session"));
    }

}
