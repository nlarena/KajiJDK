package java.time;

import java.time.zone.ZoneRulesException;

// KajiLibrary's java.time.ZoneId — the identifier for a time-zone, the base type of ZoneOffset. A
// KajiLibrary subset: only fixed-offset zones are supported. of(id) accepts the offset forms ("Z",
// "+HH:MM", "-HH:MM", …) and returns a ZoneOffset; region-based ids (e.g. "Europe/Paris") need IANA
// tzdb transition rules — a data wall — so they raise ZoneRulesException. The abstract getRules()
// accessor of the JDK is therefore omitted. The package-private constructor limits subclassing to
// java.time (as in the JDK), where ZoneOffset extends it.
public abstract class ZoneId {

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
