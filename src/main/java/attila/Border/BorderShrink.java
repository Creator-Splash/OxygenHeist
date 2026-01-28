package attila.Border;

import org.bukkit.WorldBorder;
import org.bukkit.scheduler.BukkitRunnable;

import attila.OxygenMain;

public class BorderShrink {
    
    private final OxygenMain plugin;
    private final BorderManager borderManager;
    private boolean isShrinking;
    private BukkitRunnable shrinkTask;
    
    public BorderShrink(OxygenMain plugin, BorderManager borderManager) {
        this.plugin = plugin;
        this.borderManager = borderManager;
        this.isShrinking = false;
    }
    
    public void startShrinking(double finalSize, long durationSeconds) {
        if (!borderManager.isArenaSet()) {
            return;
        }

        double minimumSize = plugin.getBorderConfig().getMinimumSize();
        if (finalSize < minimumSize) {
            finalSize = minimumSize;
        }
        
        if (isShrinking) {
            stopShrinking();
        }
        
        WorldBorder border = borderManager.getArenaWorld().getWorldBorder();
        border.setSize(finalSize, durationSeconds);
        isShrinking = true;
        
        final double targetSize = finalSize;
        shrinkTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (border.getSize() <= targetSize + 1) {
                    isShrinking = false;
                    cancel();
                }
            }
        };
        shrinkTask.runTaskTimer(plugin, 0L, 20L);
    }
    
    public void startShrinkingGradual(double targetSize, long totalSeconds, int phases) {
        if (!borderManager.isArenaSet()) {
            return;
        }

        double minimumSize = plugin.getBorderConfig().getMinimumSize();
        if (targetSize < minimumSize) {
            targetSize = minimumSize;
        }
        
        if (isShrinking) {
            stopShrinking();
        }
        
        isShrinking = true;
        double currentSize = borderManager.getArenaSize();
        double sizeReduction = (currentSize - targetSize) / phases;
        long phaseTime = totalSeconds / phases;
        
        final double finalTargetSize = targetSize;
        shrinkTask = new BukkitRunnable() {
            int currentPhase = 0;
            double nextSize = currentSize - sizeReduction;
            
            @Override
            public void run() {
                if (currentPhase >= phases) {
                    isShrinking = false;
                    cancel();
                    return;
                }

                if (nextSize < minimumSize) {
                    nextSize = minimumSize;
                }
                
                WorldBorder border = borderManager.getArenaWorld().getWorldBorder();
                border.setSize(nextSize, phaseTime);
                
                currentPhase++;
                nextSize -= sizeReduction;

                if (nextSize <= minimumSize) {
                    isShrinking = false;
                    cancel();
                }
            }
        };
        shrinkTask.runTaskTimer(plugin, 0L, phaseTime * 20L);
    }
    
    public void stopShrinking() {
        if (shrinkTask != null) {
            shrinkTask.cancel();
            shrinkTask = null;
        }
        isShrinking = false;
    }
    
    public void pauseShrinking() {
        if (!borderManager.isArenaSet() || !isShrinking) {
            return;
        }
        
        WorldBorder border = borderManager.getArenaWorld().getWorldBorder();
        double currentSize = border.getSize();
        border.setSize(currentSize, 0);
        
        if (shrinkTask != null) {
            shrinkTask.cancel();
        }
        isShrinking = false;
    }
    
    public void resumeShrinking(double finalSize, long remainingSeconds) {
        if (!borderManager.isArenaSet()) {
            return;
        }
        
        startShrinking(finalSize, remainingSeconds);
    }
    
    public boolean isShrinking() {
        return isShrinking;
    }
    
    public double getCurrentBorderSize() {
        if (!borderManager.isArenaSet()) {
            return 0;
        }
        return borderManager.getArenaWorld().getWorldBorder().getSize();
    }
}