package com.creatorsplash.oxygenheist.platform.paper.bootstrap;

import com.creatorsplash.oxygenheist.platform.paper.command.CommandHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.meta.CommandMeta;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class CommandRegistrar extends LegacyPaperCommandManager<CommandSender> {

    private final AnnotationParser<CommandSender> annotationParser;

    public CommandRegistrar(@NotNull final Plugin plugin) throws InitializationException {
        super(
            plugin,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.identity()
        );

        this.annotationParser = new AnnotationParser<>(
            this,
            CommandSender.class,
            params -> CommandMeta.empty()
        );

        // Exception handlers
        this.registerDefaultExceptionHandlers(
            triplet -> {
                final CommandSender sender = this.senderMapper().reverse(triplet.first().sender());
                final String message = triplet.first().formatCaption(triplet.second(), triplet.third());
                sender.sendMessage(Component.text(message, NamedTextColor.RED));
            },
            pair ->
                plugin.getLogger().log(Level.SEVERE, pair.first(), pair.second())
        );
    }

    /**
     * Registers a class-based command annotated with {@link org.incendo.cloud.annotations.Command}
     *
     * @param command the command instance to register
     */
    public void registerAnnotated(@NotNull final CommandHandler command) {
        this.annotationParser.parse(command);
    }

}
