package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.world.ArenaSetup;
import com.creatorsplash.oxygenheist.platform.paper.world.PlayerSelectionService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

import java.util.UUID;

@Command("oxygenheist|oh")
@Permission("com.creatorsplash.oxygenheist.admin")
@RequiredArgsConstructor
public final class SetupCommands implements CommandHandler {

    private final LogCenter log;
    private final PlayerSelectionService selectionService;
    private final ArenaConfigService arenaConfigService;

    /* Selection Wand */

    @Command("wand")
    @CommandDescription("Get the selection wand")
    public void wand(Player player) {
        player.getInventory().addItem(selectionService.createWand());
        player.sendRichMessage(
            "<gold>Selection wand given!" +
            "<gray>Hover item for instructions"
        );
    }

    @Command("wand clear")
    @CommandDescription("Clear your current selection")
    public void clearSelection(Player player) {
        selectionService.clearSelection(player.getUniqueId());
        player.sendRichMessage("<yellow>Selection cleared");
    }

    /* Arena Setup */

    @Command("arena set")
    @CommandDescription("Confirm the current selection as the arena and save it")
    public void arenaSet(Player player) {
        UUID playerId = player.getUniqueId();

        if (!selectionService.hasSelection(playerId)) {
            player.sendRichMessage(
                "<red>No selection found. Use <white>/oh wand</white> and click two points first"
            );
            return;
        }

        Location p1 = selectionService.getFirstPoint(playerId).orElseThrow();
        Location p2 = selectionService.getSecondPoint(playerId).orElseThrow();

        if (!p1.getWorld().equals(p2.getWorld())) {
            player.sendRichMessage("<red>Both points must be in the same world!");
            return;
        }

        double centerX = (p1.getX() + p2.getX()) / 2.0;
        double centerZ = (p1.getZ() + p2.getZ()) / 2.0;
        double initialSize = Math.max(
            Math.abs(p1.getX() - p2.getX()),
            Math.abs(p1.getZ() - p2.getZ())
        );

        ArenaSetup arena = new ArenaSetup(
            p1.getWorld().getName(),
            centerX,
            centerZ,
            initialSize
        );

        try {
            arenaConfigService.save(arena);
            selectionService.clearSelection(playerId);

            player.sendRichMessage(
                "<green>Arena saved! " +
                "<gray>World: <white>" + arena.worldName() +
                " <gray>Center: <white>" + (int) centerX + ", " + (int) centerZ +
                " <gray>Size: <white>" + (int) initialSize
            );
        } catch (RuntimeException e) {
            log.error("Failed to save arena to config", e);
            player.sendRichMessage("<red>Failed to save arena - check console for details");
        }
    }

    @Command("arena info")
    @CommandDescription("Show the current arena configuration")
    public void arenaInfo(CommandSender sender) {
        if (!arenaConfigService.isConfigured()) {
            sender.sendRichMessage(
                "<yellow>No arena configured yet. Use <white>/oh wand</white> in-game to get started"
            );
            return;
        }

        ArenaSetup arena = arenaConfigService.get().orElseThrow();
        sender.sendRichMessage("<gold>Arena Info");
        sender.sendRichMessage("<gray>World: <white>" + arena.worldName());
        sender.sendRichMessage(
            "<gray>Center: <white>" + (int) arena.centerX() + ", " + (int) arena.centerZ()
        );
        sender.sendRichMessage("<gray>Size: <white>" + (int) arena.initialSize() + " blocks");
    }

    // TODO zone here?

}
