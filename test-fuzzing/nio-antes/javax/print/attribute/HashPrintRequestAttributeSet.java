package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.HashPrintRequestAttributeSet -- a {@link HashAttributeSet}
 * that only accepts {@link PrintRequestAttribute}s, that is print request attributes.
 *
 * <p>No body, just like its three siblings: the constructors pass {@code
 * PrintRequestAttribute.class} to the base class and {@code HashAttributeSet.add} does the
 * restriction. See {@link HashDocAttributeSet} for the full explanation of the mechanism.
 */
public class HashPrintRequestAttributeSet extends HashAttributeSet
        implements PrintRequestAttributeSet, Serializable {

    private static final long serialVersionUID = 2364756266107751933L;

    /** Empty. */
    public HashPrintRequestAttributeSet() {
        super(PrintRequestAttribute.class);
    }

    /** With one attribute. NullPointerException if it is null. */
    public HashPrintRequestAttributeSet(PrintRequestAttribute attribute) {
        super(attribute, PrintRequestAttribute.class);
    }

    /** With another set of the same type's attributes. A null set gives the empty set. */
    public HashPrintRequestAttributeSet(PrintRequestAttributeSet attributes) {
        super(attributes, PrintRequestAttribute.class);
    }

    /** With the array's, in order: if there are two of the same category the last one wins. */
    public HashPrintRequestAttributeSet(PrintRequestAttribute[] attributes) {
        super(attributes, PrintRequestAttribute.class);
    }
}
