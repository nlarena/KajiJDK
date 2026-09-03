package java.time.zone;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

// KajiLibrary's java.time.zone.ZoneOffsetTransition — the moment a zone's offset changes.
//
// The two interesting shapes it can take, and the reason local time is not a total order:
//
//   GAP     the clock jumps forward. 2026-03-29 02:30 simply DOES NOT EXIST in Madrid.
//   OVERLAP the clock jumps back. 2026-10-25 02:30 happens TWICE, at two different instants.
//
// `isGap()` and `isOverlap()` are the whole point of the class: they are what a caller needs to
// know before pretending a LocalDateTime names one instant.
//
// Implementa `Comparable`, que es lo que corresponde: la clase ya declaraba `compareTo`, y sin la
// interfaz ese metodo no le sirve a nadie que ordene por la via generica --un `TreeSet`, un
// `Collections.sort`-- porque el puente `compareTo(Object)` no se emitia. Ordena por instante.
public final class ZoneOffsetTransition implements Comparable<ZoneOffsetTransition> {

    private final long epochSecond;
    private final int offsetBefore;
    private final int offsetAfter;

    private ZoneOffsetTransition(long epochSecond, int offsetBefore, int offsetAfter) {
        this.epochSecond = epochSecond;
        this.offsetBefore = offsetBefore;
        this.offsetAfter = offsetAfter;
    }

    public static ZoneOffsetTransition of(LocalDateTime transition, ZoneOffset offsetBefore,
            ZoneOffset offsetAfter) {
        int before = offsetBefore.getTotalSeconds();
        long epoch = ZoneMath.toEpochSecond(transition, before);
        return new ZoneOffsetTransition(epoch, before, offsetAfter.getTotalSeconds());
    }

    // Package-private factory used by ZoneRules, which already holds raw seconds.
    static ZoneOffsetTransition ofRaw(long epochSecond, int offsetBefore, int offsetAfter) {
        return new ZoneOffsetTransition(epochSecond, offsetBefore, offsetAfter);
    }

    public Instant getInstant() {
        return Instant.ofEpochSecond(this.epochSecond);
    }

    public long toEpochSecond() {
        return this.epochSecond;
    }

    // The local reading just before the change, and just after it. In a gap these two are the
    // start and end of the missing interval; in an overlap, of the repeated one.
    public LocalDateTime getDateTimeBefore() {
        return ZoneMath.ofEpochSecond(this.epochSecond, this.offsetBefore);
    }

    public LocalDateTime getDateTimeAfter() {
        return ZoneMath.ofEpochSecond(this.epochSecond, this.offsetAfter);
    }

    public ZoneOffset getOffsetBefore() {
        return ZoneOffset.ofTotalSeconds(this.offsetBefore);
    }

    public ZoneOffset getOffsetAfter() {
        return ZoneOffset.ofTotalSeconds(this.offsetAfter);
    }

    // How much local time was lost (gap) or repeated (overlap).
    public Duration getDuration() {
        return Duration.ofSeconds((long) (this.offsetAfter - this.offsetBefore));
    }

    public boolean isGap() {
        return this.offsetAfter > this.offsetBefore;
    }

    public boolean isOverlap() {
        return this.offsetAfter < this.offsetBefore;
    }

    // In an overlap BOTH offsets are valid readings of the same local time; in a gap neither is.
    public boolean isValidOffset(ZoneOffset offset) {
        boolean valid = false;
        if (this.isOverlap()) {
            int secs = offset.getTotalSeconds();
            valid = secs == this.offsetBefore || secs == this.offsetAfter;
        }
        return valid;
    }

    public int compareTo(ZoneOffsetTransition other) {
        int result = 0;
        if (this.epochSecond < other.epochSecond) {
            result = -1;
        } else if (this.epochSecond > other.epochSecond) {
            result = 1;
        }
        return result;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ZoneOffsetTransition) {
            ZoneOffsetTransition o = (ZoneOffsetTransition) other;
            return this.epochSecond == o.epochSecond
                    && this.offsetBefore == o.offsetBefore
                    && this.offsetAfter == o.offsetAfter;
        }
        return false;
    }

    public int hashCode() {
        return (int) (this.epochSecond ^ (this.epochSecond >>> 32)) ^ this.offsetBefore ^ this.offsetAfter;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Transition[");
        if (this.isGap()) {
            buf.append("Gap");
        } else {
            buf.append("Overlap");
        }
        buf.append(" at ");
        buf.append(this.getDateTimeBefore().toString());
        buf.append(this.getOffsetBefore().toString());
        buf.append(" to ");
        buf.append(this.getOffsetAfter().toString());
        buf.append("]");
        return buf.toString();
    }
}
