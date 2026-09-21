package java.time;

import java.io.Serializable;
import java.time.zone.ZoneRulesException;

// KajiLibrary's java.time.ZoneId — the identifier for a time-zone, the base type of ZoneOffset. A
// KajiLibrary subset: only fixed-offset zones are supported. of(id) accepts the offset forms ("Z",
// "+HH:MM", "-HH:MM", …) and returns a ZoneOffset; region-based ids (e.g. "Europe/Paris") need IANA
// tzdb transition rules — a data wall — so they raise ZoneRulesException. The abstract getRules()
// accessor of the JDK is therefore omitted. The package-private constructor limits subclassing to
// java.time (as in the JDK), where ZoneOffset extends it.
public abstract class ZoneId implements Serializable {

    ZoneId() {
    }

    public static ZoneId of(String zoneId) {
        if (zoneId.length() == 0 || zoneId.equals("Z")) {
            return ZoneOffset.of(zoneId);
        }
        char c = zoneId.charAt(0);
        if (c == '+' || c == '-') {
            return ZoneOffset.of(zoneId);
        }
        throw new ZoneRulesException(
            "Region-based zones require time-zone rules (tzdb), unsupported in KajiLibrary: " + zoneId);
    }

    // A KajiLibrary subset: with no OS time-zone lookup, the default zone is UTC.
    public static ZoneId systemDefault() {
        return ZoneOffset.UTC;
    }

    public abstract String getId();

    /**
     * This zone's rules: the offsets and when they change.
     *
     * <p>It is where daylight saving lives. The only zones **this class can build** are the
     * fixed-offset ones (`ZoneOffset`), so the rules a `ZoneId` obtained here carries are constant.
     * `java.time.zone` knows more: see {@link #getAvailableZoneIds()}.
     */
    public abstract java.time.zone.ZoneRules getRules();

    /**
     * The historical short identifiers (`EST`, `PST`, ...) mapped to the long ones.
     *
     * <p>They are accepted **only** when asked for explicitly, through `of(id, aliasMap)`: they are
     * ambiguous --`CST` is both Chicago and Shanghai-- and that is why the JDK stopped accepting
     * them by default. This map exists for old code that still uses them.
     */
    public static final java.util.Map<String, String> SHORT_IDS = ZoneId.shortIds();

    private static java.util.Map<String, String> shortIds() {
        java.util.Map<String, String> m = new java.util.HashMap<String, String>();
        m.put("ACT", "Australia/Darwin");
        m.put("AET", "Australia/Sydney");
        m.put("AGT", "America/Argentina/Buenos_Aires");
        m.put("ART", "Africa/Cairo");
        m.put("AST", "America/Anchorage");
        m.put("BET", "America/Sao_Paulo");
        m.put("BST", "Asia/Dhaka");
        m.put("CAT", "Africa/Harare");
        m.put("CNT", "America/St_Johns");
        m.put("CST", "America/Chicago");
        m.put("CTT", "Asia/Shanghai");
        m.put("EAT", "Africa/Addis_Ababa");
        m.put("ECT", "Europe/Paris");
        m.put("IET", "America/Indiana/Indianapolis");
        m.put("IST", "Asia/Kolkata");
        m.put("JST", "Asia/Tokyo");
        m.put("MIT", "Pacific/Apia");
        m.put("NET", "Asia/Yerevan");
        m.put("NST", "Pacific/Auckland");
        m.put("PLT", "Asia/Karachi");
        m.put("PNT", "America/Phoenix");
        m.put("PRT", "America/Puerto_Rico");
        m.put("PST", "America/Los_Angeles");
        m.put("SST", "Pacific/Guadalcanal");
        m.put("VST", "Asia/Ho_Chi_Minh");
        m.put("EST", "-05:00");
        m.put("MST", "-07:00");
        m.put("HST", "-10:00");
        return java.util.Collections.unmodifiableMap(m);
    }

    /**
     * Like `of(String)`, but translating through `aliasMap` first.
     *
     * @throws java.time.DateTimeException if the identifier is not recognised
     */
    public static ZoneId of(String zoneId, java.util.Map<String, String> aliasMap) {
        if (zoneId == null || aliasMap == null) {
            throw new NullPointerException();
        }
        String real = aliasMap.get(zoneId);
        return ZoneId.of(real != null ? real : zoneId);
    }

    /**
     * A zone with that name and that fixed offset.
     *
     * <p>The prefix has to be empty, `GMT`, `UTC` or `UT`: they are the only ones the specification
     * allows, because the resulting identifier has to be re-parseable.
     */
    public static ZoneId ofOffset(String prefix, ZoneOffset offset) {
        if (prefix == null || offset == null) {
            throw new NullPointerException();
        }
        if (prefix.length() == 0) {
            return offset;
        }
        if (!prefix.equals("GMT") && !prefix.equals("UTC") && !prefix.equals("UT")) {
            throw new IllegalArgumentException("Invalid prefix, must be GMT, UTC or UT: " + prefix);
        }
        return ZoneId.of(prefix + offset.getId());
    }

    /** The zone `temporal` holds, if it holds one. */
    public static ZoneId from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        ZoneId z = temporal.query(java.time.temporal.TemporalQueries.zone());
        if (z == null) {
            throw new java.time.DateTimeException(
                    "Unable to obtain ZoneId from TemporalAccessor: " + temporal);
        }
        return z;
    }

    /**
     * The available zone identifiers.
     *
     * <p>**Empty**, and the reason is not the one this note used to give. It said the library
     * carries no time-zone database; it does -- `java.time.zone.TzData` holds eight zones with real
     * transition data, and `ZoneRulesProvider.getAvailableZoneIds()` enumerates them.
     *
     * <p>What is still true is the second half: `ZoneId.of` builds fixed-offset zones **only** and
     * throws `ZoneRulesException` on a region id, so listing `"Europe/Madrid"` here would hand back
     * a set whose own elements this very class cannot parse. Until `of` can reach the rules,
     * empty is the honest answer, and `ZoneRulesProvider` is where the eight are enumerable.
     */
    public static java.util.Set<String> getAvailableZoneIds() {
        return java.util.Collections.unmodifiableSet(new java.util.HashSet<String>());
    }

    /**
     * This zone reduced to its normal form: a `ZoneOffset` if the offset is fixed.
     *
     * <p>It serves for comparing: `ZoneId.of("UTC")` and `ZoneOffset.UTC` name the same thing and
     * are not `equals`, but their normalised forms are.
     */
    public ZoneId normalized() {
        java.time.zone.ZoneRules rules = this.getRules();
        if (rules != null && rules.isFixedOffset()) {
            return rules.getOffset(java.time.Instant.ofEpochSecond(0L));
        }
        return this;
    }

    /**
     * The zone's name in that region.
     *
     * <p>It returns the identifier for any region: this library does not carry the localised zone
     * names. It is documented instead of faked.
     */
    public String getDisplayName(java.time.format.TextStyle style, java.util.Locale locale) {
        if (style == null || locale == null) {
            throw new NullPointerException();
        }
        return this.getId();
    }

    public String toString() {
        return this.getId();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZoneId) {
            ZoneId other = (ZoneId) obj;
            return this.getId().equals(other.getId());
        }
        return false;
    }

    public int hashCode() {
        return this.getId().hashCode();
    }
}
