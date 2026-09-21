package java.time.zone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

// KajiLibrary's java.time.zone.ZoneRulesProvider -- the lookup of "for this zone id, what are the
// rules".
//
// **It is an SPI, not an accessor**, and that is the difference this version fixes. It used to be an
// abstract class with two statics that read the embedded table directly: the shape of an SPI without
// the mechanism. An overridden `provideRules` was called by nobody, because there was no registry.
//
// Now there is: there is a list of providers, `registerProvider` extends it, and the two statics walk
// the list. The embedded table's provider (`TzData`) is simply the first one registered, and stops
// being a special case.
//
// Why it matters even though there is only one today: it is the only way for the day the IANA
// database can be read at run time to plug it in **without touching this class**. And in the
// meantime, code that registers a provider of its own --a test zone, a historical zone-- works.
//
// The providers are searched **from the last registered to the first**, so that one added later can
// cover the embedded table and not the other way round.
public abstract class ZoneRulesProvider {

    // The list of providers. It starts with the embedded table's.
    private static final List<ZoneRulesProvider> PROVIDERS = buildList();

    private static List<ZoneRulesProvider> buildList() {
        List<ZoneRulesProvider> list = new ArrayList<ZoneRulesProvider>();
        list.add(new TzDataProvider());
        return list;
    }

    protected ZoneRulesProvider() {
    }

    // ---- the two static lookups --------------------------------------------------------------------

    /** Every id some provider knows. */
    public static Set<String> getAvailableZoneIds() {
        Set<String> ids = new HashSet<String>();
        synchronized (PROVIDERS) {
            int i = 0;
            while (i < PROVIDERS.size()) {
                ZoneRulesProvider p = PROVIDERS.get(i);
                ids.addAll(p.provideZoneIds());
                i = i + 1;
            }
        }
        return ids;
    }

    /**
     * That zone's rules.
     *
     * <p>`forCaching` is accepted and **ignored**: this library's rules are immutable values out of a
     * table that does not change, so there is nothing to cache and nothing to invalidate. The
     * parameter exists in the JDK so that a dynamic provider can return an object that must not be
     * stored.
     *
     * @throws ZoneRulesException if no provider knows that zone
     */
    public static ZoneRules getRules(String zoneId, boolean forCaching) {
        if (zoneId == null) {
            throw new NullPointerException("zoneId");
        }
        synchronized (PROVIDERS) {
            int i = PROVIDERS.size() - 1;
            while (i >= 0) {
                ZoneRulesProvider p = PROVIDERS.get(i);
                ZoneRules r = p.provideRules(zoneId, forCaching);
                if (r != null) {
                    return r;
                }
                i = i - 1;
            }
        }
        throw new ZoneRulesException("Unknown time-zone ID: " + zoneId);
    }

    /**
     * That zone's rule versions, from the oldest to the newest.
     *
     * <p>It exists because a zone's rules **change over time** --a country moves its daylight
     * saving-- and a date stored five years ago may have been computed with rules that no longer
     * hold. The map has them all, keyed by the tzdb version.
     *
     * @throws ZoneRulesException if no provider knows that zone
     */
    public static NavigableMap<String, ZoneRules> getVersions(String zoneId) {
        if (zoneId == null) {
            throw new NullPointerException("zoneId");
        }
        synchronized (PROVIDERS) {
            int i = PROVIDERS.size() - 1;
            while (i >= 0) {
                ZoneRulesProvider p = PROVIDERS.get(i);
                NavigableMap<String, ZoneRules> v = p.provideVersions(zoneId);
                if (v != null && !v.isEmpty()) {
                    return v;
                }
                i = i - 1;
            }
        }
        throw new ZoneRulesException("Unknown time-zone ID: " + zoneId);
    }

    /**
     * It registers a provider.
     *
     * <p>It ends up **at the end** of the list and is therefore consulted **first**: a provider added
     * later covers the earlier ones for the ids it knows, which is what one wants from an addition.
     *
     * @throws NullPointerException if `provider` is `null`
     */
    public static void registerProvider(ZoneRulesProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        synchronized (PROVIDERS) {
            PROVIDERS.add(provider);
        }
    }

    /**
     * It asks every provider to reload its data.
     *
     * @return whether any of them changed
     */
    public static boolean refresh() {
        boolean change = false;
        synchronized (PROVIDERS) {
            int i = 0;
            while (i < PROVIDERS.size()) {
                ZoneRulesProvider p = PROVIDERS.get(i);
                if (p.provideRefresh()) {
                    change = true;
                }
                i = i + 1;
            }
        }
        return change;
    }

    // ---- what a provider implements ----------------------------------------------------------------

    /** The ids **this** provider knows. */
    protected abstract Set<String> provideZoneIds();

    /** That zone's rules according to **this** provider, or `null` if it does not know it. */
    protected abstract ZoneRules provideRules(String zoneId, boolean forCaching);

    /** That zone's versions according to **this** provider, or empty if it does not know it. */
    protected abstract NavigableMap<String, ZoneRules> provideVersions(String zoneId);

    /**
     * It reloads this provider's data.
     *
     * <p>It is not abstract because the right answer for a provider of **fixed** data is "nothing
     * changed", and that is the common case. Only one that reads from outside overrides it.
     *
     * @return whether anything changed
     */
    protected boolean provideRefresh() {
        return false;
    }
}

// The embedded table's provider. It is the first on the list and has nothing special about it: the
// only difference from one somebody writes is that this one comes registered.
//
// `provideVersions` returns a single entry because the table **is** a single version: the historical
// rules of each zone are not kept, only the ones in force. Saying so --one version, with a name-- is
// more honest than returning empty, which would mean "I do not know this zone".
final class TzDataProvider extends ZoneRulesProvider {

    protected Set<String> provideZoneIds() {
        Set<String> ids = new HashSet<String>();
        String[] all = TzData.zoneIds();
        int i = 0;
        while (i < all.length) {
            ids.add(all[i]);
            i = i + 1;
        }
        return ids;
    }

    protected ZoneRules provideRules(String zoneId, boolean forCaching) {
        int index = TzData.zoneIndex(zoneId);
        if (index < 0) {
            return null;
        }
        return ZoneRules.ofZone(index);
    }

    protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
        TreeMap<String, ZoneRules> versions = new TreeMap<String, ZoneRules>();
        ZoneRules r = this.provideRules(zoneId, false);
        if (r != null) {
            versions.put("KajiTzData", r);
        }
        return versions;
    }
}
