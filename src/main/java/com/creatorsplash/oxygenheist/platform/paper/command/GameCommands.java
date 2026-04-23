package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Command("oxygenheist|oh")
@Permission("com.creatorsplash.oxygenheist.game")
@RequiredArgsConstructor
public final class GameCommands implements CommandHandler {

    private final OxygenHeistPlugin plugin;
    private final MatchService matchService;
    private final ArenaConfigService arenaConfigService;
    private final TeamService teamService;

    @Command("start")
    @CommandDescription("Start a match")
    public void start(CommandSender sender) {
        boolean eventMode = OxygenHeistPlugin.instance()
                .getConfig().getBoolean("event-mode.enabled", false);
        if (eventMode) {
            sender.sendRichMessage("<red>Manual /oh start is disabled in event-mode. Matches are launched by the tournament Core.");
            return;
        }

        List<String> errors = validatePreStart();
        if (!errors.isEmpty()) {
            sender.sendRichMessage("<red><bold>Cannot start match:</bold>");
            errors.forEach(e -> sender.sendRichMessage("<red> ✗ " + e));
            return;
        }

        Set<UUID> activePlayers = teamService.getAllTeams().stream()
            .flatMap(t -> t.getMembers().stream())
            .filter(id -> Bukkit.getPlayer(id) != null)
            .collect(Collectors.toSet());

        matchService.createMatch();
        matchService.startMatch(activePlayers);

        Bukkit.getServer().sendRichMessage("<aqua><bold>Oxygen Heist</bold> <gray>- match starting!");
    }

    @Command("end")
    @CommandDescription("End the current match")
    public void end(CommandSender sender) {
        if (matchService.getSession().isEmpty()) {
            sender.sendRichMessage("<red>No match is currently running.");
            return;
        }

        matchService.endMatch();
        Bukkit.getServer().sendRichMessage("<red><bold>Oxygen Heist</bold> <gray>- match ended by admin");
    }

    private List<String> validatePreStart() {
        List<String> errors = new ArrayList<>();

        if (matchService.getSession().isPresent()) {
            errors.add("A match is already running. Use /oh end first.");
            return errors; // no point checking further
        }

        if (!arenaConfigService.isArenaConfigured()) {
            errors.add("No arena configured. Use /oh arena set first.");
        }

        List<Team> teams;
        teams = teamService.getAllTeams().stream()
            .filter(t -> t.getMembers().stream()
                .anyMatch(id -> plugin.getServer().getPlayer(id) != null))
                .toList();

        if (teams.size() < 2) {
            errors.add("At least 2 teams with online members are required (found " + teams.size() + ").");
        }

        for (Team team : teams) {
            if (!team.hasBase()) {
                errors.add("Team '" + team.getName() + "' has no base set. Use /oh team setbase " + team.getId());
            }
        }

        return errors;
    }
}
