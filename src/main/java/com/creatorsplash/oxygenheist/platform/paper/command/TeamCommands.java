package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.domain.team.TeamBase;
import com.creatorsplash.oxygenheist.platform.paper.config.team.TeamConfigService;
import com.creatorsplash.oxygenheist.platform.paper.display.LobbyDisplayManager;
import com.creatorsplash.oxygenheist.platform.paper.util.TeamUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;

import java.util.List;
import java.util.UUID;

@Command("oxygenheist|oh team")
@Permission("com.creatorsplash.oxygenheist.team")
@RequiredArgsConstructor
public final class TeamCommands implements CommandHandler {

    private final JavaPlugin plugin;
    private final TeamService teamService;
    private final TeamConfigService teamConfigService;
    private final LobbyDisplayManager lobbyDisplayManager;

    @Command("add <player> <team>")
    @CommandDescription("Add a player to a team")
    public void add(
        CommandSender sender,
        @Argument("player") Player player,
        @Argument(value = "team", suggestions = "teams") String teamId
    ) {
        Team team = teamService.getTeam(teamId);
        if (team == null) {
            sender.sendRichMessage("<red>Team '" + teamId + "' does not exist");
            return;
        }

        if (!teamService.addPlayerToTeam(player.getUniqueId(), teamId)) {
            sender.sendRichMessage("<red>" + player.getName()
                + " is already on a team or the team is full");
            return;
        }

        TeamUtils.applyArmor(player, team);
        lobbyDisplayManager.hideWaitingBar(player);

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        teamConfigService.save(plugin, teamService);

        player.sendRichMessage("<green>You have been added to team <"
            + team.getColor() + ">" + team.getName());
        sender.sendRichMessage("<green>Added " + player.getName()
            + " to <" + team.getColor() + ">" + team.getName());
    }

    @Command("remove <player>")
    @CommandDescription("Remove a player from their team")
    public void remove(
        CommandSender sender,
        @Argument("player") Player player
    ) {
        if (!teamService.removePlayerFromTeam(player.getUniqueId())) {
            sender.sendRichMessage("<red>" + player.getName() + " is not on any team");
            return;
        }

        TeamUtils.removeArmor(player);
        lobbyDisplayManager.showWaitingBar(player);

        player.setGameMode(GameMode.SPECTATOR);

        teamConfigService.save(plugin, teamService);

        player.sendRichMessage("<yellow>You have been removed from your team");
        sender.sendRichMessage("<green>Removed " + player.getName() + " from their team");
    }

    @Command("setbase <team>")
    @CommandDescription("Set a team's spawn base to your current location")
    public void setbase(
            Player sender,
            @Argument(value = "team", suggestions = "teams") String teamId
    ) {
        Team team = teamService.getTeam(teamId);
        if (team == null) {
            sender.sendRichMessage("<red>Team '" + teamId + "' does not exist");
            return;
        }

        var loc = sender.getLocation();
        TeamBase base = new TeamBase(
            loc.getWorld().getName(),
            loc.getX(), loc.getY(), loc.getZ(),
            loc.getYaw(), loc.getPitch()
        );

        team.setBase(base);
        teamConfigService.save(plugin, teamService);

        sender.sendRichMessage("<green>Base set for team <"
            + team.getColor() + ">" + team.getName()
            + " <gray>at <white>" + loc.getWorld().getName()
            + " (" + (int) loc.getX() + ", " + (int) loc.getY()
            + ", " + (int) loc.getZ() + ")");
    }

    @Command("captain <player> <team>")
    @CommandDescription("Set the captain of a team")
    public void captain(
        CommandSender sender,
        @Argument("player") Player player,
        @Argument(value = "team", suggestions = "teams") String teamId
    ) {
        if (!teamService.setCaptain(teamId, player.getUniqueId())) {
            sender.sendRichMessage("<red>Could not set captain - team '"
                    + teamId + "' may not exist or is full");
            return;
        }

        Team team = teamService.getTeam(teamId);
        // team null check

        TeamUtils.applyArmor(player, team);

        teamConfigService.save(plugin, teamService);

        player.sendRichMessage("<gold>You are now captain of team <"
            + team.getColor() + ">" + team.getName());
        sender.sendRichMessage("<green>Set " + player.getName()
            + " as captain of " + team.getName());
    }

    @Command("color <team> <color>")
    @CommandDescription("Change a teams armor color (MiniMessage color tag)")
    public void color(
        CommandSender sender,
        @Argument(value = "team", suggestions = "teams") String teamId,
        @Argument("color") String color
    ) {
        Team team = teamService.getTeam(teamId);
        if (team == null) {
            sender.sendRichMessage("<red>Team '" + teamId + "' does not exist");
            return;
        }

        team.setColor(color);

        for (UUID memberId : team.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                TeamUtils.applyArmor(member, team);
            }
        }

        sender.sendRichMessage("<green>Set " + team.getName()
            + " color to <" + color + ">" + color);
    }

    @Command("info <team>")
    @CommandDescription("Show info about a team")
    public void info(
        CommandSender sender,
        @Argument(value = "team", suggestions = "teams") String teamId
    ) {
        Team team = teamService.getTeam(teamId);
        if (team == null) {
            sender.sendRichMessage("<red>Team '" + teamId + "' does not exist");
            return;
        }

        sender.sendRichMessage(
            "<dark_gray>------- <" + team.getColor() + ">" + team.getName()
            + " <dark_gray>-------");
        sender.sendRichMessage("<gray>ID: <white>" + team.getId());
        sender.sendRichMessage("<gray>Members: <white>" + team.getSize()
            + "/" + teamService.getMaxTeamSize());

        if (team.hasCaptain()) {
            // null check on captain id?
            Player captain = Bukkit.getPlayer(team.getCaptainId());
            String captainName = captain != null
                ? captain.getName()
                : team.getCaptainId().toString();
            sender.sendRichMessage("<gray>Captain: <gold>" + captainName);
        }

        // todo null check on base world
        sender.sendRichMessage("<gray>Base: <white>" + (team.hasBase()
            ? team.getBase().world()
                + " (" + (int) team.getBase().x()
                + ", " + (int) team.getBase().y()
                + ", " + (int) team.getBase().z() + ")"
            : "Not set"));
    }

    @Command("list")
    @CommandDescription("List all teams")
    public void list(CommandSender sender) {
        if (teamService.getAllTeams().isEmpty()) {
            sender.sendRichMessage("<gray>No teams configured");
            return;
        }

        sender.sendRichMessage("<dark_gray>------- <white>Teams <dark_gray>-------");
        for (Team team : teamService.getAllTeams()) {
            sender.sendRichMessage("<" + team.getColor() + ">● " + team.getName()
                + " <gray>(" + team.getSize() + " members)");
        }
    }

    @Suggestions("teams")
    public List<String> suggestTeams(CommandContext<CommandSender> ctx) {
        return teamService.getAllTeams().stream().map(Team::getId).toList();
    }

}
