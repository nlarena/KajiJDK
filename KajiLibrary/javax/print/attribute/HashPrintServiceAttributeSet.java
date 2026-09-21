package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.HashPrintServiceAttributeSet -- a {@link HashAttributeSet}
 * that only accepts {@link PrintServiceAttribute}s, that is print service attributes.
 *
 * <p>No body, just like its three siblings: the constructors pass {@code
 * PrintServiceAttribute.class} to the base class and {@code HashAttributeSet.add} does the
 * restriction. See {@link HashDocAttributeSet} for the full explanation of the mechanism.
 */
public class HashPrintServiceAttributeSet extends HashAttributeSet
        implements PrintServiceAttributeSet, Serializable {

    private static final long serialVersionUID = 6642904616179203070L;

    /** Empty. */
    public HashPrintServiceAttributeSet() {
        super(PrintServiceAttribute.class);
    }

    /** With one attribute. NullPointerException if it is null. */
    public HashPrintServiceAttributeSet(PrintServiceAttribute attribute) {
        super(attribute, PrintServiceAttribute.class);
    }

    /** With another set of the same type's attributes. A null set gives the empty set. */
    public HashPrintServiceAttributeSet(PrintServiceAttributeSet attributes) {
        super(attributes, PrintServiceAttribute.class);
    }

    /** With the array's, in order: if there are two of the same category the last one wins. */
    public HashPrintServiceAttributeSet(PrintServiceAttribute[] attributes) {
        super(attributes, PrintServiceAttribute.class);
    }
}
