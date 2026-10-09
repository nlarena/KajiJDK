package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/*
 * FAMILY HEADER -- this package's {@code SetOfIntegerSyntax} attributes.
 *
 * <p>These carry not a number but a <b>set</b> of numbers, kept as a list of ranges. All the
 * arithmetic --sorting, merging the ranges that touch, discarding the empty ones-- is in {@link
 * javax.print.attribute.SetOfIntegerSyntax SetOfIntegerSyntax}, which canonicalizes in the
 * constructor. The only thing each subclass here adds is <b>which values are legal</b>, and it does
 * so after calling {@code super}, looking at the already canonical result.
 *
 * <p>That order matters and is observable: {@code new PageRanges("5-1")} does not fail because of
 * the 5 or the 1 but because {@code 5-1} is an empty range, the canonicalization discards it and a
 * set with no elements is left --which is what the subclass rejects. That is why the message speaks
 * of zero length and not of a value out of range.
 *
 * <p>Five of the six are <em>supported values</em> attributes: the printer's answer to "which
 * numbers can I ask for", the set that corresponds to a loose {@code IntegerSyntax} ({@link
 * CopiesSupported} against {@link Copies}). The sixth, {@link PageRanges}, is not: that one is a
 * request, and it is the only one in the family that can also be built from text.
 */

/**
 * Which quantities of {@link Copies} the printer accepts.
 *
 * <p>It is rarely a truly contiguous range: a printer that supports 1 to 99 says so, but the set
 * exists for the ones that only accept some loose values.
 */
public final class CopiesSupported extends SetOfIntegerSyntax implements SupportedValuesAttribute {

    private static final long serialVersionUID = 6927711687034846001L;

    /** The one-element set: the printer accepts exactly that quantity. */
    public CopiesSupported(int member) {
        super(member);
        if (member < 1) {
            throw new IllegalArgumentException("Copies value < 1 specified");
        }
    }

    public CopiesSupported(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range specified");
        } else if (lowerBound < 1) {
            throw new IllegalArgumentException("Copies value < 1 specified");
        }
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof CopiesSupported;
    }

    public final Class<? extends Attribute> getCategory() {
        return CopiesSupported.class;
    }

    public final String getName() {
        return "copies-supported";
    }
}
