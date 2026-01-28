package attila.Placeholders;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import attila.Game.CaptureZone;
import attila.Game.GameManager;
import attila.Game.GameState;
import attila.OxygenMain;
import attila.Teams.Team;
import attila.Teams.TeamManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class OxygenPlaceholderExpansion extends PlaceholderExpansion {
    
    private final OxygenMain plugin;
    
    public OxygenPlaceholderExpansion(OxygenMain plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "oxygenheist";
    }
    
    @Override
    public String getAuthor() {
        return "Arthur, Desau";
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        TeamManager teamManager = plugin.getTeamManager();
        GameManager gameManager = plugin.getGameManager();
        
        if (teamManager == null || gameManager == null) {
            return "";
        }

        if (player != null) {
            Team playerTeam = teamManager.getPlayerTeam(player);

            if (identifier.equals("player_team")) {
                return playerTeam != null ? playerTeam.getName() : "None";
            }

            if (identifier.equals("player_team_color")) {
                if (playerTeam == null) return "§7";
                return getColorCode(playerTeam);
            }

            if (identifier.equals("player_team_points")) {
                return playerTeam != null ? String.valueOf(playerTeam.getPoints()) : "0";
            }

            if (identifier.equals("player_is_captain")) {
                return playerTeam != null && playerTeam.isCaptain(player.getUniqueId()) ? "true" : "false";
            }

            if (identifier.equals("player_is_downed")) {
                return gameManager.isPlayerDowned(player.getUniqueId()) ? "true" : "false";
            }

            if (identifier.equals("player_is_dead")) {
                return gameManager.isPlayerDead(player.getUniqueId()) ? "true" : "false";
            }

            if (identifier.equals("player_oxygen")) {
                if (plugin.getOxygenManager() != null && playerTeam != null) {
                    return String.valueOf(plugin.getOxygenManager().getTeamOxygen(playerTeam));
                }
                return "100";
            }
        }

        if (identifier.startsWith("team_") && identifier.endsWith("_points")) {
            String teamName = identifier.substring(5, identifier.length() - 7);
            Team team = teamManager.getTeam(teamName);
            return team != null ? String.valueOf(team.getPoints()) : "0";
        }

        if (identifier.startsWith("team_") && identifier.endsWith("_members")) {
            String teamName = identifier.substring(5, identifier.length() - 8);
            Team team = teamManager.getTeam(teamName);
            return team != null ? String.valueOf(team.getSize()) : "0";
        }

        if (identifier.startsWith("team_") && identifier.endsWith("_captain")) {
            String teamName = identifier.substring(5, identifier.length() - 8);
            Team team = teamManager.getTeam(teamName);
            if (team != null && team.getCaptainUUID() != null) {
                Player captain = Bukkit.getPlayer(team.getCaptainUUID());
                return captain != null ? captain.getName() : "Offline";
            }
            return "None";
        }

        if (identifier.startsWith("team_") && identifier.endsWith("_oxygen")) {
            String teamName = identifier.substring(5, identifier.length() - 7);
            Team team = teamManager.getTeam(teamName);
            if (team != null && plugin.getOxygenManager() != null) {
                return String.valueOf(plugin.getOxygenManager().getTeamOxygen(team));
            }
            return "100";
        }

        if (identifier.startsWith("team_name_")) {
            String teamName = identifier.substring("team_name_".length());
            Team team = teamManager.getTeam(teamName);
            return team != null ? team.getName() : "None";
        }


        if (identifier.equals("game_state")) {
            return gameManager.getGameState().name();
        }

        if (identifier.equals("game_state_display")) {
            GameState state = gameManager.getGameState();
            switch (state) {
                case WAITING: return "§eWaiting";
                case COUNTDOWN: return "§6Starting";
                case PLAYING: return "§aIn Progress";
                case ENDED: return "§cEnded";
                default: return "§7Unknown";
            }
        }

        if (identifier.equals("teams_count")) {
            return String.valueOf(teamManager.getAllTeams().size());
        }

        if (identifier.equals("teams_list")) {
            StringBuilder sb = new StringBuilder();
            for (Team team : teamManager.getAllTeams().values()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(team.getName());
            }
            return sb.length() > 0 ? sb.toString() : "None";
        }

        if (identifier.equals("leading_team")) {
            Team leading = getLeadingTeam(teamManager);
            return leading != null ? leading.getName() : "None";
        }

        if (identifier.equals("leading_team_points")) {
            Team leading = getLeadingTeam(teamManager);
            return leading != null ? String.valueOf(leading.getPoints()) : "0";
        }

        if (identifier.equals("zones_count")) {
            return String.valueOf(gameManager.getAllCaptureZones().size());
        }

        if (identifier.startsWith("zone_") && identifier.endsWith("_owner")) {
            String zoneName = identifier.substring(5, identifier.length() - 6);
            CaptureZone zone = gameManager.getCaptureZone(zoneName);
            if (zone != null && zone.hasOwner()) {
                return zone.getOwnerTeam().getName();
            }
            return "Uncaptured";
        }

        if (identifier.startsWith("zone_") && identifier.endsWith("_progress")) {
            String zoneName = identifier.substring(5, identifier.length() - 9);
            CaptureZone zone = gameManager.getCaptureZone(zoneName);
            return zone != null ? String.valueOf(zone.getCaptureProgress()) : "0";
        }

        if (identifier.startsWith("zone_") && identifier.endsWith("_capturing")) {
            String zoneName = identifier.substring(5, identifier.length() - 10);
            CaptureZone zone = gameManager.getCaptureZone(zoneName);
            if (zone != null && zone.getCapturingTeam() != null) {
                return zone.getCapturingTeam().getName();
            }
            return "None";
        }


        if (identifier.startsWith("top_")) {
            String[] parts = identifier.split("_");
            if (parts.length == 3) {
                try {
                    int position = Integer.parseInt(parts[1]);
                    String type = parts[2];
                    
                    java.util.List<Team> sortedTeams = new java.util.ArrayList<>(teamManager.getAllTeams().values());
                    sortedTeams.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
                    
                    if (position > 0 && position <= sortedTeams.size()) {
                        Team team = sortedTeams.get(position - 1);
                        if (type.equals("name")) {
                            return team.getName();
                        } else if (type.equals("points")) {
                            return String.valueOf(team.getPoints());
                        } else if (type.equals("members")) {
                            return String.valueOf(team.getSize());
                        }
                    }
                    return type.equals("name") ? "---" : "0";
                } catch (NumberFormatException e) {
                    return "";
                }
            }
        }
        
        return null;
    }
    
    private Team getLeadingTeam(TeamManager teamManager) {
        Team leading = null;
        int maxPoints = -1;
        
        for (Team team : teamManager.getAllTeams().values()) {
            if (team.getPoints() > maxPoints) {
                maxPoints = team.getPoints();
                leading = team;
            }
        }
        
        return leading;
    }
    
    private String getColorCode(Team team) {
        org.bukkit.Color color = team.getArmorColor();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        if (r > 200 && g < 80 && b < 80) return "§c"; // Red
        if (r < 80 && g < 80 && b > 200) return "§9"; // Blue
        if (r < 80 && g > 200 && b < 80) return "§a"; // Green
        if (r > 200 && g > 200 && b < 80) return "§e"; // Yellow
        if (r > 150 && g < 80 && b > 150) return "§5"; // Purple
        if (r > 200 && g > 100 && b < 80) return "§6"; // Orange
        if (r > 200 && g < 150 && b > 200) return "§d"; // Pink
        if (r < 100 && g > 200 && b > 200) return "§b"; // Cyan
        if (r > 230 && g > 230 && b > 230) return "§f"; // White
        if (r < 50 && g < 50 && b < 50) return "§0"; // Black

        return "§f";
    }
}
