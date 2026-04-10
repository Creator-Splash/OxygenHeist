package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayService;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.domain.match.MatchState;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfig;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.misc.SoundConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Paper implementation for MatchDisplayService
 *
 * <p>Responsible for:
 * <ul>
 *     <li>Bossbars</li>
 *     <li>Titles</li>
 *     <li>Sounds</li>
 *     <li>ActionBars</li>
 *     <li>Messages</li>
 *     <li>Other UI</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
public final class PaperMatchDisplayService implements MatchDisplayService {

    private final OxygenHeistPlugin plugin;
    private final PaperAirBarController airBarController;
    private final MessageConfigService messages;

    /* State */

    /** Boss bar state - null when no match is active */
    private BossBar timerBar;

    /** Per-player downed boss bar, keyed by downed player UUID */
    private final Map<UUID, BossBar> downedBars = new HashMap<>();

    /** Tracks state from the previous render tick for transition detection */
    private @Nullable MatchState lastState = null;

    /** Time warnings (in seconds remaining) that have already fired this match */
    private final Set<Integer> firedTimeWarnings = new HashSet<>();

    private static final Set<Integer> TIME_WARNING_THRESHOLDS = Set.of(60, 30, 10);

    /* Rendering */

    @Override
    public void render(MatchSnapshot snapshot) {
        handleStateTransition(snapshot);

        switch (snapshot.state()) {
            case SETUP -> renderCountdown(snapshot);
            case PLAYING -> renderPlaying(snapshot);
            default -> { /* nothing */ }
        }

        renderPerPlayer(snapshot);

        // TODO zones?
    }

    private void handleStateTransition(MatchSnapshot snapshot) {
        if (snapshot.state() == lastState) return;

        if (snapshot.state() == MatchState.PLAYING && lastState == MatchState.SETUP) {
            // todo
        }
    }

