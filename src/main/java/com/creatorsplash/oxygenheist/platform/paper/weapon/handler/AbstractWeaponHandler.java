package com.creatorsplash.oxygenheist.platform.paper.weapon.handler;

import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Base class for all weapon handlers
 *
 * <p>Provides identity resolution and item creation, both of which are
 * identical across every weapon and should never be overridden</p>
 */
public abstract class AbstractWeaponHandler implements WeaponHandler {

    protected final WeaponTypeConfig config;
    protected final WeaponItemProvider provider;

    private final List<Scheduler.Task> worldTasks = new ArrayList<>();
    private final Map<UUID, List<Scheduler.Task>> activeTasks = new HashMap<>();

    protected AbstractWeaponHandler(
        WeaponTypeConfig config,
        WeaponItemProvider provider
    ) {
        this.config = config;
        this.provider = provider;
    }

    @Override
    public final boolean handles(ItemStack item) {
        return WeaponUtils.doWeaponHandle(item, id());
    }

    @Override
    public final ItemStack createItemStack() {
        return provider.createWeaponItem(id(), WeaponUtils.formatDisplayName(id()));
    }

    /* == Task tracking == */

    /**
     * Tracks a scheduled task against a player
     * <p>All tracked tasks are cancelled automatically on match end or player leave</p>
     */
    protected void trackPlayerTask(UUID playerId, Scheduler.Task task) {
        activeTasks.computeIfAbsent(playerId, k -> new ArrayList<>()).add(task);
    }

    /** Cancelled on match end only - persists if the player leaves */
    protected void trackWorldTask(Scheduler.Task task) {
        worldTasks.add(task);
    }

    protected void cancelPlayerTasks(UUID playerId) {
        List<Scheduler.Task> tasks = activeTasks.remove(playerId);
        if (tasks != null) tasks.forEach(Scheduler.Task::cancel);
    }

    protected void cancelAllTasks() {
        activeTasks.values().forEach(tasks -> tasks.forEach(Scheduler.Task::cancel));
        activeTasks.clear();

        worldTasks.forEach(Scheduler.Task::cancel);
        worldTasks.clear();
    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        cleanUp();
    }

    @Override
    public void cleanUp() {
        cancelAllTasks();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        cancelPlayerTasks(playerId);
    }

}
