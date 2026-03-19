package com.creatorsplash.oxygenheist.domain.match;

/**
 * Represents the current lifecycle state of a match
 */
public enum MatchState {

    /**
     * No active match is running. Players may be joining or waiting
     */
    WAITING,

    /**
     * Match is preparing to start (e.g, countdown, setup)
     */
    STARTING,

    /**
     * Match is actively in progress
     */
    PLAYING,

    /**
     * Match has ended and is performing cleanup or displaying results
     */
    ENDING
}

