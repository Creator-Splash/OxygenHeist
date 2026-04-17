package com.creatorsplash.oxygenheist.platform.paper.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utilities for building MiniMessage {@link TagResolver} instances
 */
@UtilityClass
public class PlaceholderUtils {

    private final Pattern TAG_PATTERN = Pattern.compile("[a-z0-9_-]+");

    /**
     * Builds a {@link TagResolver} from a map of placeholder keys and their string values
     *
     * <p>Keys are lowercased and validated against {@code [a-z0-9_-]+} before registration -
     * invalid keys are silently skipped rather than throwing. Null values are treated as
     * empty strings</p>
     *
     * <p>Values are deserialized as MiniMessage, so they can carry formatting tags</p>
     *
     * @param placeholders key-value map of placeholder names and their values
     * @return a resolver for all valid entries, or {@link TagResolver#empty()} if none
     */
    public TagResolver toResolver(@Nullable Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return TagResolver.empty();
        }

        TagResolver.Builder builder = TagResolver.builder();

        placeholders.forEach((key, value) -> {
            String formatted = key.toLowerCase();
            if (TAG_PATTERN.matcher(formatted).matches()) {
                builder.resolver(Placeholder.parsed(formatted, value == null ? "" : value));
            }
        });

        return builder.build();
    }

    /**
     * Builds a {@link TagResolver} populated with common player-specific tags
     *
     * <p>Provides the following tags out of the box:</p>
     * <ul>
     *   <li>{@code <player>} - the players name</li>
     *   <li>{@code <display_name>} - the players display name, may carry formatting</li>
     *   <li>{@code <uuid>} - the players UUID string</li>
     *   <li>{@code <world>} - the name of the world the player is currently in</li>
     * </ul>
     *
     * <p>Intended to be combined with other resolvers via
     * {@link TagResolver#resolver(TagResolver...)} rather than used in isolation.</p>
     *
     * @param player the player whose context is used to populate tags
     * @return a resolver containing the player-specific tags
     */
    public TagResolver playerResolver(Player player) {
        return TagResolver.resolver(
            Placeholder.parsed("player", player.getName()),
            Placeholder.component("display_name", player.displayName()),
            Placeholder.parsed("uuid", player.getUniqueId().toString()),
            Placeholder.parsed("player_id", player.getUniqueId().toString()),
            Placeholder.parsed("world", player.getWorld().getName())
        );
    }

}
