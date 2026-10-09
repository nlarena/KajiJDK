package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * Whether the printer takes new jobs right now.
 *
 * <p>It is independent of {@link PrinterState}: a {@code STOPPED} printer may keep accepting and
 * queueing.
 */
public final class PrinterIsAcceptingJobs extends EnumSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = -5052010680537678061L;

    public static final PrinterIsAcceptingJobs NOT_ACCEPTING_JOBS = new PrinterIsAcceptingJobs(0);

    public static final PrinterIsAcceptingJobs ACCEPTING_JOBS = new PrinterIsAcceptingJobs(1);

    private static final String[] myStringTable = {
        "not-accepting-jobs",
        "accepting-jobs",
    };

    private static final PrinterIsAcceptingJobs[] myEnumValueTable = {
        NOT_ACCEPTING_JOBS,
        ACCEPTING_JOBS,
    };

    protected PrinterIsAcceptingJobs(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterIsAcceptingJobs.class;
    }

    public final String getName() {
        return "printer-is-accepting-jobs";
    }
}
