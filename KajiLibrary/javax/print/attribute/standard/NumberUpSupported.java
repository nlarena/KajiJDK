package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/**
 * Which values of {@link NumberUp} the printer accepts.
 *
 * <p>See the family header in {@link CopiesSupported} for the mechanism. This is the case where the
 * set really pays off: the typical thing is supporting 1, 2, 4, 6, 9 and 16 --the powers and
 * squares that divide the sheet well-- and not a contiguous range, so the {@code int[][]}
 * constructor is needed.
 *
 * <p>The minimum is 1, as in {@link NumberUp}.
 */
public final class NumberUpSupported extends SetOfIntegerSyntax
    implements SupportedValuesAttribute {

    private static final long serialVersionUID = -1041573395759141805L;

    /**
     * The raw ranges, in any order and overlapping: the base canonicalizes them before this
     * constructor checks them.
     */
    public NumberUpSupported(int[][] members) {
        super(members);
        if (members == null) {
            throw new NullPointerException("members is null");
        }
        int[][] myMembers = getMembers();
        int n = myMembers.length;
        if (n == 0) {
            throw new IllegalArgumentException("members is zero-length");
        }
        for (int i = 0; i < n; i++) {
            if (myMembers[i][0] < 1) {
                throw new IllegalArgumentException("Number up value must be > 0");
            }
        }
    }

    public NumberUpSupported(int member) {
        super(member);
        if (member < 1) {
            throw new IllegalArgumentException("Number up value must be > 0");
        }
    }

    public NumberUpSupported(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range specified");
        } else if (lowerBound < 1) {
            throw new IllegalArgumentException("Number up value must be > 0");
        }
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof NumberUpSupported;
    }

    public final Class<? extends Attribute> getCategory() {
        return NumberUpSupported.class;
    }

    public final String getName() {
        return "number-up-supported";
    }
}
