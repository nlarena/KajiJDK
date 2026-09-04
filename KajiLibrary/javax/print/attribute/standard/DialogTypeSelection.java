package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Si el dialogo de impresion que ve el usuario es el del sistema operativo ({@code NATIVE}) o el
 * multiplataforma de Java ({@code COMMON}).
 *
 * <p>El nativo suele exponer opciones del driver que el comun no conoce; el comun se ve igual en
 * todos lados.
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
