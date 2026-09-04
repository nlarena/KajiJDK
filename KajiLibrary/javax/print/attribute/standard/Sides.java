package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Si se imprime de un lado o de los dos, y por que borde se da vuelta la hoja.
 *
 * <p>El borde importa porque decide para que lado hay que girar la hoja al leerla: por el largo la
 * segunda cara queda derecha como en un libro, por el corto queda cabeza abajo como en un anotador.
 *
 * <p>{@code DUPLEX} y {@code TUMBLE} son <b>alias</b>, no valores nuevos: son el mismo objeto que
 * {@code TWO_SIDED_LONG_EDGE} y {@code TWO_SIDED_SHORT_EDGE}, asi que {@code DUPLEX ==
 * TWO_SIDED_LONG_EDGE} da true y la tabla de nombres tiene tres filas, no cinco.
 */
public final class Sides extends EnumSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -6890309414893262822L;

    public static final Sides ONE_SIDED = new Sides(0);

    public static final Sides TWO_SIDED_LONG_EDGE = new Sides(1);

    public static final Sides TWO_SIDED_SHORT_EDGE = new Sides(2);

    /** Alias de {@link #TWO_SIDED_LONG_EDGE}: el mismo objeto, no un valor nuevo. */
    public static final Sides DUPLEX = TWO_SIDED_LONG_EDGE;

    /** Alias de {@link #TWO_SIDED_SHORT_EDGE}: el mismo objeto, no un valor nuevo. */
    public static final Sides TUMBLE = TWO_SIDED_SHORT_EDGE;

    private static final String[] myStringTable = {
        "one-sided",
        "two-sided-long-edge",
        "two-sided-short-edge",
    };

    private static final Sides[] myEnumValueTable = {
        ONE_SIDED,
        TWO_SIDED_LONG_EDGE,
        TWO_SIDED_SHORT_EDGE,
    };

    protected Sides(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return Sides.class;
    }

    public final String getName() {
        return "sides";
    }
}
