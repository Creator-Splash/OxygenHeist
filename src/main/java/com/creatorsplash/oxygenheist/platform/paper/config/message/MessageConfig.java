package com.creatorsplash.oxygenheist.platform.paper.config.message;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;

import java.time.Duration;

/**
 * Typed, immutable snapshot of all player-facing messages and display configuration
 *
 * <p>Structure mirrors {@code messages.yml} with one level of nesting per concern:</p>
 * <ul>
 *   <li>{@link MatchMessages} - lifecycle broadcasts and titles</li>
 *   <li>{@link CountdownMessages} - countdown boss bar and titles</li>
 *   <li>{@link PlayerMessages} - downed, revived, and eliminated feedback</li>
 *   <li>{@link ZoneMessages} - zone capture and oxygen restore feedback</li>
 *   <li>{@link UiMessages} - persistent boss bar format strings</li>
 * </ul>
 */
public record MessageConfig(
    MatchMessages match,
    CountdownMessages countdown,
    PlayerMessages player,
    ZoneMessages zone,
    UiMessages ui
) {

    /* Value Types */

    /**
     * A sound event with volume, pitch, and mixer source
     *
     * <p>{@code key} accepts both vanilla and resource pack sound IDs:
     * <ul>
     *   <li>Vanilla: {@code entity.player.hurt} - {@code minecraft:} namespace is implied</li>
     *   <li>Resource pack: {@code mypack:weapons.claw.fire} - explicit namespace required</li>
     * </ul>
     * </p>
     *
     * <p>{@code source} controls which mixer channel the sound is routed to
     * (e.g. {@link Sound.Source#PLAYER},
     * {@link Sound.Source#HOSTILE}).
     * Defaults to {@link Sound.Source#MASTER} if unspecified</p>
     */
    public record SoundConfig(String key, Sound.Source source, float volume, float pitch) {}

    /**
     * Title fade-in / stay / fade-out durations in ticks
     */
    public record TitleTimes(int fadeInTicks, int stayTicks, int fadeOutTicks) {
        public Title.Times toAdventure() {
            return Title.Times.times(
                ticksToDuration(fadeInTicks),
                ticksToDuration(stayTicks),
                ticksToDuration(fadeOutTicks)
            );
        }

        private static Duration ticksToDuration(int ticks) {
            return Duration.ofMillis(ticks * 50L);
        }
    }

    /* Section Records */

    /**
     * Broadcast and title messages for match lifecycle events.
     *
     * <p>Placeholders: {@code <team>} where noted.</p>
     */
    public record MatchMessages(
        /* Broadcast on match start */
        String start,
        String startSubtitle,
        TitleTimes startTitleTimes,
        /* Broadcast on match end */
        String end,
        /* Broadcast when a team wins. Placeholder: <team> */
        String teamWins,
        String teamWinsSubtitle,
        TitleTimes teamWinsTitleTimes,
        /* Broadcast + title when instant death activates */
        String instantDeath,
        String instantDeathSubtitle,
        TitleTimes instantDeathTitleTimes,
        SoundConfig instantDeathSound,
        /* Broadcast at 60s / 30s / 10s remaining. Placeholder: <time> */
        String timeWarning
    ) {}

    /**
     * Boss bar and title display during the pre-match countdown
     *
     * <p>Placeholder: {@code <time>} (integer seconds remaining)</p>
     */
    public record CountdownMessages(
        /* Boss bar text during countdown */
        String bossBar,
        /* Large countdown title */
        String title,
        /* Countdown subtitle */
        String subtitle,
        TitleTimes titleTimes
    ) {}

    /**
     * Titles, sounds, and chat messages for per-player combat events.
     */
    public record PlayerMessages(
        // Downed
        String downedTitle,
        String downedSubtitle,
        TitleTimes downedTimes,
        SoundConfig downedSound,
        /* Chat message sent to teammates. Placeholder: <player> */
        String downedTeammateAlert,
        /* Chat message sent to the attacker. Placeholder: <player> */
        String downedAttackerAlert,

        // Revived
        String revivedTitle,
        String revivedSubtitle,
        TitleTimes revivedTimes,
        SoundConfig revivedSound,
        /* Chat message sent to the reviver. Placeholder: <player> */
        String reviveSuccess,

        // Eliminated
        String eliminatedTitle,
        /* Subtitle shown when eliminated from a downed bleedout */
        String eliminatedSubtitleBleedout,
        /* Subtitle shown when eliminated instantly (instant death mode) */
        String eliminatedSubtitleInstant,
        TitleTimes eliminatedTimes,
        SoundConfig eliminatedSound,
        /* Broadcast on elimination. Placeholder: <player> */
        String eliminatedBroadcast
    ) {}

    /**
     * Chat messages for zone capture events
     *
     * <p>Placeholders: {@code <team>}, {@code <zone>}, {@code <amount>} where noted</p>
     */
    public record ZoneMessages(
        /* Broadcast on capture. Placeholders: <team>, <zone> */
        String captured,
        /* Sent to capturing team members. Placeholders: <amount>, <zone> */
        String oxygenRestored
    ) {}

    /**
     * Format strings for persistent boss bar UI elements
     */
    public record UiMessages(
        /* Timer bar during an active match. Placeholder: <time> (MM:SS) */
        String timerBarPlaying,
        /* Timer bar when no match is running */
        String timerBarIdle,
        /* Per-player downed bar. Placeholder: <time> (seconds remaining) */
        String downedBar,
        /* Per-player reviving bar. Placeholder: <progress> (0–100) */
        String revivingBar
    ) {}

}
