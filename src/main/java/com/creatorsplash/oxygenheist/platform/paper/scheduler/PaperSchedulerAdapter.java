package com.creatorsplash.oxygenheist.platform.paper.scheduler;

import com.creatorsplash.oxygenheist.application.common.task.Scheduler;
import com.creatorsplash.oxygenheist.application.common.task.TickingTask;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class PaperSchedulerAdapter implements Scheduler {

    private final JavaPlugin plugin;

    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();

    private BukkitScheduler scheduler() {
        return plugin.getServer().getScheduler();
    }

    @Override
    public void onDisable() {
        asyncExecutor.shutdown();
    }

    @Override
    public Executor syncExecutor() {
        return scheduler().getMainThreadExecutor(plugin);
    }

    @Override
    public Executor asyncExecutor() {
        return asyncExecutor;
    }

    @Override
    public Task run(Runnable task) {
        BukkitTask bukkitTask = scheduler().runTask(plugin, task);
        return bukkitTask::cancel;
    }

    @Override
    public Task runAsync(Runnable task) {
        BukkitTask bukkitTask = scheduler().runTaskAsynchronously(plugin, task);
        return bukkitTask::cancel;
    }

    @Override
    public Task runLater(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = scheduler().runTaskLater(plugin, task, delayTicks);
        return bukkitTask::cancel;
    }

    @Override
    public Task runAsyncLater(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = scheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        return bukkitTask::cancel;
    }

    @Override
    public Task runRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = scheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    @Override
    public Task runRepeating(Consumer<TickingTask> task, long delayTicks, long periodTicks) {
        int[] ticks = {0};
        BukkitTask[] handle = new BukkitTask[1];

        TickingTask tickingTask = new TickingTask() {
            @Override public int elapsedTicks() { return ticks[0]; }
            @Override public void cancel() { if (handle[0] != null) handle[0].cancel(); }
        };

        handle[0] = scheduler().runTaskTimer(plugin, () -> {
            task.accept(tickingTask);
            ticks[0]++;
        }, delayTicks, periodTicks);

        return tickingTask;
    }

    @Override
    public Task runRepeating(Consumer<TickingTask> task, long delayTicks, long periodTicks, int maxTicks) {
        int[] ticks = {0};
        BukkitTask[] handle = new BukkitTask[1];

        TickingTask tickingTask = new TickingTask() {
            @Override public int elapsedTicks() { return ticks[0]; }
            @Override public void cancel() { if (handle[0] != null) handle[0].cancel(); }
        };

        handle[0] = scheduler().runTaskTimer(plugin, () -> {
            task.accept(tickingTask);
            ticks[0]++;
            if (ticks[0] >= maxTicks) handle[0].cancel();
        }, delayTicks, periodTicks);

        return tickingTask;
    }

    @Override
    public Task runRepeatingAsync(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = scheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }

}
