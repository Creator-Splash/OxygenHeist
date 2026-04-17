package com.creatorsplash.oxygenheist.platform.paper.config.team;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.domain.team.TeamBase;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Loads and saves team configuration from {@code teams.yml}
 */
@RequiredArgsConstructor
public final class TeamConfigService {

    private static final String FILE_NAME = "teams.yml";

    private final LogCenter log;

    public TeamService load(JavaPlugin plugin) {
        plugin.saveResource(FILE_NAME, false);

        File file = new File(plugin.getDataFolder(), FILE_NAME);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        boolean friendlyFire = config.getBoolean("settings.friendly-fire", false);
        int maxTeamSize = config.getInt("settings.max-team-size", 10);

        /* Teams */

        List<Team> teams = new ArrayList<>();

        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String teamId : teamsSection.getKeys(false)) {
                ConfigurationSection section = teamsSection.getConfigurationSection(teamId);
                if (section == null) continue;

                String name = section.getString("name", teamId);
                String color = section.getString("color", "white");
                TeamBase base = loadBase(section.getConfigurationSection("base"));

                Team team = new Team(teamId.toLowerCase(), name, color, base);

                for (String uuidStr : section.getStringList("members")) {
                    try {
                        team.addMember(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException e) {
                        log.warn(
                            "Invalid member UUID in team '" + teamId + "': " + uuidStr);
                    }
                }

                String captainStr = section.getString("captain");
                if (captainStr != null && !captainStr.isEmpty()) {
                    try {
                        team.setCaptain(UUID.fromString(captainStr));
                    } catch (IllegalArgumentException e) {
                        log.warn(
                            "Invalid captain UUID in team '" + teamId + "': " + captainStr);
                    }
                }

                teams.add(team);
                log.info("Loaded team: %s (id=%s)".formatted(name, teamId));
            }
        }

        log.info("Loaded " + teams.size() + " team(s) from " + FILE_NAME);

        return new TeamService(teams, friendlyFire, maxTeamSize);
    }

    /**
     * Saves mutable roster data (members, captain) back to {@code teams.yml}
     */
    public void save(JavaPlugin plugin, TeamService teamService) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (Team team : teamService.getAllTeams()) {
            String path = "teams." + team.getId();

            List<String> memberStrings = new ArrayList<>();
            for (UUID memberId : team.getMembers()) {
                memberStrings.add(memberId.toString());
            }
            config.set(path + ".members", memberStrings);
            config.set(path + ".captain",
                team.getCaptainId() != null ? team.getCaptainId().toString() : null);

            TeamBase base = team.getBase();
            if (base != null) {
                String basePath = path + ".base";

                config.set(basePath + ".world", base.world());
                config.set(basePath + ".x", base.x());
                config.set(basePath + ".y", base.y());
                config.set(basePath + ".z", base.z());
                config.set(basePath + ".yaw", base.yaw());
                config.set(basePath + ".pitch", base.pitch());
                config.set(basePath + ".radius", base.radius());
            } else {
                config.set(path + ".base", null);
            }
        }

        try {
            config.save(file);
        } catch (Exception e) {
            log.warn("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /* Helpers */

    private @Nullable TeamBase loadBase(@Nullable ConfigurationSection section) {
        if (section == null) return null;

        String world = section.getString("world");
        if (world == null || world.isEmpty()) return null;

        return new TeamBase(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw", 0.0),
            (float) section.getDouble("pitch", 0.0),
            section.getInt("radius", 15)
        );
    }

}
