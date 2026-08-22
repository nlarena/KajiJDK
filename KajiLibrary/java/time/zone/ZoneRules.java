package java.time.zone;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

// KajiLibrary's java.time.zone.ZoneRules — the rules that say what a zone's offset IS at a given
// moment. This is the engine the whole package exists for.
//
// Two directions, and they are not symmetric:
//
//   instant -> offset   always exactly one answer. Binary search the transition list, fall through
//                       to the recurring rules for years past the tabulated data.
//   local   -> offset   ZERO answers inside a gap, TWO inside an overlap. That asymmetry is why
//                       `getValidOffsets` returns a List and not a ZoneOffset.
//
// The data is a documented subset of tzdb — see TzData for which zones and why.
public final class ZoneRules {

    // -1 marks "not a tabulated zone": a fixed-offset rule set, built by of(ZoneOffset).
    private final int zone;
    private final int fixedOffset;

    private ZoneRules(int zone, int fixedOffset) {
        this.zone = zone;
        this.fixedOffset = fixedOffset;
    }

    public static ZoneRules of(ZoneOffset offset) {
        return new ZoneRules(-1, offset.getTotalSeconds());
    }

    // Package-private: the provider builds these from the embedded table.
    static ZoneRules ofZone(int zoneIndex) {
        return new ZoneRules(zoneIndex, 0);
    }

    public boolean isFixedOffset() {
        boolean fixed = this.zone < 0;
        if (!fixed) {
            fixed = TzData.transitionCount(this.zone) == 0 && TzData.ruleCount(this.zone) == 0;
        }
        return fixed;
    }

    // The offset in force at an instant. Exactly one answer, always.
    public ZoneOffset getOffset(Instant instant) {
        return ZoneOffset.ofTotalSeconds(this.offsetSecondsAt(instant.getEpochSecond()));
    }

    private int offsetSecondsAt(long epochSecond) {
        int result = this.fixedOffset;
        if (this.zone >= 0) {
            int count = TzData.transitionCount(this.zone);
            if (count == 0) {
                result = 0;
            } else if (epochSecond < TzData.transitionEpoch(this.zone, 0)) {
                result = TzData.transitionBefore(this.zone, 0);
            } else {
                // Last transition at or before the instant.
                int lo = 0;
                int hi = count - 1;
                int found = 0;
                while (lo <= hi) {
                    int mid = (lo + hi) / 2;
                    if (TzData.transitionEpoch(this.zone, mid) <= epochSecond) {
                        found = mid;
                        lo = mid + 1;
                    } else {
                        hi = mid - 1;
                    }
                }
                result = TzData.transitionAfter(this.zone, found);
                // Past the tabulated data the recurring rules take over.
                if (found == count - 1 && TzData.ruleCount(this.zone) > 0) {
                    result = this.offsetFromRules(epochSecond, result);
                }
            }
        }
        return result;
    }

    // Walk the recurring rules for the instant's year (and the previous one, since a southern
    // hemisphere rule set puts the year's first transition in the middle of summer).
    private int offsetFromRules(long epochSecond, int fallback) {
        LocalDateTime approx = ZoneMath.ofEpochSecond(epochSecond, 0);
        int year = approx.getYear();
        int result = fallback;
        long best = Long.MIN_VALUE + 1L;
        int y = year - 1;
        while (y <= year + 1) {
            int i = 0;
            while (i < TzData.ruleCount(this.zone)) {
                ZoneOffsetTransition t = this.rule(i).createTransition(y);
                long at = t.toEpochSecond();
                if (at <= epochSecond && at > best) {
                    best = at;
                    result = t.getOffsetAfter().getTotalSeconds();
                }
                i = i + 1;
            }
            y = y + 1;
        }
        return result;
    }

    private ZoneOffsetTransitionRule rule(int i) {
        return new ZoneOffsetTransitionRule(
                TzData.ruleField(this.zone, i, 0),
                TzData.ruleField(this.zone, i, 1),
                TzData.ruleField(this.zone, i, 2),
                TzData.ruleField(this.zone, i, 3),
                TzData.ruleField(this.zone, i, 4) != 0,
                TzData.ruleField(this.zone, i, 5),
                TzData.ruleField(this.zone, i, 6),
                TzData.ruleField(this.zone, i, 7),
                TzData.ruleField(this.zone, i, 8));
    }

    // The offsets a LOCAL reading could mean: one normally, none in a gap, two in an overlap.
    public List<ZoneOffset> getValidOffsets(LocalDateTime localDateTime) {
        List<ZoneOffset> result = new ArrayList<ZoneOffset>();
        ZoneOffsetTransition trans = this.getTransition(localDateTime);
        if (trans == null) {
            result.add(ZoneOffset.ofTotalSeconds(this.offsetSecondsForLocal(localDateTime)));
        } else if (trans.isOverlap()) {
            result.add(trans.getOffsetBefore());
            result.add(trans.getOffsetAfter());
        }
        return result;
    }

