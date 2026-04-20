package com.creatorsplash.oxygenheist.domain.team;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a team
 */
@Getter
public class Team {

    private final String id;
    private final String name;

    @Setter
    private String color; // color tag

    @Setter
    @Nullable private TeamBase base;
    @Nullable private UUID captainId;

    private final List<UUID> members = new ArrayList<>();

    public Team(
        String id,
        String name,
        String color,
        @Nullable TeamBase base
    ) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.base = base;
    }

    /* Roster */

    public boolean addMember(UUID playerId) {
        if (members.contains(playerId)) return false;
        members.add(playerId);
        return true;
    }

    public boolean removeMember(UUID playerId) {
        boolean removed = members.remove(playerId);
        if (removed && playerId.equals(captainId)) {
            captainId = null;
        }
        return removed;
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean hasCaptain() {
        return captainId != null;
    }

    public boolean isCaptain(UUID playerId) {
        return captainId != null && captainId.equals(playerId);
    }

    public void setCaptain(UUID playerId) {
        this.captainId = playerId;
        if (!members.contains(playerId)) {
            members.add(playerId);
        }
    }

    public List<UUID> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public int getSize() {
        return members.size();
    }

    public boolean hasBase() {
        return base != null;
    }

    /* Snapshot */

    public TeamSnapshot toSnapshot(int matchScore) {
        return new TeamSnapshot(id, name, color, matchScore);
    }

}
