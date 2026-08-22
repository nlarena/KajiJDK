package java.time.zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

// KajiLibrary's java.time.zone.ZoneRulesProvider — the lookup from a zone ID to its rules.
//
// In the JDK this is an extensible SPI: an abstract class with `registerProvider`, so an
// application can supply zones the platform does not ship. Here it is the accessor over the
// embedded table (see TzData) — the plugin half is omitted because there is nothing to plug in
// until zone data can be loaded at runtime.
//
// A KajiLibrary subset: `getRules`/`getAvailableZoneIds` are the two static entry points; the
// abstract `provideRules`/`provideZoneIds` and `registerProvider`/`refresh` are omitted.
public abstract class ZoneRulesProvider {

    protected ZoneRulesProvider() {
    }

    public static Set<String> getAvailableZoneIds() {
        Set<String> ids = new HashSet<String>();
        String[] all = TzData.zoneIds();
        int i = 0;
        while (i < all.length) {
            ids.add(all[i]);
            i = i + 1;
        }
        return ids;
    }

    // `forCaching` is accepted for signature fidelity and ignored: our rules are immutable values
    // read from a table that never changes, so there is nothing to cache or invalidate.
    public static ZoneRules getRules(String zoneId, boolean forCaching) {
        int index = TzData.zoneIndex(zoneId);
        if (index < 0) {
            throw new ZoneRulesException("Unknown time-zone ID: " + zoneId);
        }
        return ZoneRules.ofZone(index);
    }
}
