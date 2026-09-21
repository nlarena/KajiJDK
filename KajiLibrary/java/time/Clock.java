package java.time;

// KajiLibrary's java.time.Clock — a supplier of the current instant.
//
// The surface is complete: instant()/millis(), the zone (getZone/withZone) and the six factories
// (system/systemUTC/systemDefaultZone/fixed/offset/tick, plus tick's three variants).
public abstract class Clock implements InstantSource {

    protected Clock() {
    }

    public abstract Instant instant();

    /**
     * This clock's zone.
     *
     * <p>It is the only thing a `Clock` adds over an `InstantSource`: that one knows **when**, this
     * one knows **where** as well. `LocalDate.now(clock)` needs both -- the same instant is a
     * different day in Tokyo and in Buenos Aires.
     */
    public abstract ZoneId getZone();

    /** This clock with another zone. The instant does not change; how it is read does. */
    public abstract Clock withZone(ZoneId zone);

    /** The system clock in that zone. */
    public static Clock system(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return new SystemClock(zone);
    }

    /** The system clock in the default zone. */
    public static Clock systemDefaultZone() {
        return Clock.system(ZoneId.systemDefault());
    }

    /**
     * A **stopped** clock.
     *
     * <p>It is what makes code that looks at the time testable: "now" becomes a value the test
     * chooses, and the result stops depending on when it is run.
     */
    public static Clock fixed(Instant fixedInstant, ZoneId zone) {
        if (fixedInstant == null || zone == null) {
            throw new NullPointerException();
        }
        return new SourceClock(InstantSource.fixed(fixedInstant), zone);
    }

    /** The same clock, shifted by `offsetDuration`. */
    public static Clock offset(Clock baseClock, Duration offsetDuration) {
        if (baseClock == null || offsetDuration == null) {
            throw new NullPointerException();
        }
        if (offsetDuration.isZero()) {
            return baseClock;
        }
        return new SourceClock(InstantSource.offset(baseClock, offsetDuration), baseClock.getZone());
    }

    /**
     * The same clock, advancing **in jumps** of `tickDuration`.
     *
     * @throws IllegalArgumentException if the duration is negative or does not divide a day
     */
    public static Clock tick(Clock baseClock, Duration tickDuration) {
        if (baseClock == null || tickDuration == null) {
            throw new NullPointerException();
        }
        if (tickDuration.isZero()) {
            return baseClock;
        }
        return new SourceClock(InstantSource.tick(baseClock, tickDuration), baseClock.getZone());
    }

    /** A clock that advances in whole milliseconds. */
    public static Clock tickMillis(ZoneId zone) {
        return Clock.tick(Clock.system(zone), Duration.ofMillis(1L));
    }

    /** In whole seconds. */
    public static Clock tickSeconds(ZoneId zone) {
        return Clock.tick(Clock.system(zone), Duration.ofSeconds(1L));
    }

    /** In whole minutes. */
    public static Clock tickMinutes(ZoneId zone) {
        return Clock.tick(Clock.system(zone), Duration.ofMinutes(1L));
    }

    public long millis() {
        return System.currentTimeMillis();
    }

    public static Clock systemUTC() {
        return new SystemClock(ZoneOffset.UTC);
    }
}

// The system clock, reading the VM's wall time. Package-private (the JDK nests it inside Clock;
// the API-shape gate skips it since there is no java.time.SystemClock in the JDK).
//
// It carries the zone because a `Clock` has one: the same instant is a different day in Tokyo and in
// Buenos Aires, and `LocalDate.now(clock)` needs both.
final class SystemClock extends Clock {

    private final ZoneId zone;

    SystemClock(ZoneId zone) {
        this.zone = zone;
    }

    public Instant instant() {
        return Instant.now();
    }

    public ZoneId getZone() {
        return this.zone;
    }

    public Clock withZone(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return zone.equals(this.zone) ? this : new SystemClock(zone);
    }
}
