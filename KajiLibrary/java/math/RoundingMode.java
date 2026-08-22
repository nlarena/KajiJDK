package java.math;

// KajiLibrary's java.math.RoundingMode — the eight ways to drop digits you cannot keep.
//
// They split into three families, and the split is the useful thing to remember:
//   directed    UP / DOWN / CEILING / FLOOR — always go one way (away from zero, toward zero,
//               toward +inf, toward -inf). No tie-breaking needed.
//   half        HALF_UP / HALF_DOWN / HALF_EVEN — go to the nearest, and only differ ON THE TIE.
//               HALF_EVEN is the default of IEEE-754 and of MathContext because it does not drift:
//               it rounds ties up and down equally often.
//   assertion   UNNECESSARY — "this operation must be exact"; if it is not, throw.
public enum RoundingMode {

    // Each constant carries its own name. `toString()`/`name()` are inherited from java.lang.Enum,
    // and a call to a method inherited from an EXTERNAL superclass is silently dropped by our
    // compiler (finding #120) — so anything that needs the name reads `label()` instead. It is
    // package-private, so it is implementation and the API-shape gate ignores it.
    UP("UP"),
    DOWN("DOWN"),
    CEILING("CEILING"),
    FLOOR("FLOOR"),
    HALF_UP("HALF_UP"),
    HALF_DOWN("HALF_DOWN"),
    HALF_EVEN("HALF_EVEN"),
    UNNECESSARY("UNNECESSARY");

    private final String label;

    RoundingMode(String label) {
        this.label = label;
    }

    String label() {
        return this.label;
    }

    // The legacy `BigDecimal.ROUND_*` integer constants, which predate this enum. The mapping is
    // positional, but it is written out rather than derived from `ordinal()`: a call to a method
    // inherited from an external superclass (java.lang.Enum) is silently dropped by our compiler
    // (finding #120).
    public static RoundingMode valueOf(int rm) {
        if (rm == 0) {
            return UP;
        }
        if (rm == 1) {
            return DOWN;
        }
        if (rm == 2) {
            return CEILING;
        }
        if (rm == 3) {
            return FLOOR;
        }
        if (rm == 4) {
            return HALF_UP;
        }
        if (rm == 5) {
            return HALF_DOWN;
        }
        if (rm == 6) {
            return HALF_EVEN;
        }
        if (rm == 7) {
            return UNNECESSARY;
        }
        throw new IllegalArgumentException("argument out of range");
    }
}