    private void onCooldownComplete(MatchSnapshot snapshot) {
        // Switch boss bar to timer mode
        if (timerBar != null) {
            timerBar.color(BossBar.Color.YELLOW);
            timerBar.overlay(BossBar.Overlay.PROGRESS);
        }

        // Broadcast + title to all players
        broadcast(MM.msg(msg().match().start()));

        MessageConfig.MatchMessages m = msg().match();
        Title gameStartTitle = Title.title(
            MM.msg(m.start()),
            MM.msg(m.startSubtitle()),
            m.startTitleTimes().toAdventure()
        );

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.showTitle(gameStartTitle);
        }
    }

    private void renderCountdown(MatchSnapshot snapshot) {
        // Only fire once per second
        if (snapshot.remainingTicks() % 20 != 0) return;

        int secondsLeft = snapshot.remainingSeconds();
        String timeStr = String.valueOf(secondsLeft);

        // Update boss bar
        if (timerBar != null) {
            int countdownTotal = snapshot.config().countdownSeconds();
            float progress = countdownTotal > 0
                ? Math.clamp((float) secondsLeft / countdownTotal, 0f, 1f)
                : 1f;

            timerBar.name(MM.msg(msg().countdown().bossBar(),
                Map.of("time", timeStr)));
            timerBar.progress(progress);
        }

        // Show countdown title to all players
        MessageConfig.TitleTimes times = msg().countdown().titleTimes();
        Title countdownTitle = Title.title(
            MM.msg(msg().countdown().title(), Map.of("time", timeStr)),
            MM.msg(msg().countdown().subtitle()),
            times.toAdventure()
        );

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.showTitle(countdownTitle);
        }
    }

    private void renderPlaying(MatchSnapshot snapshot) {
        // Time warnings fire once at exactly 60 / 30 / 10 seconds
        int secondsLeft = snapshot.remainingSeconds();
        if (TIME_WARNING_THRESHOLDS.contains(secondsLeft)
                && !firedTimeWarnings.contains(secondsLeft)
                && snapshot.remainingTicks() % 20 == 0) {
            firedTimeWarnings.add(secondsLeft);
            broadcast(MM.msg(msg().match().timeWarning(),
                Map.of("time", String.valueOf(secondsLeft))));
        }

        // Update timer bar every second
        if (snapshot.remainingTicks() % 20 != 0) return;

        if (timerBar != null) {
            int totalTicks = snapshot.config().durationSeconds() * 20;
            float progress = totalTicks > 0
                    ? Math.clamp((float) snapshot.remainingTicks() / totalTicks, 0f, 1f)
                    : 0f;

            timerBar.name(MM.msg(msg().ui().timerBarPlaying(),
                    Map.of("time", formatTime(secondsLeft))));
            timerBar.progress(progress);

            // Switch bar to red during instant death
            if (snapshot.instantDeath()) {
                timerBar.color(BossBar.Color.RED);
            }
        }
    }

    private void renderPerPlayer(MatchSnapshot snapshot) {
        snapshot.players().forEach((id, playerSnap) -> {
            Player player = plugin.getServer().getPlayer(id);
            if (player == null || !player.isOnline()) return;

            // Air bar
            airBarController.update(player, playerSnap.oxygen(), playerSnap.maxOxygen());

            // Downed boss bar
            BossBar downedBar = downedBars.get(id);
            if (downedBar == null) return;

            int revivePercent = playerSnap.reviveProgressPercent();
            if (revivePercent > 0) {
                // Being revived - show progress
                downedBar.name(MM.msg(msg().ui().revivingBar(),
                        Map.of("progress", String.valueOf(revivePercent))));
                downedBar.color(BossBar.Color.GREEN);
                downedBar.progress(Math.clamp(revivePercent / 100f, 0f, 1f));
            } else {
                // Bleeding out - show time remaining
                int bleedoutTotal = snapshot.config().downed().bleedoutSeconds();
                int bleedoutTicks = playerSnap.bleedoutTicks();
                int secondsLeft = Math.max(0, bleedoutTicks / 20);

                float progress = bleedoutTotal > 0
                    ? Math.clamp((float) bleedoutTicks / (bleedoutTotal * 20), 0f, 1f)
                    : 0f;

                downedBar.name(MM.msg(msg().ui().downedBar(),
                    Map.of("time", String.valueOf(secondsLeft))));
                downedBar.color(BossBar.Color.RED);
                downedBar.progress(progress);
            }
        });
    }

    /* Lifecycle */

    @Override
    public void onMatchStarted() {
        firedTimeWarnings.clear();
        lastState = null;

        // Create and show the timer/countdown boss bar to all online players
        timerBar = BossBar.bossBar(
            MM.msg(msg().countdown().bossBar(), Map.of("time",
            String.valueOf(Integer.MAX_VALUE))), // will be updated on first render tick
            1f,
            BossBar.Color.YELLOW,
            BossBar.Overlay.PROGRESS
        );

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.showBossBar(timerBar);
        }
    }

    @Override
    public void onMatchEnd(String winner) {
        // Broadcast result
        broadcast(MM.msg(msg().match().end()));

        if (!winner.isEmpty()) {
            broadcast(MM.msg(msg().match().teamWins(), Map.of("team", winner)));
        }

        // Winner title to all online players
        if (!winner.isEmpty()) {
            MessageConfig.MatchMessages m = msg().match();
            Title winnerTitle = Title.title(
                MM.msg(m.teamWins(), Map.of("team", winner)),
                MM.msg(m.teamWinsSubtitle()),
                m.teamWinsTitleTimes().toAdventure()
            );
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.showTitle(winnerTitle);
            }
        }

        clearAll();
    }

    @Override
    public void clearAll() {
        // Remove timer bar from all players
        if (timerBar != null) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.hideBossBar(timerBar);
            }
            timerBar = null;
        }

        // Remove all downed bars
        for (Map.Entry<UUID, BossBar> entry : downedBars.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        downedBars.clear();

        airBarController.clearAll();
        firedTimeWarnings.clear();
        lastState = null;
    }

    @Override
    public void onZoneCaptured(
        String teamId,
        String teamName,
        String zoneName,
        int oxygenRestored,
        Set<UUID> teamMemberIds
    ) {
        // Broadcast capture to everyone
        broadcast(MM.msg(msg().zone().captured(),
            Map.of("team", teamName, "zone", zoneName)));

        // Oxygen restore message only to capturing team
        Component oxygenMsg = MM.msg(msg().zone().oxygenRestored(),
            Map.of("amount", String.valueOf(oxygenRestored), "zone", zoneName));

        for (UUID memberId : teamMemberIds) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(oxygenMsg);
            }
        }
    }

    /* Player Hooks */

    @Override
    public void onPlayerDowned(
        UUID victimId,
        @Nullable UUID attackerId,
        Set<UUID> teammateIds
    ) {
        Player victim = plugin.getServer().getPlayer(victimId);
        MessageConfig.PlayerMessages p = msg().player();

        // Title + sound to downed player
        if (victim != null && victim.isOnline()) {
            victim.showTitle(Title.title(
                MM.msg(p.downedTitle()),
                MM.msg(p.downedSubtitle()),
                p.downedTimes().toAdventure()
            ));
            playSound(victim, p.downedSound());

            // Create downed boss bar for this player
            BossBar bar = BossBar.bossBar(
                MM.msg(p.downedTitle()),
                1f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
            );
            victim.showBossBar(bar);
            downedBars.put(victimId, bar);
        }

        // Notify teammates
        String victimName = victim != null ? victim.getName() : victimId.toString();
        Component teammateAlert = MM.msg(p.downedTeammateAlert(), Map.of("player", victimName));

        for (UUID teammateId : teammateIds) {
            Player teammate = plugin.getServer().getPlayer(teammateId);
            if (teammate != null && teammate.isOnline()) {
                teammate.sendMessage(teammateAlert);
            }
        }

        // Notify attacker
        if (attackerId != null) {
            Player attacker = plugin.getServer().getPlayer(attackerId);
            if (attacker != null && attacker.isOnline()) {
                attacker.sendMessage(MM.msg(p.downedAttackerAlert(),
                    Map.of("player", victimName)));
            }
        }
    }

    @Override
    public void onPlayerRevived(UUID downedId, UUID reviverId) {
        Player revived = plugin.getServer().getPlayer(downedId);
        Player reviver = plugin.getServer().getPlayer(reviverId);
        MessageConfig.PlayerMessages p = msg().player();

        // Remove downed bar
        removeDDownedBar(downedId);

        // Title + sound to revived player
        if (revived != null && revived.isOnline()) {
            revived.showTitle(Title.title(
                MM.msg(p.revivedTitle()),
                MM.msg(p.revivedSubtitle()),
                p.revivedTimes().toAdventure()
            ));
            playSound(revived, p.revivedSound());
        }

        // Confirm message to reviver
        if (reviver != null && reviver.isOnline()) {
            String revivedName = revived != null ? revived.getName() : downedId.toString();
            reviver.sendMessage(MM.msg(p.reviveSuccess(), Map.of("player", revivedName)));
        }
    }

    @Override
    public void onPlayerEliminated(UUID playerId, boolean wasInstantDeath) {
        Player player = plugin.getServer().getPlayer(playerId);
        MessageConfig.PlayerMessages p = msg().player();

        // Clean up downed bar if present
        removeDDownedBar(playerId);

        // Title + sound to eliminated player
        if (player != null && player.isOnline()) {
            String subtitle = wasInstantDeath
                ? p.eliminatedSubtitleInstant()
                : p.eliminatedSubtitleBleedout();

            player.showTitle(Title.title(
                MM.msg(p.eliminatedTitle()),
                MM.msg(subtitle),
                p.eliminatedTimes().toAdventure()
            ));
            playSound(player, p.eliminatedSound());
        }

        // Broadcast to server
        String name = player != null ? player.getName() : playerId.toString();
        broadcast(MM.msg(p.eliminatedBroadcast(), Map.of("player", name)));

        // Clean up air bar
        if (player != null) {
            airBarController.remove(player);
        }
    }

    @Override
    public void onPlayerRemoved(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);

        removeDDownedBar(playerId);

        if (player != null) {
            airBarController.remove(player);
            if (timerBar != null) {
                player.hideBossBar(timerBar);
            }
        }
    }

    @Override
    public void onInstantDeathActivated() {
        MessageConfig.MatchMessages m = msg().match();
        broadcast(MM.msg(m.instantDeath()));
        Title title = Title.title(
            MM.msg(m.instantDeath()),
            MM.msg(m.instantDeathSubtitle()),
            m.instantDeathTitleTimes().toAdventure()
        );
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.showTitle(title);
            playSound(player, m.instantDeathSound());
        }
        if (timerBar != null) {
            timerBar.color(BossBar.Color.RED);
        }
    }

    @Override
    public void showBarsToNewPlayer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        if (timerBar != null) player.showBossBar(timerBar);
    }

    /* Internals/Helpers */


    private void removeDDownedBar(UUID playerId) {
        BossBar bar = downedBars.remove(playerId);
        if (bar == null) return;

        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            player.hideBossBar(bar);
        }
    }

    private MessageConfig msg() {
        return messages.get();
    }

    private void broadcast(Component message) {
        plugin.getServer().broadcast(message);
    }

    private void playSound(Player player, SoundConfig sound) {
        sound.playTo(player);
    }

    private static String formatTime(int totalSeconds) {
        int mins = Math.max(0, totalSeconds) / 60;
        int secs = Math.max(0, totalSeconds) % 60;
        return String.format("%02d:%02d", mins, secs);
    }

}
