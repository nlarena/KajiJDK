package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;

/**
 * Cuanto molesta una {@link PrinterStateReason}: {@code REPORT} es informativo, {@code WARNING}
 * deja seguir imprimiendo, {@code ERROR} para la impresora.
 *
 * <p>Nunca aparece sola en un conjunto de atributos: es el valor de las entradas de {@link
 * PrinterStateReasons}.
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
