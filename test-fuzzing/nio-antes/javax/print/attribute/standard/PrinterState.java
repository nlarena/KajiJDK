package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * The printer's coarse state.
 *
 * <p>There are three useful states and {@code UNKNOWN}; the detail of why it is stopped is carried
 * by {@link PrinterStateReasons}. Values 1 and 2 are reserved in IPP, hence the table's {@code
 * null}s.
 */
public final class PrinterState extends EnumSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = -649578618346507718L;

    public static final PrinterState UNKNOWN = new PrinterState(0);

    public static final PrinterState IDLE = new PrinterState(3);

    public static final PrinterState PROCESSING = new PrinterState(4);

    public static final PrinterState STOPPED = new PrinterState(5);

    private static final String[] myStringTable = {
        "unknown",
        null,
        null,
        "idle",
        "processing",
        "stopped",
    };

    private static final PrinterState[] myEnumValueTable = {
        UNKNOWN,
        null,
        null,
        IDLE,
        PROCESSING,
        STOPPED,
    };

    protected PrinterState(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterState.class;
    }

    public final String getName() {
        return "printer-state";
    }
}
