package attila.Border;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import attila.OxygenMain;
import net.kyori.adventure.text.Component;

public class BorderTimer {
    
    private final OxygenMain plugin;
    private final BorderShrink borderShrink;
    private BukkitRunnable timerTask;
    private boolean isRunning;
    private int remainingSeconds;
    private double targetSize;
    private long shrinkDuration;
    
    public BorderTimer(OxygenMain plugin, BorderShrink borderShrink) {
        this.plugin = plugin;
        this.borderShrink = borderShrink;
        this.isRunning = false;
    }
    
    public void startTimer(int delaySeconds, double targetSize, long shrinkDuration) {
        if (isRunning) {
            cancelTimer();
        }
        
        this.remainingSeconds = delaySeconds;
        this.targetSize = targetSize;
        this.shrinkDuration = shrinkDuration;
        this.isRunning = true;
        
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    borderShrink.startShrinking(targetSize, shrinkDuration);
                    Bukkit.getServer().broadcast(Component.text("§c§lTHE BORDER's SHRINKING IS STARTING!"));
                    isRunning = false;
                    cancel();
                    return;
                }
                
                if (remainingSeconds % 60 == 0 || remainingSeconds <= 10) {
                    Bukkit.getServer().broadcast(Component.text("§e§lBorder will shrink in: §f" + remainingSeconds + "s"));
                }
                
                remainingSeconds--;
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 20L);
    }
    
    public void startTimerWithPhases(int delaySeconds, double targetSize, long totalDuration, int phases) {
        if (isRunning) {
            cancelTimer();
        }
        
        this.remainingSeconds = delaySeconds;
        this.targetSize = targetSize;
        this.shrinkDuration = totalDuration;
        this.isRunning = true;
        
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    borderShrink.startShrinkingGradual(targetSize, totalDuration, phases);
                    Bukkit.getServer().broadcast(Component.text("§c§lTHE BORDER's SHRINKING IS STARTING!, RUN!"));
                    isRunning = false;
                    cancel();
                    return;
                }
                
                if (remainingSeconds % 60 == 0 || remainingSeconds <= 10) {
                    Bukkit.getServer().broadcast(Component.text("§e§lBorder will shrink in: §f" + remainingSeconds + "s"));
                }
                
                remainingSeconds--;
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 20L);
    }
    
    public void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        isRunning = false;
        remainingSeconds = 0;
    }
    
    public void pauseTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        isRunning = false;
    }
    
    public void resumeTimer() {
        if (remainingSeconds > 0 && !isRunning) {
            startTimer(remainingSeconds, targetSize, shrinkDuration);
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}