package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Si el trabajo lleva hojas separadoras --la caratula con el nombre del duenio-- entre un trabajo y
 * el siguiente.
 *
 * <p>{@code STANDARD} deja que el sitio decida como es esa hoja; la clase no es final justamente
 * para que un sitio agregue las suyas.
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
