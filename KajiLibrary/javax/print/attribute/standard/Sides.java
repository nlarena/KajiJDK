package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Whether it prints on one side or on both, and along which edge the sheet is turned.
 *
 * <p>The edge matters because it decides which way the sheet has to be turned to read it: along the
 * long edge the second side comes out upright as in a book, along the short one it comes out upside
 * down as in a notepad.
 *
 * <p>{@code DUPLEX} and {@code TUMBLE} are <b>aliases</b>, not new values: they are the same object
 * as {@code TWO_SIDED_LONG_EDGE} and {@code TWO_SIDED_SHORT_EDGE}, so {@code DUPLEX ==
 * TWO_SIDED_LONG_EDGE} gives true and the name table has three rows, not five.
 */
public final class Sides extends EnumSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -6890309414893262822L;

    public static final Sides ONE_SIDED = new Sides(0);

    public static final Sides TWO_SIDED_LONG_EDGE = new Sides(1);

    public static final Sides TWO_SIDED_SHORT_EDGE = new Sides(2);

    /** Alias of {@link #TWO_SIDED_LONG_EDGE}: the same object, not a new value. */
    public static final Sides DUPLEX = TWO_SIDED_LONG_EDGE;

    /** Alias of {@link #TWO_SIDED_SHORT_EDGE}: the same object, not a new value. */
    public static final Sides TUMBLE = TWO_SIDED_SHORT_EDGE;

    private static final String[] myStringTable = {
        "one-sided",
        "two-sided-long-edge",
        "two-sided-short-edge",
    };

    private static final Sides[] myEnumValueTable = {
        ONE_SIDED,
        TWO_SIDED_LONG_EDGE,
        TWO_SIDED_SHORT_EDGE,
    };

    protected Sides(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return Sides.class;
    }

    public final String getName() {
        return "sides";
    }
}
