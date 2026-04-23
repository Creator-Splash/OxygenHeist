package com.creatorsplash.oxygenheist.platform.paper.config.weapon;

import com.creatorsplash.oxygenheist.platform.paper.config.misc.SoundConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public record WeaponSoundSlot(List<SoundConfig> sounds) {

    public static final WeaponSoundSlot EMPTY = new WeaponSoundSlot(List.of());

    /**
     *  Plays all sounds in this slot from the player location
     *  <p>No-op if empty</p>
     */
    public void playFrom(Player player) {
        if (isEmpty()) return;
        sounds.forEach(s -> s.playAt(player.getLocation()));
    }

    /**
     *  Plays all sounds in this slot from the player location
     *  <p>No-op if empty</p>
     */
    public void playFrom(Location location) {
        if (isEmpty()) return;
        sounds.forEach(s -> s.playAt(location));
    }

    /**
     *  Plays all sounds in this slot to the player
     *  <p>No-op if empty</p>
     */
    public void playTo(Player player) {
        if (isEmpty()) return;
        sounds.forEach(s -> s.playTo(player));
    }

    public boolean isEmpty() {
        return sounds.isEmpty();
    }

}
