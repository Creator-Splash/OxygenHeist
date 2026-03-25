package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.Position3;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

import java.util.ArrayList;
import java.util.List;

@Command("oxygenheight|oh")
@Permission("com.creatorsplash.oxygenheist.game")
@RequiredArgsConstructor
public final class GameCommands implements CommandHandler {

    private final MatchService matchService;

    @Command("start")
    @CommandDescription("Start a match")
    public void start(CommandSender sender) {
        matchService.createMatch();

        MatchSession session = matchService.getSession().orElseThrow();

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            matchService.addPlayer(player.getUniqueId());

            String teamId = (i % 2 == 0) ? "red" : "blue"; // temp
            session.assignPlayerTeam(player.getUniqueId(), teamId);

            // temp
            player.sendRichMessage("<gray>You are on team <" + teamId + ">" + teamId);
        }

        CaptureZoneState zone = CaptureZoneState.fromCenterRadius(
            "test_zone",
                "world",
                new Position3(0.0, 64.0, 0.0),
                6.0
        );

        session.addZone(zone);

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
