package com.creatorsplash.oxygenheist.application.common.task;

/**
 * A scheduled task that is aware of how many times it has been executed
 */
public interface TickingTask extends Scheduler.Task {
    /** Returns the number of times this task has been executed so far */
    int elapsedTicks();
}
