package com.creatorsplash.oxygenheist.domain.player;

import java.util.UUID;

public record AttackCredit(
    UUID attackerId,
    String weaponName,
    long timestamp
) {
    public boolean isWithin(long windowMs) {
        return System.currentTimeMillis() - timestamp <= windowMs;
    }
}
