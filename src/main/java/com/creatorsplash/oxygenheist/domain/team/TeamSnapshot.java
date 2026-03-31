package com.creatorsplash.oxygenheist.domain.team;

/**
 * Read-only view of a team's state within an active match
 */
public record TeamSnapshot(
    String id,
    String name,
    String color,
    int score
) {
}
