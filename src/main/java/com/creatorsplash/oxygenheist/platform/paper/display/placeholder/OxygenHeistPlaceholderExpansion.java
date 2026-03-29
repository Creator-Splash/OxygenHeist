package com.creatorsplash.oxygenheist.platform.paper.display.placeholder;

import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@RequiredArgsConstructor
public final class OxygenHeistPlaceholderExpansion extends PlaceholderExpansion {

    private final Supplier<MatchSnapshot> snapshotSupplier;

    @Override
    public @NotNull String getIdentifier() {
        return "oxygenheist";
    }

    @Override
    public @NotNull String getAuthor() {
        return "OxygenHeist";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }



    @Override
    public String onPlaceholderRequest(
        Player player,
        @NotNull String params
    ) {
        MatchSnapshot snapshot = snapshotSupplier.get();

        if (snapshot == null) return "";

        if (player != null) {
            var playerSnapshot = snapshot.getPlayer(player.getUniqueId());
            if (playerSnapshot == null) return "";

            switch (params) {
                case "player_oxygen":
                    return Double.toString(playerSnapshot.oxygen());
                case "player_is_downed":
                    return Boolean.toString(playerSnapshot.downed());
                case "player_is_dead":
                    return Boolean.toString(!playerSnapshot.alive());
                default: break;
            }
        }

        switch (params) {
            case "game_state":
                return snapshot.state().name();
            case "game_state_display":
                return switch (snapshot.state()) {
                    case PLAYING -> "In Progress";
                    case WAITING -> "Waiting";
                    case SETUP -> "Starting";
                    case ENDING -> "Ended";
                };

            case "game_time_seconds":
                return Integer.toString(snapshot.remainingSeconds());
            case "game_time_ticks":
                return Integer.toString(snapshot.remainingSeconds() * 20);
            case "game_time":
                return formatTime(snapshot.remainingSeconds());
            case "game_instant_death":
                return Boolean.toString(snapshot.instantDeath());

            case "zone_count":
                return Integer.toString(snapshot.zones().size());
            default: break;
        }

        if (params.startsWith("zone_") && params.endsWith("_progress")) {
            String zoneId = params.substring(5, params.length() - 9);
            var zone = snapshot.getZone(zoneId);
            return zone != null ? Double.toString(zone.captureProgress()) : "0";
        }

        return "";
    }

    private static String formatTime(int totalSeconds) {
        int mins = Math.max(0, totalSeconds) / 60;
        int secs = Math.max(0, totalSeconds) % 60;
        return String.format("%02d:%02d", mins, secs);
    }

}
