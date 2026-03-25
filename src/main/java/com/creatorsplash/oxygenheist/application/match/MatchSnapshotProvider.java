package com.creatorsplash.oxygenheist.application.match;

import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;

import java.util.function.Supplier;

public final class MatchSnapshotProvider implements Supplier<MatchSnapshot> {

    private volatile MatchSnapshot latest;

    public void update(MatchSnapshot snapshot) {
        this.latest = snapshot;
    }

    @Override
    public MatchSnapshot get() {
        return latest != null ? latest : MatchSnapshot.EMPTY;
    }

}
