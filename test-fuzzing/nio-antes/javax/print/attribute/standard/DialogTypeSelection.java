package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Whether the print dialog the user sees is the operating system's ({@code NATIVE}) or Java's
 * cross-platform one ({@code COMMON}).
 *
 * <p>The native one usually exposes driver options the common one does not know; the common one
 * looks the same everywhere.
 */
public final class DialogTypeSelection extends EnumSyntax implements PrintRequestAttribute {

    private static final long serialVersionUID = 7518682952133256029L;

    public static final DialogTypeSelection NATIVE = new DialogTypeSelection(0);

    public static final DialogTypeSelection COMMON = new DialogTypeSelection(1);

    private static final String[] myStringTable = {
        "native",
        "common",
    };

    private static final DialogTypeSelection[] myEnumValueTable = {
        NATIVE,
        COMMON,
    };

    protected DialogTypeSelection(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return DialogTypeSelection.class;
    }

    public final String getName() {
        return "dialog-type-selection";
    }
}
