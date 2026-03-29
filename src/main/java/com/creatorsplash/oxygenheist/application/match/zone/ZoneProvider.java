package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;

import java.util.List;

/**
 * Supplies the set of capture zones to load into a new match session
 */
@FunctionalInterface
public interface ZoneProvider {

    /**
     * Returns all zones that should be active for the upcoming match
     *
     * <p>Each call should return a fresh set of {@link CaptureZoneState} instances -
     * these are mutable and owned by the session for its lifetime</p>
     */
    List<CaptureZoneState> loadZones();

}
