package com.creatorsplash.oxygenheist.platform.paper.util;

import lombok.experimental.UtilityClass;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class CommandUtils {

    public @Nullable Player resolveTarget(@NotNull CommandSender sender, @Nullable Player explicit) {
        return explicit != null ? explicit
            : sender instanceof Player p ? p
            : null;
    }

}
