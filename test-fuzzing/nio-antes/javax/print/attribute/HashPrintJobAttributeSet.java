package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.HashPrintJobAttributeSet -- a {@link HashAttributeSet} that
 * only accepts {@link PrintJobAttribute}s, that is print job attributes.
 *
 * <p>No body, just like its three siblings: the constructors pass {@code PrintJobAttribute.class}
 * to the base class and {@code HashAttributeSet.add} does the restriction. See {@link
 * HashDocAttributeSet} for the full explanation of the mechanism.
 */
public class HashPrintJobAttributeSet extends HashAttributeSet
        implements PrintJobAttributeSet, Serializable {

    private static final long serialVersionUID = -4204473656070350348L;

    /** Empty. */
    public HashPrintJobAttributeSet() {
        super(PrintJobAttribute.class);
    }

    /** With one attribute. NullPointerException if it is null. */
    public HashPrintJobAttributeSet(PrintJobAttribute attribute) {
        super(attribute, PrintJobAttribute.class);
    }

    /** With another set of the same type's attributes. A null set gives the empty set. */
    public HashPrintJobAttributeSet(PrintJobAttributeSet attributes) {
        super(attributes, PrintJobAttribute.class);
    }

    /** With the array's, in order: if there are two of the same category the last one wins. */
    public HashPrintJobAttributeSet(PrintJobAttribute[] attributes) {
        super(attributes, PrintJobAttribute.class);
    }
}
