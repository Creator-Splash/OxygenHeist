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
import org.bukkit.persistence.PersistentDataType;

/**
 * Team utils
 */
@UtilityClass
public class TeamUtils {

    public boolean isTeamArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta()
            .getPersistentDataContainer()
            .has(PDCKeys.TEAM_ARMOR, PersistentDataType.STRING);
    }

    public void applyArmor(Player player, Team team) {
        String teamId = team.getId();
        Color color = colorTagToRgb(team.getColor());

        player.getInventory().setHelmet(coloredArmor(teamId, Material.LEATHER_HELMET, color));
        player.getInventory().setChestplate(coloredArmor(teamId, Material.LEATHER_CHESTPLATE, color));
        player.getInventory().setLeggings(coloredArmor(teamId, Material.LEATHER_LEGGINGS, color));
        player.getInventory().setBoots(coloredArmor(teamId, Material.LEATHER_BOOTS, color));
    }

    public void removeArmor(Player player) {
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
    }

    private ItemStack coloredArmor(
        String teamId,
        Material material,
        Color color
    ) {
        ItemStack item = new ItemStack(material);
        item.editMeta(LeatherArmorMeta.class, meta -> {
            meta.setColor(color);
            meta.getPersistentDataContainer().set(
                PDCKeys.TEAM_ARMOR, PersistentDataType.STRING, teamId);
        });

        return item;
    }

    public Color colorTagToRgb(String colorTag) {
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