    // The transition straddling this local reading, or null when the reading is unambiguous.
    public ZoneOffsetTransition getTransition(LocalDateTime localDateTime) {
        ZoneOffsetTransition found = null;
        if (this.zone >= 0) {
            int count = TzData.transitionCount(this.zone);
            int i = 0;
            while (i < count) {
                ZoneOffsetTransition t = this.transition(i);
                LocalDateTime before = t.getDateTimeBefore();
                LocalDateTime after = t.getDateTimeAfter();
                LocalDateTime low = before;
                LocalDateTime high = after;
                if (t.isOverlap()) {
                    low = after;
                    high = before;
                }
                if (localDateTime.compareTo(low) >= 0 && localDateTime.compareTo(high) < 0) {
                    found = t;
                    i = count;
                } else {
                    i = i + 1;
                }
            }
        }
        return found;
    }

    private int offsetSecondsForLocal(LocalDateTime localDateTime) {
        // A local reading is resolved by trying the offsets around it: take the offset in force at
        // the instant the reading would name under a first guess, then confirm.
        int guess = this.offsetSecondsAt(ZoneMath.toEpochSecond(localDateTime, 0));
        long epoch = ZoneMath.toEpochSecond(localDateTime, guess);
        return this.offsetSecondsAt(epoch);
    }

    public boolean isValidOffset(LocalDateTime localDateTime, ZoneOffset offset) {
        List<ZoneOffset> valid = this.getValidOffsets(localDateTime);
        boolean ok = false;
        int i = 0;
        while (i < valid.size()) {
            ZoneOffset candidate = valid.get(i);
            if (candidate.getTotalSeconds() == offset.getTotalSeconds()) {
                ok = true;
            }
            i = i + 1;
        }
        return ok;
    }

    // The offset ignoring daylight saving — what the zone would use all year.
    public ZoneOffset getStandardOffset(Instant instant) {
        ZoneOffset result = ZoneOffset.ofTotalSeconds(this.fixedOffset);
        if (this.zone >= 0) {
            int ruleCount = TzData.ruleCount(this.zone);
            if (ruleCount > 0) {
                result = this.rule(0).getStandardOffset();
            } else {
                result = this.getOffset(instant);
            }
        }
        return result;
    }

    public Duration getDaylightSavings(Instant instant) {
        int actual = this.offsetSecondsAt(instant.getEpochSecond());
        int standard = this.getStandardOffset(instant).getTotalSeconds();
        return Duration.ofSeconds((long) (actual - standard));
    }

    public boolean isDaylightSavings(Instant instant) {
        int actual = this.offsetSecondsAt(instant.getEpochSecond());
        int standard = this.getStandardOffset(instant).getTotalSeconds();
        return actual != standard;
    }

    public ZoneOffsetTransition nextTransition(Instant instant) {
        ZoneOffsetTransition found = null;
        if (this.zone >= 0) {
            long epoch = instant.getEpochSecond();
            int count = TzData.transitionCount(this.zone);
            int i = 0;
            while (i < count) {
                if (TzData.transitionEpoch(this.zone, i) > epoch) {
                    found = this.transition(i);
                    i = count;
                } else {
                    i = i + 1;
                }
            }
        }
        return found;
    }

    public ZoneOffsetTransition previousTransition(Instant instant) {
        ZoneOffsetTransition found = null;
        if (this.zone >= 0) {
            long epoch = instant.getEpochSecond();
            int i = TzData.transitionCount(this.zone) - 1;
            while (i >= 0) {
                if (TzData.transitionEpoch(this.zone, i) < epoch) {
                    found = this.transition(i);
                    i = -1;
                } else {
                    i = i - 1;
                }
            }
        }
        return found;
    }

    private ZoneOffsetTransition transition(int i) {
        return ZoneOffsetTransition.ofRaw(TzData.transitionEpoch(this.zone, i),
                TzData.transitionBefore(this.zone, i), TzData.transitionAfter(this.zone, i));
    }

    public List<ZoneOffsetTransition> getTransitions() {
        List<ZoneOffsetTransition> out = new ArrayList<ZoneOffsetTransition>();
        if (this.zone >= 0) {
            int i = 0;
            while (i < TzData.transitionCount(this.zone)) {
                out.add(this.transition(i));
                i = i + 1;
            }
        }
        return out;
    }

    public List<ZoneOffsetTransitionRule> getTransitionRules() {
        List<ZoneOffsetTransitionRule> out = new ArrayList<ZoneOffsetTransitionRule>();
        if (this.zone >= 0) {
            int i = 0;
            while (i < TzData.ruleCount(this.zone)) {
                out.add(this.rule(i));
                i = i + 1;
            }
        }
        return out;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ZoneRules) {
            ZoneRules o = (ZoneRules) other;
            return this.zone == o.zone && this.fixedOffset == o.fixedOffset;
        }
        return false;
    }

    public int hashCode() {
        return this.zone ^ this.fixedOffset;
    }

    public String toString() {
        if (this.zone < 0) {
            return "ZoneRules[fixed=" + ZoneOffset.ofTotalSeconds(this.fixedOffset).toString() + "]";
        }
        return "ZoneRules[" + TzData.zoneIds()[this.zone] + "]";
    }
}
