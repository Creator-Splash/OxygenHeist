package com.creatorsplash.oxygenheist.platform.paper.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Utility class for handling MiniMessage deserialization and placeholder resolution
 */
@UtilityClass
public class MM {

    /** MiniMessage instance for parsing and serializing components */
    public final MiniMessage CODEX = MiniMessage.miniMessage();

    private final String WHITE = "<white>";

    /**
     * Resolves a raw MiniMessage string into a component
     *
     * @param raw Raw MiniMessage string to deserialize
     * @return The resolved component
     */
    public Component msg(String raw) {
        if (raw == null) return Component.empty();
        return CODEX.deserialize(raw);
    }

    /**
     * Resolves a raw MiniMessage string into a component with custom tag resolution
     *
     * @param raw Raw MiniMessage string to deserialize
     * @param resolver Custom tag resolver to apply to the message
     * @return The resolved component
     */
    public Component msg(String raw, TagResolver resolver) {
        if (raw == null) return Component.empty();
        return CODEX.deserialize(raw, resolver);
    }

    /**
     * Resolves a raw MiniMessage string into a component with custom tag resolution
     *
     * @param raw Raw MiniMessage string to deserialize
     * @param resolvers Custom tag resolvers to apply to the message
     * @return The resolved component
     */
    public Component msg(String raw, TagResolver... resolvers) {
        if (raw == null) return Component.empty();
        return CODEX.deserialize(raw, TagResolver.resolver(resolvers));
    }

    /**
     * Resolves a raw MiniMessage string into a component with custom tag resolution
     * and applies the resolution based on the viewers context (player-specific)
     *
     * @param raw Raw MiniMessage string to deserialize
     * @param resolver Custom tag resolver to apply to the message
     * @param viewer The player viewing the message
     * @return The resolved component
     */
    public Component msg(String raw, TagResolver resolver, Player viewer) {
        if (raw == null) return Component.empty();
        return CODEX.deserialize(raw, TagResolver.resolver(
            resolver,
            PlaceholderUtils.playerResolver(viewer)
        ));
    }

    /**
     * Resolves a raw MiniMessage string into a component using a map of placeholder
     * <p>
     * The placeholders are automatically resolved using a utility method
     *
     * @param raw Raw MiniMessage string to deserialize
     * @param tags Map of placeholder tags and their values
     * @return The resolved component
     */
    public Component msg(String raw, Map<String, String> tags) {
        if (raw == null) return Component.empty();
        return CODEX.deserialize(raw, PlaceholderUtils.toResolver(tags));
    }



    /**
     * Resolves a raw MiniMessage string into a component using a map of placeholder
     * <p>
     * The placeholders are automatically resolved using a utility method
     *
     * @param raw Raw MiniMessage string to deserialize
     * @param tags Map of placeholder tags and their values
     * @param viewer The player viewing the message
     * @return The resolved component
     */
    public Component msg(String raw, Map<String, String> tags, Player viewer) {
        if (raw == null) return Component.empty();
        return CODEX.deserialize(raw, TagResolver.resolver(
            PlaceholderUtils.toResolver(tags),
            PlaceholderUtils.playerResolver(viewer)
        ));
    }

    public Component item(@NotNull String rawMessage) {
        return msg(WHITE + rawMessage).decoration(TextDecoration.ITALIC, false);
    }

    public Component item(@NotNull String rawMessage, Map<String, String> tags) {
        return msg(WHITE + rawMessage, tags).decoration(TextDecoration.ITALIC, false);
    }

    public Component item(@NotNull String rawMessage, TagResolver... tags) {
        return msg(WHITE + rawMessage, tags).decoration(TextDecoration.ITALIC, false);
    }

    public String strip(@NotNull String formattedMessage) { return CODEX.stripTags(formattedMessage); }

}
