package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * En que orden se recorren las celdas cuando {@link NumberUp} pone varias paginas por hoja.
 *
 * <p>El nombre dice los dos ejes en orden: {@code TOBOTTOM_TORIGHT} llena una columna hacia abajo y
 * despues salta a la de la derecha. Las direcciones que van a la izquierda existen para las
 * escrituras que se leen de derecha a izquierda.
 *
 * <p>Es la unica clase de la familia con el constructor {@code private}: no admite valores de
 * sitio, porque las ocho combinaciones de los dos ejes ya estan todas.
 */
public final class PresentationDirection extends EnumSyntax implements PrintJobAttribute, PrintRequestAttribute {

    private static final long serialVersionUID = 8294728067230931780L;

    public static final PresentationDirection TOBOTTOM_TORIGHT = new PresentationDirection(0);

    public static final PresentationDirection TOBOTTOM_TOLEFT = new PresentationDirection(1);

    public static final PresentationDirection TOTOP_TORIGHT = new PresentationDirection(2);

    public static final PresentationDirection TOTOP_TOLEFT = new PresentationDirection(3);

    public static final PresentationDirection TORIGHT_TOBOTTOM = new PresentationDirection(4);

    public static final PresentationDirection TORIGHT_TOTOP = new PresentationDirection(5);

    public static final PresentationDirection TOLEFT_TOBOTTOM = new PresentationDirection(6);

    public static final PresentationDirection TOLEFT_TOTOP = new PresentationDirection(7);

    private static final String[] myStringTable = {
        "tobottom-toright",
        "tobottom-toleft",
        "totop-toright",
        "totop-toleft",
        "toright-tobottom",
        "toright-totop",
        "toleft-tobottom",
        "toleft-totop",
    };

    private static final PresentationDirection[] myEnumValueTable = {
        TOBOTTOM_TORIGHT,
        TOBOTTOM_TOLEFT,
        TOTOP_TORIGHT,
        TOTOP_TOLEFT,
        TORIGHT_TOBOTTOM,
        TORIGHT_TOTOP,
        TOLEFT_TOBOTTOM,
        TOLEFT_TOTOP,
    };

    private PresentationDirection(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return PresentationDirection.class;
    }

    public final String getName() {
        return "presentation-direction";
    }
}
