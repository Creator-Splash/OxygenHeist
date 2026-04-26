package com.creatorsplash.oxygenheist.platform.paper.world;

import com.creatorsplash.oxygenheist.application.bridge.GamePlayerService;
import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.task.Scheduler;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.domain.team.TeamBase;
import com.creatorsplash.oxygenheist.platform.paper.util.TeamUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponDropService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
    private final DownedCrawlManager crawlManager;
    private final LogCenter log;

    @Nullable @Setter
    private WeaponDropService weaponDropService;

    @Override
    public void prepareForCountdown(MatchSession session) {
        for (PlayerMatchState playerState : session.getPlayers()) {
            prepareSinglePlayer(session, playerState.getPlayerId());
        }
        log.info("Players prepared for countdown");
    }

    /**
     * Per-player init for the countdown phase. Runs the same teleport-to-base
     * + setupPlayer + worldborder steps as {@link #prepareForCountdown} but
     * for a single player. Used by the event-mode adapter when a late
     * arrival lands on the server AFTER {@code startMatch} has already run
     * (the standard prepareForCountdown loop iterated an empty
     * {@code session.getPlayers()} at that point).
     */
    @Override
    public void prepareSinglePlayer(MatchSession session, UUID playerId) {
        Player player = server.getPlayer(playerId);
        if (player == null) return;

        Team team = resolveTeam(session, playerId);
        if (team == null) {
            log.error("Player " + player.getName() + " has no team!");
            return;
        }

        TeamBase base = team.getBase();
        if (base == null) {
            log.error("Team " + team.getName() + " has no base configured!");
            return;
        }
        Location loc = resolveBaseLoc(team);
        if (loc == null) {
            log.error("Team " + team.getName() + " could not resolve base location!");
            return;
        }

        if (loc.getWorld() != null) {
            // Pre-load destination chunk so the cross-chunk teleport during a
            // late arrival doesn't silently fail.
            loc.getWorld().getChunkAt(loc.getBlockX() >> 4, loc.getBlockZ() >> 4).load();
        }
        player.teleport(loc);
        setupPlayer(player, team, GameMode.ADVENTURE);

        player.setWorldBorder(null);
        scheduler.runLater(() -> {
            if (!player.isOnline()) return;
            WorldBorder border = server.createWorldBorder();
            border.setCenter(base.x(), base.z());
            border.setSize(base.radius() * 2.0);
            border.setWarningDistance(2);
            border.setWarningTime(0);
            border.setDamageAmount(0.5);
            border.setDamageBuffer(1.0);
            player.setWorldBorder(border);
        }, 1L);
    }

    @Override
    public void prepareForMatch(MatchSession session) {
        for (PlayerMatchState playerState : session.getPlayers()) {
            Player player = server.getPlayer(playerState.getPlayerId());
            if (player == null) continue;

            Team team = resolveTeam(session, playerState.getPlayerId());
            if (team == null) {
                log.error("Player " + player.getName() + " has no team!");
                continue;
            }

            Location loc = resolveBaseLoc(team);
            if (loc == null) {
                log.error("Team " + team.getName() + " could not resolve base location!");
                continue;
            }

            player.teleport(loc);
            setupPlayer(player, team, GameMode.SURVIVAL);
            player.setWorldBorder(null);
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

        crawlManager.apply(player);

//        player.addPotionEffect(new PotionEffect(
//            PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 3, false, false));

        player.addPotionEffect(new PotionEffect(
            PotionEffectType.BLINDNESS, 20, 0, false, false));
    }

    @Override
    public void onPlayerRevived(UUID playerId) {
        Player player = server.getPlayer(playerId);
        if (player == null) return;

        crawlManager.restore(player);

        player.setHealth(maxHealth(player) * 0.5);
    }

    @Override
    public void onPlayerEliminated(UUID playerId, MatchSession session) {
        Player player = server.getPlayer(playerId);
        if (player == null) return;

        crawlManager.restore(player);

        player.setGameMode(GameMode.SPECTATOR);

        dropWeaponsAtDeath(player);

        UUID spectateTargetId = session.resolveSpectateTarget(playerId);
        Player target = spectateTargetId != null
            ? server.getPlayer(spectateTargetId) : null;
        if (target != null && target.isOnline()) {
            player.setSpectatorTarget(target);
        }

        // Redirect spectators of the eliminated player
        for (Player online : server.getOnlinePlayers()) {
            if (online.getUniqueId().equals(playerId)) continue;
            if (online.getGameMode() != GameMode.SPECTATOR) continue;

            Entity currentTarget = online.getSpectatorTarget();
            if (currentTarget == null) continue;
            if (!currentTarget.getUniqueId().equals(playerId)) continue;

            UUID newSpectateId = session.resolveSpectateTarget(online.getUniqueId());
            Player newTarget = newSpectateId != null
                ? server.getPlayer(newSpectateId) : null;

            online.setSpectatorTarget(newTarget);
        }
    }

    @Override
    public void applySuffocationDamage(UUID playerId, double amount) {
        Player player = server.getPlayer(playerId);
        if (player == null) return;
        player.damage(amount);
    }

    /* Helpers */

    private void setupPlayer(Player player, Team team, GameMode gameMode) {
        player.setGameMode(gameMode);
        player.setHealth(maxHealth(player));
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.getInventory().clear();
        player.clearActivePotionEffects();
        player.setRemainingAir(player.getMaximumAir());
        scheduler.runLater(() -> TeamUtils.applyArmor(player, team), 1L);
    }

    private @Nullable Team resolveTeam(MatchSession session, UUID playerId) {
        String teamId = session.getPlayerTeam(playerId);
        if (teamId == null) return null;

        Team team = teamService.getTeam(teamId);
        if (team == null || !team.hasBase()) {
            log.warn("Team '" + teamId + "' has no base set - player skipped");
            return null;
        }
        return team;
    }

    private @Nullable Location resolveBaseLoc(Team team) {
        TeamBase base = team.getBase();
        if (base == null) return null;

        World world = server.getWorld(base.world());
        if (world == null) {
            log.warn("Base world '" + base.world() + "' for team '" + team.getId()
                + "' is not loaded - player skipped");
            return null;
        }

        return new Location(world, base.x(), base.y(), base.z(), base.yaw(), base.pitch());
    }

    private void dropWeaponsAtDeath(Player player) {
        if (this.weaponDropService == null) return;

        Location base = player.getLocation();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !WeaponUtils.isAnyWeapon(item)) continue;

            // Scatter each weapon slightly so they don't stack
            double ox = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
            double oz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
            Location dropLoc = base.clone().add(ox, 0.1, oz);

            weaponDropService.dropWeaponFromPlayer(item, dropLoc);
            player.getInventory().remove(item);
        }
    }

    private double maxHealth(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}
