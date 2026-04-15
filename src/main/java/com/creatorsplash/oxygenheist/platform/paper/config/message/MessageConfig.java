package com.creatorsplash.oxygenheist.platform.paper.config.message;

import com.creatorsplash.oxygenheist.platform.paper.config.misc.SoundConfig;
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
    UiMessages ui,
    UiSymbols symbols
) {

    /* Value Types */

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
        SoundConfig startSound,
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
        TitleTimes titleTimes,
        SoundConfig tickSound
    ) {}

    /**
     * Titles, sounds, and chat messages for per-player combat events.
     */
    public record PlayerMessages(
        // Damage
        SoundConfig suffocatingSound,
        SoundConfig lowOxygenSound,

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
        SoundConfig eliminatedWorldSound,
        /* Broadcast on elimination. Placeholder: <player> */
        String eliminatedBroadcast,

        // Kills
        /* Chat message sent to attacker on kill reward. Placeholders: <points>, <player> */
        String killRewardAttacker,
        /* Chat message sent to attacker on captain kill bonus. Placeholders: <points>, <player> */
        String captainKillAttacker,
        /* Sent to attacker when they hit a teammate with FF disabled */
        String friendlyFireDenied,

        // Player <-> Weapon interactions
        String weaponInventoryFull,
        SoundConfig weaponInventoryFullSound
    ) {}

    /**
     * Chat messages for zone capture events
     *
     * <p>Placeholders: {@code <team>}, {@code <zone>}, {@code <amount>} where noted</p>
     */
    public record ZoneMessages(
        /* Broadcast on capture. Placeholders: <team>, <zone> */
        String captured,
        SoundConfig captureSound,
        SoundConfig contestedSound,
        SoundConfig capturingSound,
        /* Sent to capturing team members. Placeholders: <amount>, <zone> */
        String oxygenRestored,
        String oxygenDepletedTitle,
        String oxygenDepletedSubtitle,
        TitleTimes oxygenDepletedTimes
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
        String revivingBar,
        String waitingBar
    ) {}

    /**
     * Unicode/custom font characters used throughout the UI
     */
    public record UiSymbols(
        // Progress bars
        String barFilled,
        String barEmpty,
        // Zone status
        String zoneOwned,
        String zoneNeutral,
        String zoneCapturing,
        String zoneContested,
        String zoneOxygen,
        String zoneRefilling,
        // Downed / revive
        String downedWarning,
        String reviving,
        String smallBarFilled,
        String smallBarEmpty,
        // Pickup
        String pickupPrompt
    ) {}

}
