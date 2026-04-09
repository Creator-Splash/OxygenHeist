package com.creatorsplash.oxygenheist.platform.paper.config.message;

import com.creatorsplash.oxygenheist.platform.paper.config.misc.SoundConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.Supplier;

/**
 * Loads {@link MessageConfig} from {@code messages.yml} in the plugin data folder
 *
 * <p>Call {@link #load()} once during plugin startup. If the file does not
 * exist it is extracted from the bundled resource automatically</p>
 */
@RequiredArgsConstructor
public final class MessageConfigService implements Supplier<MessageConfig> {

    private static final String FILE_NAME = "messages.yml";

    private final JavaPlugin plugin;
    private volatile MessageConfig config = null;

    @Override
    public MessageConfig get() {
        return null;
    }

    /**
     * Extracts the default {@code messages.yml} if absent, then loads and caches the config
     */
    public void load() {
        plugin.saveResource(FILE_NAME, false);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), FILE_NAME)
        );
        this.config = parse(yml);
    }

    /* Parser */

    private static MessageConfig parse(YamlConfiguration c) {
        return new MessageConfig(
            parseMatch(c),
            parseCountdown(c),
            parsePlayer(c),
            parseZone(c),
            parseUi(c)
        );
    }

    private static MessageConfig.MatchMessages parseMatch(YamlConfiguration c) {
        return new MessageConfig.MatchMessages(
            s(c, "match.start", "<green><bold>LET THE GAMES BEGIN!"),
            s(c, "match.start-subtitle", "<gray>May the best team win"),
            times(c, "match.start", 0, 40, 20),
            s(c, "match.end", "<red><bold>GAME OVER!"),
            s(c, "match.team-wins", "<gold><bold><team> <green><bold>WINS!"),
            s(c, "match.team-wins-subtitle", "<gray>Congratulations!"),
            times(c, "match.team-wins", 0, 100, 40),
            s(c, "match.instant-death", "<red><bold>⚠ INSTANT DEATH MODE!"),
            s(c, "match.instant-death-subtitle", "<gray>No more revives!"),
            times(c, "match.instant-death", 10, 60, 20),
            SoundConfig.sound(c, "match.instant-death-sound", "entity.ender_dragon.growl", 1.0f, 1.0f),
            s(c, "match.time-warning", "<yellow><bold>Round ending in <time> seconds!")
        );
    }

    private static MessageConfig.CountdownMessages parseCountdown(YamlConfiguration c) {
        return new MessageConfig.CountdownMessages(
            s(c, "countdown.bossbar", "<yellow><bold>Game starting in <time> seconds"),
            s(c, "countdown.title", "<gold><bold><time>"),
            s(c, "countdown.subtitle", "<gray>Game starting soon!"),
            times(c, "countdown", 0, 25, 5)
        );
    }

    private static MessageConfig.PlayerMessages parsePlayer(YamlConfiguration c) {
        return new MessageConfig.PlayerMessages(
            // Downed
            s(c, "player.downed-title", "<red><bold>YOU'RE DOWN!"),
            s(c, "player.downed-subtitle", "<gray>Hold on! A teammate can revive you"),
            times(c, "player.downed", 0, 60, 20),
            SoundConfig.sound(c, "player.downed-sound", "entity.player.hurt", 1.0f, 0.5f),
            s(c, "player.downed-teammate-alert", "<yellow><bold><player> <gray>is down! Go revive them!"),
            s(c, "player.downed-attacker-alert", "<gray>You knocked down <yellow><player><gray>!"),

            // Revived
            s(c, "player.revived-title", "<green><bold>REVIVED!"),
            s(c, "player.revived-subtitle", "<gray>You're back in the fight!"),
            times(c, "player.revived", 0, 40, 10),
            SoundConfig.sound(c, "player.revived-sound", "entity.player.levelup", 1.0f, 1.5f),
            s(c, "player.revive-success", "<green><bold>You revived <yellow><player><green>!"),

            // Eliminated
            s(c, "player.eliminated-title", "<dark_red><bold>ELIMINATED"),
            s(c, "player.eliminated-subtitle-bleedout", "<gray>You've bled out!"),
            s(c, "player.eliminated-subtitle-instant", "<gray>You've been eliminated!"),
            times(c, "player.eliminated", 0, 60, 20),
            SoundConfig.sound(c, "player.eliminated-sound", "entity.wither.death", 0.5f, 1.0f),
            s(c, "player.eliminated-broadcast", "<dark_red><bold><player> <gray>has been eliminated!")
        );
    }

    private static MessageConfig.ZoneMessages parseZone(YamlConfiguration c) {
        return new MessageConfig.ZoneMessages(
            s(c, "zone.captured", "<green><bold><team> <gray>captured <yellow><zone><gray>!"),
            s(c, "zone.oxygen-restored", "<aqua><bold>+<amount> Oxygen! <reset><gray>Your team captured <zone>")
        );
    }

    private static MessageConfig.UiMessages parseUi(YamlConfiguration c) {
        return new MessageConfig.UiMessages(
            s(c, "ui.timer-bar-playing", "<yellow><bold><time>"),
            s(c, "ui.timer-bar-idle", "<gold><bold>No game in progress"),
            s(c, "ui.downed-bar", "<red><bold>DOWNED <reset><gray>- <yellow><time>s"),
            s(c, "ui.reviving-bar", "<green><bold>BEING REVIVED <reset><gray>- <yellow><progress>%")
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    /** Reads a string from config, falling back to {@code def} if absent or null */
    private static String s(YamlConfiguration c, String path, String def) {
        String val = c.getString(path);
        return val != null ? val : def;
    }

    /**
     * Reads TitleTimes from {@code <prefix>-title-fade-in}, {@code -stay}, {@code -fade-out}
     */
    private static MessageConfig.TitleTimes times(
        YamlConfiguration c,
        String prefix,
        int defaultFadeIn,
        int defaultStay,
        int defaultFadeOut
    ) {
        int fadeIn = c.getInt(prefix + "-title-fade-in", defaultFadeIn);
        int stay = c.getInt(prefix + "-title-stay", defaultStay);
        int fadeOut = c.getInt(prefix + "-title-fade-out", defaultFadeOut);
        return new MessageConfig.TitleTimes(fadeIn, stay, fadeOut);
    }

}
