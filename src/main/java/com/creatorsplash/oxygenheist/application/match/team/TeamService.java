package com.creatorsplash.oxygenheist.application.match.team;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.team.Team;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manages the team registry and persistent player-team assignments
 *
 * <p>Team assignments persist between matches. Match-scoped state
 * (scores, alive status) lives in {@link MatchSession}</p>
 */
public class TeamService {

    private final Map<String, Team> teams = new LinkedHashMap<>();

    /**
     * Inverse index: playerId -> teamId
     * Rebuilt from team member lists on construction
     */
    private final Map<UUID, String> playerTeams = new HashMap<>();

    @Getter private final boolean friendlyFireEnabled;
    @Getter private final int maxTeamSize;
    @Getter private final String waitingBarMessage;

    public TeamService(
        List<Team> teams,
        boolean friendlyFireEnabled,
        int maxTeamSize,
        String waitingBarMessage
    ) {
        for (Team team : teams) {
            this.teams.put(team.getId(), team);
            for (UUID memberId : team.getMembers()) {
                playerTeams.put(memberId, team.getId());
            }
        }
        this.friendlyFireEnabled = friendlyFireEnabled;
        this.maxTeamSize = maxTeamSize;
        this.waitingBarMessage = waitingBarMessage;
    }

    /* Queries */

    public @Nullable Team getTeam(String teamId) {
        return teams.get(teamId.toLowerCase());
    }

    public @Nullable Team getPlayerTeam(UUID playerId) {
        String teamId = playerTeams.get(playerId);
        return teamId != null ? teams.get(teamId) : null;
    }

    public @Nullable String getPlayerTeamId(UUID playerId) {
        return playerTeams.get(playerId);
    }

    public Collection<Team> getAllTeams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    public boolean isOnTeam(UUID playerId) {
        return playerTeams.containsKey(playerId);
    }

    public boolean areTeammates(UUID a, UUID b) {
        String teamA = playerTeams.get(a);
        String teamB = playerTeams.get(b);
        return teamA != null && teamA.equals(teamB);
    }

    /* Mutations */

    /**
     * Adds a player to a team
     *
     * @return false if the player is already on a team, the team doesn't exist, or the team is full
     */
    public boolean addPlayerToTeam(UUID playerId, String teamId) {
        if (playerTeams.containsKey(playerId)) return false;

        Team team = teams.get(teamId.toLowerCase());
        if (team == null) return false;
        if (team.getSize() >= maxTeamSize) return false;

        team.addMember(playerId);
        playerTeams.put(playerId, teamId.toLowerCase());
        return true;
    }

    /**
     * Removes a player from their current team
     *
     * @return false if the player is not on any team
     */
    public boolean removePlayerFromTeam(UUID playerId) {
        String teamId = playerTeams.remove(playerId);
        if (teamId == null) return false;

        Team team = teams.get(teamId);
        if (team != null) team.removeMember(playerId);
        return true;
    }

    /**
     * Sets the captain of a team, adding them as a member if needed
     *
     * @return false if the team doesn't exist or the player couldn't be added
     */
    public boolean setCaptain(String teamId, UUID playerId) {
        Team team = teams.get(teamId.toLowerCase());
        if (team == null) return false;

        if (!team.isMember(playerId) && !addPlayerToTeam(playerId, teamId)) return false;

        team.setCaptain(playerId);
        return true;
    }

    /* Match integration */

    /**
     * Seeds a match session with current persistent team assignments
     *
     * <p>Called during match start. Players not in any team are simply
     * absent from the session's team map</p>
     */
    public void populateMatchSession(MatchSession session) {
        playerTeams.forEach(session::assignPlayerTeam);
    }

}
