package java.time;

// KajiLibrary's java.time.InstantSource — a source of the current instant, the time-zone-free
// supertype of Clock (a Clock is an InstantSource that also knows a zone).
@FunctionalInterface
public interface InstantSource {

    Instant instant();

    default long millis() {
        return instant().toEpochMilli();
    }

    /**
     * A `Clock` with this source and that zone.
     *
     * <p>It is the bridge from `InstantSource` to `Clock`: the former only knows **when**, and a
     * `Clock` knows **where** as well, which is what `LocalDate.now()` needs.
     */
    default java.time.Clock withZone(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return new SourceClock(this, zone);
    }

    /** The system clock. */
    static InstantSource system() {
        return java.time.Clock.systemUTC();
    }

    /**
     * A source **stopped** at that instant.
     *
     * <p>It is what makes code that looks at the time testable: with a fixed source, "now" is a value
     * the test chooses, and the result stops depending on when it is run.
     */
    static InstantSource fixed(Instant fixedInstant) {
        if (fixedInstant == null) {
            throw new NullPointerException("fixedInstant");
        }
        return new FixedSource(fixedInstant);
    }

    /** The same source, shifted by `offsetDuration`. */
    static InstantSource offset(InstantSource baseSource, Duration offsetDuration) {
        if (baseSource == null || offsetDuration == null) {
            throw new NullPointerException();
        }
        if (offsetDuration.isZero()) {
            return baseSource;
        }
        return new OffsetSource(baseSource, offsetDuration);
    }

    /**
     * The same source, **truncated** to multiples of `tickDuration`.
     *
     * <p>A clock that advances in jumps: with a one-second tick, the nanos always come out zero. It
     * serves to make two nearby readings give the same value on purpose.
     *
     * @throws IllegalArgumentException if `tickDuration` is negative or does not divide a day
     */
    static InstantSource tick(InstantSource baseSource, Duration tickDuration) {
        if (baseSource == null || tickDuration == null) {
            throw new NullPointerException();
        }
        if (tickDuration.isNegative()) {
            throw new IllegalArgumentException("Tick duration must not be negative");
        }
        long nanos = tickDuration.toNanos();
        if (nanos == 0L) {
            return baseSource;
        }
        if (nanos % 1000000L != 0L && 1000000000L % nanos != 0L) {
            throw new IllegalArgumentException("Invalid tick duration");
        }
        return new TickSource(baseSource, nanos);
    }
}

// The three derived sources and the clock that wraps them. They go as package-private classes and
// not as nested ones because a class nested inside a generic interface does not resolve properly
// yet.

final class FixedSource implements InstantSource {

    private final Instant fixed;

    FixedSource(Instant fixed) {
        this.fixed = fixed;
    }

    public Instant instant() {
        return this.fixed;
    }
}

final class OffsetSource implements InstantSource {

    private final InstantSource base;
    private final Duration shift;

    OffsetSource(InstantSource base, Duration shift) {
        this.base = base;
        this.shift = shift;
    }

    public Instant instant() {
        return this.base.instant().plus(this.shift);
    }
}

final class TickSource implements InstantSource {

    private final InstantSource base;
    private final long tickNanos;

    TickSource(InstantSource base, long tickNanos) {
        this.base = base;
        this.tickNanos = tickNanos;
    }

    public Instant instant() {
        Instant i = this.base.instant();
        long nanos = i.getNano();
        // It truncates **downwards** within the second, which is what keeps the clock from ever going
        // backwards: every reading lands on the same tick or on a later one.
        long leftOver = nanos % this.tickNanos;
        return i.minusNanos(leftOver);
    }
}

// The `Clock` `InstantSource.withZone` returns: the source supplies the instant and the zone the place.
final class SourceClock extends java.time.Clock {

    private final InstantSource source;
    private final ZoneId zone;

    SourceClock(InstantSource source, ZoneId zone) {
        this.source = source;
        this.zone = zone;
    }

    public Instant instant() {
        return this.source.instant();
    }

    public ZoneId getZone() {
        return this.zone;
    }

    public java.time.Clock withZone(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return zone.equals(this.zone) ? this : new SourceClock(this.source, zone);
    }
}
