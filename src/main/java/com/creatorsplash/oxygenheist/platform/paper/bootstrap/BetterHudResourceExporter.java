package com.creatorsplash.oxygenheist.platform.paper.bootstrap;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class BetterHudResourceExporter {

    /** OH-bundled resources to copy on every enable. Path is relative to jar root and to the BetterHud folder. */
    private static final List<String> OVERWRITE_RESOURCES = List.of(
            "betterhud/huds/default-hud.yml",
            "betterhud/huds/oxygenheist_hud_game.yml",
            "betterhud/layouts/oxygenheist-main-hud-layout.yml",
            "betterhud/layouts/eventcore-scoreboard-layout.yml",
            "betterhud/texts/eventcore-scoreboard.yml",
            "betterhud/images/shared/eventcore-scoreboard-images.yml"
    );

    /** Asset files copied only if missing: avoids overwriting user PNG edits. */
    private static final List<String> KEEP_IF_EXISTS_RESOURCES = List.of(
            "betterhud/assets/eventcore/scoreboard/background.png",
            "betterhud/assets/eventcore/scoreboard/splashvoting.png",
            "betterhud/assets/eventcore/scoreboard/tophud.png",
            "betterhud/assets/eventcore/scoreboard/icons/dolphin.png",
            "betterhud/assets/eventcore/scoreboard/icons/jellyfish.png",
            "betterhud/assets/eventcore/scoreboard/icons/octupus.png",
            "betterhud/assets/eventcore/scoreboard/icons/orcas.png",
            "betterhud/assets/eventcore/scoreboard/icons/seahorse.png",
            "betterhud/assets/eventcore/scoreboard/icons/stingray.png",
            "betterhud/assets/eventcore/scoreboard/icons/swordfish.png",
            "betterhud/assets/eventcore/scoreboard/icons/turtle.png",
            "betterhud/fonts/5z5.ttf"
    );

    /** Cross-game leftover files in BetterHud/layouts, BetterHud/huds and BetterHud/images
     *  that we proactively disable. They reference disabled layouts or SS/SR placeholders. */
    private static final List<String> LEFTOVER_FILES_TO_DISABLE = List.of(
            "layouts/supersoakers-layout.yml",
            "layouts/powerups-layout.yml",
            "layouts/splashvoting-layout.yml",
            "layouts/bounties-panel.yml",
            "layouts/center_texts.yml",
            "huds/scoreboard-hud.yml",
            "huds/centerhud.yml",
            "images/supersoakers-image.yml",
            "images/powerups-images.yml",
            "images/splashvoting-image.yml",
            "images/bounties.yml"
    );

    private final Plugin plugin;

    public BetterHudResourceExporter(Plugin plugin) {
        this.plugin = plugin;
    }

    public void run() {
        File betterHudFolder = new File(plugin.getServer().getPluginsFolder(), "BetterHud");
        if (!betterHudFolder.exists()) {
            plugin.getLogger().warning("BetterHud folder not found; skipping HUD resource export.");
            return;
        }

        for (String path : OVERWRITE_RESOURCES) {
            saveResource(betterHudFolder, path, true);
        }
        for (String path : KEEP_IF_EXISTS_RESOURCES) {
            saveResource(betterHudFolder, path, false);
        }
        disableLeftovers(betterHudFolder);
    }

    private void saveResource(File betterHudFolder, String resourcePath, boolean overwrite) {
        File target = resolveTarget(betterHudFolder, resourcePath);
        if (!overwrite && target.exists()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Could not create folder " + parent.getAbsolutePath());
            return;
        }
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("Bundled BetterHud resource missing from jar: " + resourcePath);
                return;
            }
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to export BetterHud resource " + resourcePath + ": " + e.getMessage());
        }
    }

    private File resolveTarget(File betterHudFolder, String resourcePath) {
        return new File(betterHudFolder, resourcePath.substring("betterhud/".length()));
    }

    private void disableLeftovers(File betterHudFolder) {
        for (String relative : LEFTOVER_FILES_TO_DISABLE) {
            File file = new File(betterHudFolder, relative);
            if (!file.exists() || !file.isFile()) continue;
            File renamed = new File(file.getParentFile(), "-" + file.getName());
            if (renamed.exists()) {
                if (file.delete()) {
                    plugin.getLogger().info("Removed redundant leftover " + relative
                            + " (already disabled at " + renamed.getName() + ").");
                }
                continue;
            }
            if (file.renameTo(renamed)) {
                plugin.getLogger().info("Disabled cross-game leftover BetterHud file: "
                        + relative + " -> " + renamed.getName());
            } else {
                plugin.getLogger().warning("Could not rename leftover " + relative
                        + " to disable it; please remove manually.");
            }
        }
    }
}
