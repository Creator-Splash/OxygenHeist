package com.creatorsplash.oxygenheist.platform.paper.util;

import com.creatorsplash.oxygenheist.domain.team.Team;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Applies and removes colored leather armor based on team color tags
 */
@UtilityClass
public class TeamArmorUtils {

    public void applyArmor(Player player, Team team) {
        Color color = colorTagToRgb(team.getColor());

        player.getInventory().setHelmet(coloredArmor(Material.LEATHER_HELMET, color));
        player.getInventory().setChestplate(coloredArmor(Material.LEATHER_CHESTPLATE, color));
        player.getInventory().setLeggings(coloredArmor(Material.LEATHER_LEGGINGS, color));
        player.getInventory().setBoots(coloredArmor(Material.LEATHER_BOOTS, color));
    }

    public void removeArmor(Player player) {
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
    }

    private ItemStack coloredArmor(Material material, org.bukkit.Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Color colorTagToRgb(String colorTag) {
        // Try named color first ("red", "dark_blue", "gold", etc.)
        TextColor textColor = NamedTextColor.NAMES.value(colorTag);

        if (textColor == null) {
            // Try hex - accept both "#RRGGBB" and "RRGGBB"
            String hex = colorTag.startsWith("#") ? colorTag : "#" + colorTag;
            textColor = TextColor.fromHexString(hex);
        }

        if (textColor == null) {
            textColor = NamedTextColor.WHITE;
        }

        return Color.fromRGB(textColor.value());
    }

}
