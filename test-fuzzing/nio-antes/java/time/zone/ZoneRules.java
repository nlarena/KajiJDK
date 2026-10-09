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

    // ---- the third form: the lists the caller gave -------------------------------------------------
    //
    // `of(ZoneOffset, ZoneOffset, List, List, List)` builds rules that do **not** come from the
    // table: the transitions and the recurring rules come from outside. They are kept here and read
    // through the same private accessors that read the table, so the seventeen public methods tell no
    // form from the other. Branching them one by one would have been the other way, and the way for
    // two of them to drift apart.
    //
    // `null` = this is not that form.
    private final ZoneOffsetTransition[] transitions;
    private final ZoneOffsetTransitionRule[] rules;
    // The **standard** offset's transitions: when the zone changed its base time, not its daylight
    // saving. Almost always empty, which is why it is kept apart from the others.
    private final ZoneOffsetTransition[] standardTransitions;
    private final int baseStandard;

    private ZoneRules(int zone, int fixedOffset) {
        this.zone = zone;
        this.fixedOffset = fixedOffset;
        this.transitions = null;
        this.rules = null;
        this.standardTransitions = null;
        this.baseStandard = fixedOffset;
    }

    private ZoneRules(int baseStandard, int baseWall, ZoneOffsetTransition[] standardTransitions,
            ZoneOffsetTransition[] transitions, ZoneOffsetTransitionRule[] rules) {
        // -2 and not -1: `-1` already means "fixed offset", and `isFixedOffset` uses it. A rule set
        // given by lists is **not** fixed even if the lists come in empty... unless it is, and that is
        // decided by `isFixedOffset` looking at the lists, not at the marker.
        this.zone = -2;
        this.fixedOffset = baseWall;
        this.baseStandard = baseStandard;
        this.standardTransitions = standardTransitions;
        this.transitions = transitions;
        this.rules = rules;
    }

    public static ZoneRules of(ZoneOffset offset) {
        return new ZoneRules(-1, offset.getTotalSeconds());
    }

    /**
     * A rule set built with **explicit lists** instead of with the embedded table.
     *
     * <p>It is the factory used by whoever has zone data of their own: a tzdb reader, a test that
     * wants a controlled zone, or a `ZoneRulesProvider` of their own. The three lists say different
     * things and are worth not confusing:
     *
     * <ul>
     *   <li>`standardOffsetTransitionList` -- when the zone changed its **base** time. It is very
     *       rare (a country moving between offsets) and that is why it almost always comes empty.
     *   <li>`transitionList` -- the **historical** changes that have already happened, with an exact
     *       date.
     *   <li>`lastRules` -- the **recurring** rules that hold from there on, with no end date. They
     *       are what keeps the zone having an answer for a year that has not happened yet.
     * </ul>
     *
     * <p>The lists are **copied**: whoever passes them can go on using their own without these rules
     * changing underneath. A rule set that mutated would be no use at all -- `ZoneRules` is shared
     * among every `ZonedDateTime` of that zone.
     *
     * @throws NullPointerException if any argument is `null`
     */
    public static ZoneRules of(ZoneOffset baseStandardOffset, ZoneOffset baseWallOffset,
            List<ZoneOffsetTransition> standardOffsetTransitionList,
            List<ZoneOffsetTransition> transitionList,
            List<ZoneOffsetTransitionRule> lastRules) {
        if (baseStandardOffset == null || baseWallOffset == null
                || standardOffsetTransitionList == null || transitionList == null
                || lastRules == null) {
            throw new NullPointerException();
        }
        return new ZoneRules(baseStandardOffset.getTotalSeconds(),
                baseWallOffset.getTotalSeconds(),
                copyTransitions(standardOffsetTransitionList),
                copyTransitions(transitionList),
                copyRules(lastRules));
    }

    private static ZoneOffsetTransition[] copyTransitions(List<ZoneOffsetTransition> xs) {
        ZoneOffsetTransition[] out = new ZoneOffsetTransition[xs.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = xs.get(i);
            i = i + 1;
        }
        return out;
    }

    private static ZoneOffsetTransitionRule[] copyRules(List<ZoneOffsetTransitionRule> xs) {
        ZoneOffsetTransitionRule[] out = new ZoneOffsetTransitionRule[xs.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = xs.get(i);
            i = i + 1;
        }
        return out;
    }

    // ---- the data layer, common to the three forms --------------------------------------------------
    //
    // This is where it is decided where the numbers come from. Everything above asks through these
    // five.

    private int transitionCount() {
        if (this.transitions != null) {
            return this.transitions.length;
        }
        return this.zone >= 0 ? TzData.transitionCount(this.zone) : 0;
    }

    private int ruleCountOf() {
        if (this.rules != null) {
            return this.rules.length;
        }
        return this.zone >= 0 ? TzData.ruleCount(this.zone) : 0;
    }

    private long epochOf(int i) {
        if (this.transitions != null) {
            return this.transitions[i].toEpochSecond();
        }
        return TzData.transitionEpoch(this.zone, i);
    }

    private int before(int i) {
        if (this.transitions != null) {
            return this.transitions[i].getOffsetBefore().getTotalSeconds();
        }
        return TzData.transitionBefore(this.zone, i);
    }

    private int after(int i) {
        if (this.transitions != null) {
            return this.transitions[i].getOffsetAfter().getTotalSeconds();
        }
        return TzData.transitionAfter(this.zone, i);
    }

    // Package-private: the provider builds these from the embedded table.
    static ZoneRules ofZone(int zoneIndex) {
        return new ZoneRules(zoneIndex, 0);
    }

    public boolean isFixedOffset() {
        // The list form is fixed if it has neither transitions nor rules -- which is exactly the same
        // test a tabulated zone gets. That is why the question is asked of the counters and not of the
        // marker: `-2` says nothing about whether the zone changes.
        if (this.zone == -1) {
            return true;
        }
        return this.transitionCount() == 0 && this.ruleCountOf() == 0;
    }

    /**
     * The offset in force for that **local** date and time.
     *
     * <p>The difference from the `Instant` version is the whole difficulty of time zones: an instant
     * has **always exactly one** answer, and a local time can have two --the hour that repeats when
     * the clock goes back-- or none --the one skipped when it goes forward--.
     *
     * <p>This method returns **one only**, and the contract says which: in an overlap, the one from
     * **before** the change; in a gap, the one from **before** as well. It is a deliberate
     * simplification of the JDK's, and that is why `getValidOffsets` exists, returning the whole
     * list. Whoever needs to tell the three cases apart has to look at the transition's
     * `isGap`/`isOverlap`.
     */
    public ZoneOffset getOffset(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            throw new NullPointerException("localDateTime");
        }
        if (this.zone == -1) {
            return ZoneOffset.ofTotalSeconds(this.fixedOffset);
        }
        // It starts from the offset in force before the first transition and advances while the local
        // time falls after the change. Comparing in local terms and not in instants is exactly what
        // makes a gap and an overlap give the "before" answer.
        int count = this.transitionCount();
        if (count == 0) {
            return this.getOffset(java.time.Instant.ofEpochSecond(0L));
        }
        int result = this.before(0);
        int i = 0;
        while (i < count) {
            int before = this.before(i);
            long changeEpoch = this.epochOf(i);
            // The instant of the change, read on the wall clock of **before**.
            long changeLocal = changeEpoch + (long) before;
            long requestedLocal = ZoneMath.toEpochSecond(localDateTime, 0);
            if (requestedLocal < changeLocal) {
                return ZoneOffset.ofTotalSeconds(before);
            }
            result = this.after(i);
            i = i + 1;
        }
        return ZoneOffset.ofTotalSeconds(result);
    }

    // The offset in force at an instant. Exactly one answer, always.
    public ZoneOffset getOffset(Instant instant) {
        return ZoneOffset.ofTotalSeconds(this.offsetSecondsAt(instant.getEpochSecond()));
    }

    private int offsetSecondsAt(long epochSecond) {
        int result = this.fixedOffset;
        if (this.zone != -1) {
            int count = this.transitionCount();
            if (count == 0) {
                // With no transitions: the tabulated one knows nothing and gives zero; the list one
                // has its base wall offset, which is exactly what the caller said.
                result = this.transitions != null ? this.fixedOffset : 0;
                if (this.ruleCountOf() > 0) {
                    result = this.offsetFromRules(epochSecond, result);
                }
            } else if (epochSecond < this.epochOf(0)) {
                result = this.before(0);
            } else {
                // Last transition at or before the instant.
                int lo = 0;
                int hi = count - 1;
                int found = 0;
                while (lo <= hi) {
                    int mid = (lo + hi) / 2;
                    if (this.epochOf(mid) <= epochSecond) {
                        found = mid;
                        lo = mid + 1;
                    } else {
                        hi = mid - 1;
                    }
                }
                result = this.after(found);
                // Past the tabulated data the recurring rules take over.
                if (found == count - 1 && this.ruleCountOf() > 0) {
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
            while (i < this.ruleCountOf()) {
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
        if (this.rules != null) {
            return this.rules[i];
        }
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
        if (this.zone != -1) {
            int count = this.transitionCount();
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
        // The list form genuinely knows: it starts at the base standard offset and shifts it at every
        // **standard** transition that has already happened. It is the only one of the three that can
        // answer this without guessing.
        if (this.standardTransitions != null) {
            int result = this.baseStandard;
            long epoch = instant.getEpochSecond();
            int i = 0;
            while (i < this.standardTransitions.length) {
                if (this.standardTransitions[i].toEpochSecond() > epoch) {
                    break;
                }
                result = this.standardTransitions[i].getOffsetAfter().getTotalSeconds();
                i = i + 1;
            }
            return ZoneOffset.ofTotalSeconds(result);
        }
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
        int current = this.offsetSecondsAt(instant.getEpochSecond());
        int standard = this.getStandardOffset(instant).getTotalSeconds();
        return Duration.ofSeconds((long) (current - standard));
    }

    public boolean isDaylightSavings(Instant instant) {
        int current = this.offsetSecondsAt(instant.getEpochSecond());
        int standard = this.getStandardOffset(instant).getTotalSeconds();
        return current != standard;
    }

    public ZoneOffsetTransition nextTransition(Instant instant) {
        ZoneOffsetTransition found = null;
        if (this.zone != -1) {
            long epoch = instant.getEpochSecond();
            int count = this.transitionCount();
            int i = 0;
            while (i < count) {
                if (this.epochOf(i) > epoch) {
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
        if (this.zone != -1) {
            long epoch = instant.getEpochSecond();
            int i = this.transitionCount() - 1;
            while (i >= 0) {
                if (this.epochOf(i) < epoch) {
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
        if (this.transitions != null) {
            return this.transitions[i];
        }
        return ZoneOffsetTransition.ofRaw(TzData.transitionEpoch(this.zone, i),
                TzData.transitionBefore(this.zone, i), TzData.transitionAfter(this.zone, i));
    }

    public List<ZoneOffsetTransition> getTransitions() {
        List<ZoneOffsetTransition> out = new ArrayList<ZoneOffsetTransition>();
        int i = 0;
        while (i < this.transitionCount()) {
            out.add(this.transition(i));
            i = i + 1;
        }
        return out;
    }

    public List<ZoneOffsetTransitionRule> getTransitionRules() {
        List<ZoneOffsetTransitionRule> out = new ArrayList<ZoneOffsetTransitionRule>();
        int i = 0;
        while (i < this.ruleCountOf()) {
            out.add(this.rule(i));
            i = i + 1;
        }
        return out;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ZoneRules) {
            ZoneRules o = (ZoneRules) other;
            if (this.zone != o.zone || this.fixedOffset != o.fixedOffset) {
                return false;
            }
            // Two sets built from lists are equal if their lists are. Comparing them by identity
            // --which is what the bare `zone == zone` did-- would say that two `of(...)` with the same
            // data are different, and they are not.
            if (this.zone == -2) {
                return this.baseStandard == o.baseStandard
                        && this.getTransitions().equals(o.getTransitions())
                        && this.getTransitionRules().equals(o.getTransitionRules());
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int h = this.zone ^ this.fixedOffset;
        if (this.zone == -2) {
            h = h ^ this.baseStandard ^ this.getTransitions().hashCode();
        }
        return h;
    }

    public String toString() {
        if (this.zone == -1) {
            return "ZoneRules[fixed=" + ZoneOffset.ofTotalSeconds(this.fixedOffset).toString() + "]";
        }
        if (this.zone == -2) {
            return "ZoneRules[" + this.transitionCount() + " transitions, "
                    + this.ruleCountOf() + " rules]";
        }
        return "ZoneRules[" + TzData.zoneIds()[this.zone] + "]";
    }
}
