package com.creatorsplash.oxygenheist.platform.paper.display.placeholder;

import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.domain.player.PlayerSnapshot;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.domain.zone.ZoneSnapshot;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
public final class OxygenHeistPlaceholderExpansion extends PlaceholderExpansion {

    private final OxygenHeistPlugin plugin;
    private final TeamService teamService;
    private final Supplier<MatchSnapshot> snapshotSupplier;

    @Override
    public @NotNull String getIdentifier() {
        return "oxygenheist";
    }

    @Override
    public @NotNull String getAuthor() {
        return "OxygenHeist";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(
        Player player,
        @NotNull String params
    ) {
        MatchSnapshot snapshot = snapshotSupplier.get();

        /* Player scoped */
        if (player != null) {
            switch (params) {
                case "player_team" -> {
                    Team team = teamService.getPlayerTeam(player.getUniqueId());
                    return team != null ? team.getName() : "None";
                }
                case "player_team_color" -> {
                    Team team = teamService.getPlayerTeam(player.getUniqueId());
                    return team != null ? team.getColor() : "gray";
                }
                case "player_is_captain" -> {
                    Team team = teamService.getPlayerTeam(player.getUniqueId());
                    return Boolean.toString(team != null && team.isCaptain(player.getUniqueId()));
                }
            }

            if (snapshot != null) {
                var playerSnapshot = snapshot.getPlayer(player.getUniqueId());
                if (playerSnapshot != null) {
                    switch (params) {
                        case "player_oxygen":
                            return Double.toString(playerSnapshot.oxygen());
                        case "player_score":
                            return Integer.toString(playerSnapshot.score());
                        case "player_is_downed":
                            return Boolean.toString(playerSnapshot.downed());
                        case "player_is_dead":
                            return Boolean.toString(!playerSnapshot.alive());
                        default: break;
                    }
                }
            }
        }

        /* Team-scoped (team_<id>_<field>) */
        if (params.startsWith("team_")) {
            if (params.endsWith("_score")) {
                String id = params.substring(5, params.length() - 6);
                int score = snapshot != null
                    ? snapshot.getTeamScoreOrZero(id)
                    : 0;
                return String.valueOf(score);
            }
            if (params.endsWith("_members")) {
                String id = params.substring(5, params.length() - 8);
                Team team = teamService.getTeam(id);
                return team != null ? String.valueOf(team.getSize()) : "0";
            }
            if (params.endsWith("_captain")) {
                String id = params.substring(5, params.length() - 8);
                Team team = teamService.getTeam(id);
                if (team != null && team.hasCaptain()) {
                    Player captain = plugin.getServer().getPlayer(team.getCaptainId());
                    return captain != null ? captain.getName() : "Offline";
                }
                return "None";
            }
        }

        /* Zone-scoped (zone_<id>_<field>) */
        if (params.startsWith("zone_") && snapshot != null) {
            if (params.endsWith("_progress")) {
                String id = params.substring(5, params.length() - 9);
                ZoneSnapshot z = snapshot.getZone(id);
                return z != null ? String.valueOf((int) z.captureProgress()) : "0";
            }
            if (params.endsWith("_owner")) {
                String id = params.substring(5, params.length() - 6);
                ZoneSnapshot z = snapshot.getZone(id);
                if (z != null && z.ownerTeamId() != null) {
                    Team team = teamService.getTeam(z.ownerTeamId());
                    return team != null ? team.getName() : z.ownerTeamId();
                }
                return "Neutral";
            }
            if (params.endsWith("_capturing")) {
                String id = params.substring(5, params.length() - 10);
                ZoneSnapshot z = snapshot.getZone(id);
                if (z != null && z.capturingTeamId() != null) {
                    Team team = teamService.getTeam(z.capturingTeamId());
                    return team != null ? team.getName() : z.capturingTeamId();
                }
                return "None";
            }
            if (params.contains("_oxygen_")) {
                String[] parts = params.substring(5).split("_oxygen_", 2);
                if (parts.length == 2) {
                    ZoneSnapshot z = snapshot.getZone(parts[0]);
                    if (z != null) {
                        return String.valueOf(z.teamOxygen().getOrDefault(parts[1], 100.0));
                    }
                }
                return "100";
            }
        }

        /* Top N (top_<n>_name / top_<n>_score) */
        if (params.startsWith("top_") && snapshot != null) {
            String[] parts = params.split("_");
            if (parts.length == 3) {
                try {
                    int pos = Integer.parseInt(parts[1]);
                    String field = parts[2];
                    List<Team> sorted = teamService.getAllTeams().stream()
                        .sorted((a, b) -> Integer.compare(
                            snapshot.getTeamScoreOrZero(b.getId()),
                            snapshot.getTeamScoreOrZero(a.getId())
                        ))
                        .toList();
                    if (pos > 0 && pos <= sorted.size()) {
                        Team team = sorted.get(pos - 1);
                        return switch (field) {
                            case "name" -> team.getName();
                            case "score" -> String.valueOf(snapshot.getTeamScoreOrZero(team.getId()));
                            case "members" -> String.valueOf(team.getSize());
                            default -> "";
                        };
                    }
                    return field.equals("name") ? "---" : "0";
                } catch (NumberFormatException ignored) {}
            }
        }

        if (snapshot == null) return "";

        return switch (params) {
            case "game_state" -> snapshot.state().name();
            case "game_state_display" -> switch (snapshot.state()) {
                case PLAYING -> "In Progress";
                case WAITING -> "Waiting";
                case SETUP   -> "Starting";
                case ENDING  -> "Ended";
            };
            case "game_time" -> formatTime(snapshot.remainingSeconds());
            case "game_time_seconds" -> String.valueOf(snapshot.remainingSeconds());
            case "game_instant_death" -> Boolean.toString(snapshot.instantDeath());
            case "zone_count" -> String.valueOf(snapshot.zones().size());
            case "leading_team" -> leadingTeam(snapshot).map(Team::getName).orElse("None");
            case "leading_team_score" -> leadingTeam(snapshot)
                .map(t -> String.valueOf(snapshot.getTeamScoreOrZero(t.getId())))
                .orElse("0");
            default -> "";
        };
    }

    private static String formatTime(int totalSeconds) {
        int mins = Math.max(0, totalSeconds) / 60;
        int secs = Math.max(0, totalSeconds) % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private Optional<Team> leadingTeam(MatchSnapshot snapshot) {
        return teamService.getAllTeams().stream()
            .max(Comparator.comparingInt(t -> snapshot.getTeamScoreOrZero(t.getId())));
    }

}
