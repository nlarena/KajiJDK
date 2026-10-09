package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;

/**
 * How much a {@link PrinterStateReason} bothers: {@code REPORT} is informative, {@code WARNING}
 * lets printing go on, {@code ERROR} stops the printer.
 *
 * <p>It never appears alone in an attribute set: it is the value of {@link PrinterStateReasons}'
 * entries.
 */
public final class Severity extends EnumSyntax implements Attribute {

    private static final long serialVersionUID = 8781881462717925380L;

    public static final Severity REPORT = new Severity(0);

    public static final Severity WARNING = new Severity(1);

    public static final Severity ERROR = new Severity(2);

    private static final String[] myStringTable = {
        "report",
        "warning",
        "error",
    };

    private static final Severity[] myEnumValueTable = {
        REPORT,
        WARNING,
        ERROR,
    };

    protected Severity(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return Severity.class;
    }

    public final String getName() {
        return "severity";
    }
}
