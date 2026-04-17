package com.creatorsplash.oxygenheist.platform.paper.config.misc;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
public record SoundConfig(String key, Sound.Source source, float volume, float pitch) {

    /**
     * Convenience - plays this sound at the player's location
     */
    public void playTo(Player player) {
        player.playSound(
            Sound.sound(
                Key.key(key),
                source,
                volume,
                pitch
            )
        );
    }

    /**
     * Plays to a set of players at their respective locations
     */
    public void playTo(Iterable<? extends Player> players) {
        Sound sound = Sound.sound(
            Key.key(key),
            source, volume, pitch
        );
        for (Player player : players) {
            player.playSound(sound);
        }
    }

    /**
     * Plays at a specific world location - heard by nearby players based on volume
     */
    public void playAt(Location location) {
        Sound sound = Sound.sound(
            Key.key(key),
            source, volume, pitch
        );
        location.getWorld().playSound(sound, location.getX(), location.getY(), location.getZ());
    }

    /**
     * Plays to all online players regardless of location - for global announcements
     */
    public void playGlobal() {
        Sound sound = Sound.sound(
            Key.key(key),
            source, volume, pitch
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(sound);
        }
    }

    /* Load Helpers */

    /**
     * Reads a SoundConfig from a config section
     */
    public static @Nullable SoundConfig sound(ConfigurationSection full, String key) {
        ConfigurationSection sound = full.getConfigurationSection(key);
        if (sound == null) return null;

        return new SoundConfig(
            sound.getString("sound"),
            parseSource(sound.getString("source", "master")),
            (float) sound.getDouble("volume", 1.0),
            (float) sound.getDouble("pitch", 1.0)
        );
    }

    /**
     * Reads a SoundConfig from a config section
     */
    public static SoundConfig from(ConfigurationSection c) {
        return new SoundConfig(
            c.getString("sound"),
            parseSource(c.getString("source", "master")),
            (float) c.getDouble("volume", 1.0),
            (float) c.getDouble("pitch", 1.0)
        );
    }

    public static Sound.Source parseSource(String raw) {
        if (raw == null) return Sound.Source.MASTER;
        return switch (raw.toLowerCase()) {
            case "music" -> Sound.Source.MUSIC;
            case "record" -> Sound.Source.RECORD;
            case "weather" -> Sound.Source.WEATHER;
            case "block" -> Sound.Source.BLOCK;
            case "hostile" -> Sound.Source.HOSTILE;
            case "neutral" -> Sound.Source.NEUTRAL;
            case "player" -> Sound.Source.PLAYER;
            case "ambient" -> Sound.Source.AMBIENT;
            case "voice" -> Sound.Source.VOICE;
            default -> Sound.Source.MASTER;
        };
    }

}
