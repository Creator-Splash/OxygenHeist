package com.creatorsplash.oxygenheist.platform.paper.config.misc;

import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
     * Convenience — plays this sound at the player's location
     */
    public void playTo(Player player) {
        player.playSound(
            Sound.sound(
                net.kyori.adventure.key.Key.key(key),
                source,
                volume,
                pitch
            )
        );
    }

    /* Load Helpers */

    /**
     * Reads a SoundConfig from a base key plus {@code -volume} and {@code -pitch} suffixes
     */
    public static SoundConfig sound(
        ConfigurationSection c,
        String key,
        String defaultKey,
        float defaultVolume,
        float defaultPitch
    ) {
        String soundKey = c.getString(key, defaultKey);
        if (soundKey == null) soundKey = defaultKey;
        float volume = (float) c.getDouble(key + "-volume", defaultVolume);
        float pitch = (float) c.getDouble(key + "-pitch", defaultPitch);
        Sound.Source source = parseSource(c.getString(key + "-source", "master"));
        return new SoundConfig(soundKey, source, volume, pitch);
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
