package com.creatorsplash.oxygenheist.domain.match.config;

public record OxygenConfig(
    double max,
    double drainPerTick,
    int depletionDownTicks
) {}
