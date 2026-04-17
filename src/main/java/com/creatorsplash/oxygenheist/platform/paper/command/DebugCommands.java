package com.creatorsplash.oxygenheist.platform.paper.command;

import com.creatorsplash.oxygenheist.application.bridge.GamePlayerService;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.platform.paper.util.CommandUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponRegistry;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.WeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponDropService;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Command("oxygenheist|oh debug")
@Permission("com.creatorsplash.oxygenheist.debug")
@RequiredArgsConstructor
public final class DebugCommands implements CommandHandler {

    private final MatchService matchService;
    private final GamePlayerService playerService;
    private final WeaponRegistry weaponRegistry;
    private final WeaponDropService weaponDropService;

    @Command("down <player>")
    @CommandDescription("Force down a player")
    public void down(
        CommandSender sender,
        @Argument("player") Player target
    ) {
        matchService.getSession().ifPresentOrElse(ignored ->
            matchService.downPlayer(target.getUniqueId()),
                () -> sender.sendRichMessage("<red>No active game session"));
    }

    @Command("revive <player>")
    @CommandDescription("Force revive a player")
    public void revive(
        CommandSender sender,
        @Argument("player") Player target
    ) {
        UUID reviverId = sender instanceof Player reviver
            ? reviver.getUniqueId() : UUID.randomUUID();

        matchService.getSession().ifPresentOrElse(ignored ->
            matchService.completeRevive(target.getUniqueId(), reviverId),
                () -> sender.sendRichMessage("<red>No active game session"));
    }

    @Command("down-effect <player>")
    @CommandDescription("Test down a player")
    public void testDownEffect(
        CommandSender sender,
        @Argument("player") Player target
    ) {
        playerService.onPlayerDowned(target.getUniqueId());
    }

    @Command("weapon give <weapon> [player]")
    @CommandDescription("Gives a debug ready weapon")
    public void giveWeapon(
        CommandSender sender,
        @Argument(value = "weapon", suggestions = "weapon-ids") String weaponId,
        @Argument("player") @Nullable Player player
    ) {
        WeaponHandler handler = weaponRegistry.get(weaponId);
        if (handler == null) {
            sender.sendRichMessage("<red>Unknown weapon: " + weaponId);
            return;
        }

        ItemStack item = handler.createItemStack();

        Player target = CommandUtils.resolveTarget(sender, player);
        if (target == null) {
            sender.sendRichMessage("<red>Must be or select a player to give the weapon to");
            return;
        }

        target.getInventory().addItem(item);
        sender.sendRichMessage("<green>Given " + weaponId + " to " + target.getName());
    }

    @Command("weapon drop-cache")
    @CommandDescription("Run the weapon drop surface cache builder")
    public void runWeaponDropCacheBuilder(
        CommandSender sender
    ) {
        weaponDropService.onCountdownStart();
    }

    @Suggestions("weapon-ids")
    public List<String> suggestWeapons(CommandContext<CommandSender> ctx) {
        return weaponRegistry.all().stream().map(WeaponHandler::id).toList();
    }

}
