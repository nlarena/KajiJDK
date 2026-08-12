package java.time;

// KajiLibrary's java.time.Clock — a supplier of the current instant. A KajiLibrary subset: the
// abstract instant() + millis(), and the systemUTC() factory. The zone-based parts
// (getZone/withZone, and the fixed/offset/tick/system factories) belong with ZoneId and the
// timezone database (a later tier).
public abstract class Clock implements InstantSource {

    protected Clock() {
    }

    public abstract Instant instant();

    public long millis() {
        return System.currentTimeMillis();
    }

    public static Clock systemUTC() {
        return new SystemClock();
    }
}

// The system clock, reading the VM's wall time. Package-private (the JDK nests it inside Clock;
// the API-shape gate skips it since there is no java.time.SystemClock in the JDK).
final class SystemClock extends Clock {

    public Instant instant() {
        return Instant.now();
    }
}
