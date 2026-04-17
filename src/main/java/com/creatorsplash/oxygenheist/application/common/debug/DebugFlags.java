package com.creatorsplash.oxygenheist.application.common.debug;

import java.util.Set;

public class DebugFlags {

    private final Set<String> enabled;

    public DebugFlags(Set<String> enabled) {
        this.enabled = enabled;
    }

    public boolean enabled(String key) {
        return enabled.contains("all") || enabled.contains(key);
    }

}
