package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Whether the job carries separator sheets --the cover with the owner's name-- between one job and
 * the next.
 *
 * <p>{@code STANDARD} lets the site decide what that sheet is like; the class is not final
 * precisely so that a site can add its own.
 */
public class JobSheets extends EnumSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -4735258056132519759L;

    public static final JobSheets NONE = new JobSheets(0);

    public static final JobSheets STANDARD = new JobSheets(1);

    private static final String[] myStringTable = {
        "none",
        "standard",
    };

    private static final JobSheets[] myEnumValueTable = {
        NONE,
        STANDARD,
    };

    protected JobSheets(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobSheets.class;
    }

    public final String getName() {
        return "job-sheets";
    }
}
