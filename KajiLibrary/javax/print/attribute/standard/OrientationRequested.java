package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * How the content lies on the sheet.
 *
 * <p>The {@code REVERSE_} variants are the same orientation turned half a turn; they matter when
 * the job is two-sided or when the staple has to end up on a given side. The numbering starts at 3
 * because it is IPP's.
 */
public final class OrientationRequested extends EnumSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -4447437289862822276L;

    public static final OrientationRequested PORTRAIT = new OrientationRequested(3);

    public static final OrientationRequested LANDSCAPE = new OrientationRequested(4);

    public static final OrientationRequested REVERSE_LANDSCAPE = new OrientationRequested(5);

    public static final OrientationRequested REVERSE_PORTRAIT = new OrientationRequested(6);

    private static final String[] myStringTable = {
        "portrait",
        "landscape",
        "reverse-landscape",
        "reverse-portrait",
    };

    private static final OrientationRequested[] myEnumValueTable = {
        PORTRAIT,
        LANDSCAPE,
        REVERSE_LANDSCAPE,
        REVERSE_PORTRAIT,
    };

    protected OrientationRequested(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /** IPP numbers this category from 3; row 0 of the tables is that 3. */
    protected int getOffset() {
        return 3;
    }

    public final Class<? extends Attribute> getCategory() {
        return OrientationRequested.class;
    }

    public final String getName() {
        return "orientation-requested";
    }
}
