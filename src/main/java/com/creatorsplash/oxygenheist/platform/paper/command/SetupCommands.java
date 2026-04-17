package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.domain.match.config.ExclusionZone;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfig;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.world.ArenaSetup;
import com.creatorsplash.oxygenheist.platform.paper.world.PlayerSelectionService;
import com.creatorsplash.oxygenheist.platform.paper.world.ZoneSelectionService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Command("oxygenheist|oh")
@Permission("com.creatorsplash.oxygenheist.admin")
@RequiredArgsConstructor
public final class SetupCommands implements CommandHandler {

    private static final int ZONE_MAX = 50;

    private final LogCenter log;
    private final PlayerSelectionService selectionService;
    private final ZoneSelectionService zoneSelectionService;
    private final GlobalConfigService globals;
    private final ArenaConfigService arenaConfigService;

    /* Selection Wand */

    @Command("wand")
    @CommandDescription("Get the selection wand")
    public void wand(Player player) {
        player.getInventory().addItem(selectionService.createWand());
        player.sendRichMessage(
            "<gold>Selection wand given!\n" +
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
            arenaConfigService.saveArena(arena);
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
        if (!arenaConfigService.isArenaConfigured()) {
            sender.sendRichMessage(
                "<yellow>No arena configured yet. Use <white>/oh wand</white> in-game to get started"
            );
            return;
        }

        ArenaSetup arena = arenaConfigService.getArena().orElseThrow();
        sender.sendRichMessage("<gold>Arena Info");
        sender.sendRichMessage("<gray>World: <white>" + arena.worldName());
        sender.sendRichMessage(
            "<gray>Center: <white>" + (int) arena.centerX() + ", " + (int) arena.centerZ()
        );
        sender.sendRichMessage("<gray>Size: <white>" + (int) arena.initialSize() + " blocks");
    }

    /* Zones setup */

    @Command("zone set <id> [displayName]")
    @CommandDescription("Create a cuboid zone from your wand selection")
    public void zoneSet(
        Player player,
        @Argument("id") String id,
        @Argument("displayName") @Nullable String displayName
    ) {
        String safeId = id.toLowerCase();
        String name = displayName != null ? displayName : safeId;

        Optional<ZoneDefinition.Cuboid> zone =
            zoneSelectionService.confirmCuboid(player.getUniqueId(), safeId, name);

        if (zone.isEmpty()) {
            player.sendRichMessage(
                "<red>No complete selection found. Use <white>/oh wand</white>" +
                    " and set both points, then run this command");
            player.sendRichMessage("<red>Or use <gray>/oh zone setcircle</gray> " +
                "to use a single selection point and radius");
            return;
        }

        try {
            ZoneDefinition.Cuboid c = zone.get();

            arenaConfigService.saveZone(c);
            zoneSelectionService.clearSelection(player.getUniqueId());

            player.sendRichMessage(
                "<green>Zone '<white>" + safeId + "</white>' saved! " +
                "<gray>(" + (int) c.minX() + ", " + (int) c.minY() + ", " + (int) c.minZ() + ")" +
                " -> (" + (int) c.maxX() + ", " + (int) c.maxY() + ", " + (int) c.maxZ() + ")"
            );
        } catch (RuntimeException e) {
            log.error("Failed to save zone config", e);
            player.sendRichMessage("<red>Failed to save zone - check console for details");
        }
    }

    @Command("zone setcircle <id> <radius> [displayName]")
    @CommandDescription("Create a circle zone from your first wand point, and a radius")
    public void zoneSetCircle(
        Player player,
        @Argument("id") String id,
        @Argument("radius") double radius,
        @Argument("displayName") @Nullable String displayName
    ) {
        if (radius <= 0 || radius > ZONE_MAX) {
            player.sendRichMessage("<red>Radius must be between 1 and " + ZONE_MAX);
            return;
        }

        String safeId = id.toLowerCase();
        String name = displayName != null ? displayName : safeId;

        Optional<ZoneDefinition.Circle> zone =
            zoneSelectionService.confirmCircle(player.getUniqueId(), safeId, name, radius);

        if (zone.isEmpty()) {
            player.sendRichMessage(
                "<red>No first point found. Use <white>/oh wand</white>" +
                " and left-click a block to set your center point"
            );
            return;
        }

        try {
            arenaConfigService.saveZone(zone.get());
            zoneSelectionService.clearSelection(player.getUniqueId());

            ZoneDefinition.Circle c = zone.get();
            player.sendRichMessage(
                "<green>Zone '<white>" + safeId + "</white>' saved! " +
                "<gray>Center: (" + (int) c.centerX() + ", " + (int) c.centerY() +
                ", " + (int) c.centerZ() + ") Radius: <white>" + (int) radius
            );
        } catch (RuntimeException e) {
            log.error("Failed to save zone config", e);
            player.sendRichMessage("<red>Failed to save zone - check console for details");
        }
    }

    /**
     * Removes a zone by id
     */
    @Command("zone remove <id>")
    @CommandDescription("Remove a zone by id")
    public void zoneRemove(CommandSender sender, @Argument("id") String id) {
        String safeId = id.toLowerCase();

        if (arenaConfigService.removeZone(safeId)) {
            sender.sendRichMessage("<green>Zone '<white>" + safeId + "</white>' removed");
        } else {
            sender.sendRichMessage("<red>No zone found with id '<white>" + safeId + "</white>'");
        }
    }

    /**
     * Lists all configured zones
     */
    @Command("zone list")
    @CommandDescription("List all configured zones")
    public void zoneList(CommandSender sender) {
        List<ZoneDefinition> zones = arenaConfigService.getZones();

        if (zones.isEmpty()) {
            sender.sendRichMessage(
                "<yellow>No zones configured yet. " +
                "Use <white>/oh wand</white> and <white>/oh zone set</white> to create one."
            );
            return;
        }

        sender.sendRichMessage("<gold>Configured Zones <gray>(" + zones.size() + ")");

        for (ZoneDefinition zone : zones) {
            String shape = switch (zone) {
                case ZoneDefinition.Circle c  -> "circle r=" + (int) c.radius();
                case ZoneDefinition.Cuboid c  -> "cuboid";
            };

            sender.sendRichMessage(
                "<gray> - <white>" + zone.id() +
                " <dark_gray>(" + shape + ")" +
                " <gray>'" + zone.displayName() + "'" +
                " <dark_gray>@ " + zone.worldName()
            );
        }
    }

    /* Weapon Setup */

    @Command("weapon-exclusion add <id>")
    @CommandDescription("Create a weapon spawn exclusion zone from your wand selection")
    public void weaponExclusionAdd(
        Player player,
        @Argument("id") String id
    ) {
        UUID playerId = player.getUniqueId();
        if (!selectionService.hasSelection(playerId)) {
            player.sendRichMessage("<red>No selection found. Use <white>/oh wand</white> and click two points first");
            return;
        }

        Location p1 = selectionService.getFirstPoint(playerId).orElseThrow();
        Location p2 = selectionService.getSecondPoint(playerId).orElseThrow();

        if (!p1.getWorld().equals(p2.getWorld())) {
            player.sendRichMessage("<red>Both points must be in the same world");
            return;
        }

        arenaConfigService.saveExclusionZone(new ExclusionZone(
            id.toLowerCase(),
            p1.getWorld().getName(),
            Math.min(p1.getX(), p2.getX()), Math.min(p1.getZ(), p2.getZ()),
            Math.max(p1.getX(), p2.getX()), Math.max(p1.getZ(), p2.getZ())
        ));

        selectionService.clearSelection(playerId);
        player.sendRichMessage("<green>Exclusion zone '<white>" + id + "</white>' saved");
    }

    @Command("weapon-exclusion remove <id>")
    @CommandDescription("Remove a weapon spawn exclusion zone")
    public void weaponExclusionRemove(CommandSender sender, @Argument("id") String id) {
        if (arenaConfigService.removeExclusionZone(id.toLowerCase())) {
            sender.sendRichMessage("<green>Exclusion zone '<white>" + id + "</white>' removed");
        } else {
            sender.sendRichMessage("<red>No exclusion zone found with id '<white>" + id + "</white>'");
        }
    }

    @Command("weapon-exclusion list")
    @CommandDescription("List all weapon spawn exclusion zones")
    public void weaponExclusionList(CommandSender sender) {
        var zones = arenaConfigService.getExclusionZones();
        if (zones.isEmpty()) {
            sender.sendRichMessage("<gray>No exclusion zones configured");
            return;
        }
        sender.sendRichMessage("<dark_gray>------- <white>Exclusion Zones <dark_gray>-------");
        for (var z : zones) {
            sender.sendRichMessage("<gray>" + z.id() + " <white>(" + z.world() + ") "
                + (int)z.minX() + "," + (int)z.minZ() + " -> " + (int)z.maxX() + "," + (int)z.maxZ());
        }
    }

}
