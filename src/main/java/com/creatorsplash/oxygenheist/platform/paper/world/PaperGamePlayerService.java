package com.creatorsplash.oxygenheist.platform.paper.world;

import com.creatorsplash.oxygenheist.application.bridge.GamePlayerService;
import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.domain.team.TeamBase;
import com.creatorsplash.oxygenheist.platform.paper.util.TeamUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Paper implementation of {@link GamePlayerService}
 *
 * <p>Responsible for teleporting players to team bases at match start and
 * resetting their state to a clean slate at match end.</p>
 */
@RequiredArgsConstructor
public final class PaperGamePlayerService implements GamePlayerService {

    private final Server server;
    private final Scheduler scheduler;
    private final TeamService teamService;
    private final LogCenter log;

    @Override
    public void prepareForMatch(MatchSession session) {
        for (PlayerMatchState playerState : session.getPlayers()) {
            Player player = server.getPlayer(playerState.getPlayerId());
            if (player == null) continue;

            String teamId = session.getPlayerTeam(playerState.getPlayerId());
            if (teamId == null) continue;

            Team team = teamService.getTeam(teamId);
            if (team == null || !team.hasBase()) {
                log.warn("Team '" + teamId + "' has no base set - "
                        + player.getName() + " was not teleported");
                continue;
            }

            TeamBase base = team.getBase();
            World world = server.getWorld(base.world());
            if (world == null) {
                log.warn("Base world '" + base.world() + "' for team '" + teamId
                    + "' is not loaded - " + player.getName() + " was not teleported");
                continue;
            }

            Location loc = new Location(
                world, base.x(), base.y(), base.z(), base.yaw(), base.pitch()
            );

            player.teleport(loc);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(maxHealth(player));
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.getInventory().clear();
            player.clearActivePotionEffects();
            player.setRemainingAir(player.getMaximumAir());

            scheduler.runLater(() -> TeamUtils.applyArmor(player, team), 1L);
        }

        log.info("Players prepared for match start");
    }

    @Override
    public void cleanupAfterMatch(MatchSession session) {
        for (PlayerMatchState playerState : session.getPlayers()) {
            Player player = server.getPlayer(playerState.getPlayerId());
            if (player == null) continue;

            player.setGameMode(GameMode.ADVENTURE);
            player.clearActivePotionEffects();
            player.setHealth(maxHealth(player));
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.getInventory().clear();
            player.setSneaking(false);
            player.setRemainingAir(player.getMaximumAir());
            TeamUtils.removeArmor(player);
        }

        log.info("Players cleaned up after match end");
    }

    /* Gameplay */

    @Override
    public void onPlayerDowned(UUID playerId) {
        Player player = server.getPlayer(playerId);
        if (player == null) return;

        player.setHealth(maxHealth(player));
        player.setSneaking(true);

        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 3, false, false));

        player.addPotionEffect(new PotionEffect(
            PotionEffectType.BLINDNESS, 20, 0, false, false));
    }

    @Override
    public void onPlayerRevived(UUID playerId) {
        Player player = server.getPlayer(playerId);
        if (player == null) return;

        player.setSneaking(false);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setHealth(maxHealth(player) * 0.5);
    }

    @Override
    public void onPlayerEliminated(UUID playerId) {
        Player player = server.getPlayer(playerId);
        if (player == null) return;

        player.setSneaking(false);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setGameMode(GameMode.SPECTATOR);
    }

    private double maxHealth(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}
