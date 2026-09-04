package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Como se apoya el contenido sobre la hoja.
 *
 * <p>Las variantes {@code REVERSE_} son la misma orientacion girada media vuelta; importan cuando
 * el trabajo es a dos caras o cuando la grapa tiene que quedar de un lado determinado. La
 * numeracion arranca en 3 porque es la de IPP.
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

    /** IPP numera esta categoria desde 3; la fila 0 de las tablas es ese 3. */
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
