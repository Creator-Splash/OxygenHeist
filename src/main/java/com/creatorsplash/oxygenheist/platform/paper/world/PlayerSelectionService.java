package com.creatorsplash.oxygenheist.platform.paper.world;

import com.creatorsplash.oxygenheist.platform.paper.util.LocationFormatter;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Generic in-memory, per-player, two-point block selection using a wand item
 */
@RequiredArgsConstructor
public final class PlayerSelectionService implements Listener {

    static final String WAND_KEY = "selection_wand";

    private final JavaPlugin plugin;

    private final Map<UUID, Location> firstPoints = new HashMap<>();
    private final Map<UUID, Location> secondPoints = new HashMap<>();

    /* Event */

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.getClickedBlock() == null) return;
        if (!isWand(player.getInventory().getItemInMainHand())) return;
        if (!player.hasPermission("com.creatorsplash.oxygenheist.selection")) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);

            Location loc = event.getClickedBlock().getLocation();
            setFirstPoint(playerId, loc);

            player.sendRichMessage(
                "<green>Point 1 set <white>" + LocationFormatter.coords(loc)
            );

            if (hasSecondPoint(playerId)) {
                sendSelectionCompleteMessage(player);
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            Location loc = event.getClickedBlock().getLocation();
            setSecondPoint(playerId, loc);

            player.sendRichMessage(
                "<green>Point 2 set <white>" + LocationFormatter.coords(loc)
            );

            if (hasFirstPoint(playerId)) {
                sendSelectionCompleteMessage(player);
            }
        }
    }

    /* Selection */

    /**
     * Records the first selection point for the given player
     */
    public void setFirstPoint(UUID playerId, Location location) {
        firstPoints.put(playerId, location);
    }

    /**
     * Records the second selection point for the given player
     */
    public void setSecondPoint(UUID playerId, Location location) {
        secondPoints.put(playerId, location);
    }

    /**
     * @return true if the player has set only their first point
     */
    public boolean hasFirstPoint(UUID playerId) {
        return firstPoints.containsKey(playerId);
    }

    /**
     * @return true if the player has set only their second point
     */
    public boolean hasSecondPoint(UUID playerId) {
        return secondPoints.containsKey(playerId);
    }

    /**
     * Returns the first point set by this player, or empty if not yet set
     */
    public Optional<Location> getFirstPoint(UUID playerId) {
        return Optional.ofNullable(firstPoints.get(playerId));
    }

    /**
     * Returns the second point set by this player, or empty if not yet set
     */
    public Optional<Location> getSecondPoint(UUID playerId) {
        return Optional.ofNullable(secondPoints.get(playerId));
    }

    /**
     * Returns true if the player has set both selection points
     */
    public boolean hasSelection(UUID playerId) {
        return firstPoints.containsKey(playerId) && secondPoints.containsKey(playerId);
    }

    /**
     * Clears all pending selection points for the given player
     */
    public void clearSelection(UUID playerId) {
        firstPoints.remove(playerId);
        secondPoints.remove(playerId);
    }

    public void clear() {
        firstPoints.clear();
        secondPoints.clear();
    }

    /* Wand */

    /**
     * @return true if the given item is a selection wand
     */
    public boolean isWand(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
            wandKey(), PersistentDataType.BOOLEAN
        );
    }

    /**
     * @return a freshly created selection wand item
     */
    public ItemStack createWand() {
        ItemStack wand = ItemStack.of(Material.BLAZE_ROD);
        wand.editMeta(meta -> {
            meta.displayName(MM.item("<gold><bold>Selection Wand"));
            meta.lore(
                List.of(
                    MM.item("<gray>Left click block: <white>Set point 1"),
                    MM.item("<gray>Right click block: <white>Set point 2")
                )
            );

            meta.getPersistentDataContainer().set(wandKey(), PersistentDataType.BOOLEAN, true);

            meta.setEnchantmentGlintOverride(true);
        });

        return wand;
    }

    /* Internals */

    private NamespacedKey wandKey() {
        return new NamespacedKey(plugin, WAND_KEY);
    }

    private void sendSelectionCompleteMessage(Player player) {
        Component arenaCmd = Component.text("/oh arena set")
            .color(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)
            .hoverEvent(HoverEvent.showText(
                Component.text("Click to use")
                    .color(NamedTextColor.GRAY)
            ))
            .clickEvent(ClickEvent.suggestCommand("/oh arena set"));

        Component zoneCmd = Component.text("/oh zone set <name>")
            .color(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)
            .hoverEvent(HoverEvent.showText(
                Component.text("Click to use")
                    .color(NamedTextColor.GRAY)
            ))
            .clickEvent(ClickEvent.suggestCommand("/oh zone set "));

        Component message = Component.text("Both points set! ")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
            .append(arenaCmd)
            .append(Component.text(" or ").color(NamedTextColor.GRAY))
            .append(zoneCmd);

        player.sendMessage(message);
    }

}
