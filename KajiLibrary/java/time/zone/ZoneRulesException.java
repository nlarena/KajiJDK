package java.time.zone;

import java.time.DateTimeException;

// KajiLibrary's java.time.zone.ZoneRulesException — thrown when time-zone rules cannot be obtained
// (e.g. for a region-based zone id whose rules are not available). In KajiLibrary region-based zones
// are not supported (the IANA tzdb transition data is a data wall), so ZoneId.of throws this for any
// non-offset zone.
public class ZoneRulesException extends DateTimeException {

    public ZoneRulesException(String message) {
        super(message);
    }

    public ZoneRulesException(String message, Throwable cause) {
        super(message, cause);
    }
}
